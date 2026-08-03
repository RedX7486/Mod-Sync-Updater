package com.redx.modsync;
import java.io.File;
import com.google.gson.Gson;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class ModScanner {

    // This method takes a file path and returns its SHA-256 fingerprint
    public static String getFileChecksum(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        FileInputStream fis = new FileInputStream(file);
        
        byte[] byteArray = new byte[8192];
        int bytesCount; 
        
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };
        
        fis.close();
        byte[] bytes = digest.digest();
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        
        return sb.toString();
    }

    // It looks at a folder, finds only the .jar files, and hashes them
    public static Map<String, String> scanModsFolder(String folderPath) {
        // This is our dictionary that will hold key = "FileName", value = "Hash"
        Map<String, String> localMods = new HashMap<>();
        File folder = new File(folderPath);

        // If the folder is missing or typed wrong, stop immediately
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Error: The folder " + folderPath + " does not exist!");
            return localMods;
        }

        // Get an array of everything inside the folder
        File[] listOfFiles = folder.listFiles();
        
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                // We ONLY want to hash .jar files. Ignores everything else
                if (file.isFile() && file.getName().endsWith(".jar")) {
                    try {
                        String hash = getFileChecksum(file);
                        
                        // Save the result into our HashMap
                        localMods.put(file.getName(), hash);
                        System.out.println("Scanned: " + file.getName() + " -> " + hash);
                        
                    } catch (Exception e) {
                        System.out.println("Failed to hash " + file.getName() + ": " + e.getMessage());
                    }
                }
            }
        }

        return localMods;
    }

    // Checks what type of OS the user has 
    public static String getMinecraftModsFolder() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        String mcFolder;

        if (os.contains("win")) {
            // On Windows, Minecraft hides in the AppData folder
            String appData = System.getenv("APPDATA");
            mcFolder = (appData != null ? appData : userHome) + "/.minecraft";
        } else if (os.contains("mac")) {
            // On Mac, it hides in Application Support
            mcFolder = userHome + "/Library/Application Support/minecraft";
        } else {
            // On Linux, it sits right in the home directory
            mcFolder = userHome + "/.minecraft";
        }
        
        return mcFolder + "/mods";
    }

    public static void main(String[] args) {
        
        // Get the real Minecraft mods folder based on their OS
        String clientModsFolder = getMinecraftModsFolder();

        // they installed Forge but haven't run it yet to generate the mods folder
        java.io.File directory = new java.io.File(clientModsFolder);
        if (!directory.exists()) {
            directory.mkdirs();
            System.out.println("Created missing mods folder at: " + clientModsFolder);
        }
        
        System.out.println("Starting folder scan...");
        Map<String, String> localState = scanModsFolder(clientModsFolder);
        
        System.out.println("\nScan complete! Found " + localState.size() + " mods.");

        // Create the Gson tool
        Gson gsonMessage = new Gson();

        // Convert our dictionary into a JSON string
        String jsonOutput = gsonMessage.toJson(localState);

        System.out.println("\nHere is the JSON ready for the server:");
        System.out.println(jsonOutput);

        // Send the data to the server
        String serverUrl = "http://localhost:8080/api/sync"; 

        // Create the "Mailman" that will deliver our request
        HttpClient client = HttpClient.newHttpClient();

        // Build the actual request (The Address, the Label, and the Letter)
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(serverUrl))
                                         .header("Content-Type", "application/json")
                                         .POST(HttpRequest.BodyPublishers.ofString(jsonOutput))
                                         .build();

        // Try to send the request and wait for the server to reply
        try {
            System.out.println("\nSending data to the server...");
            
            // This sends the data and saves whatever the server says back
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body(); 
            
            System.out.println("The server replied with code: " + response.statusCode());
            System.out.println("Server message: " + response.body());

            // The next part of the code is breaking the code 
            // Use Gson to instantly convert the JSON text into our Java object
            com.google.gson.Gson gsonAnswer = new com.google.gson.Gson();
            SyncResponse syncData = gsonAnswer.fromJson(responseBody, SyncResponse.class);

            // Process Deletions
            System.out.println("\n--- PROCESSING DELETIONS ---");
            String clientFolder = getMinecraftModsFolder(); // Using your new dynamic path

            for (String modName : syncData.getToDelete()) {
                java.io.File fileToDelete = new java.io.File(clientFolder + "/" + modName);
                
                if (fileToDelete.exists()) {
                    if (fileToDelete.delete()) {
                        System.out.println("- Deleted unwanted mod: " + modName);
                    } else {
                        System.out.println("- Failed to delete, is Minecraft running?)");
                        break;
                    }
                }
            }

            System.out.println("----------------------------\n");


            System.out.println("\n--- PROCESSING DOWNLOADS ---");
            for (String modName : syncData.getToDownload()) {
                System.out.println("Downloading: " + modName + "...");
                
                // Target the new server endpoint
                String downloadUrl = "http://localhost:8080/api/download/" + modName;
                
                HttpRequest downloadReq = HttpRequest.newBuilder()
                                                     .uri(URI.create(downloadUrl))
                                                     .GET()
                                                     .build();
                    
                // Tells Java exactly where to save the file
                java.nio.file.Path targetFile = java.nio.file.Paths.get(clientFolder + "/" + modName);
                
                // Send the request and save the body directly to the file
                HttpResponse<Path> dlResponse = client.send(downloadReq, HttpResponse.BodyHandlers.ofFile(targetFile));
                
                if (dlResponse.statusCode() == 200) {
                    System.out.println("- Successfully downloaded: " + modName);
                } else {
                    System.out.println("- Failed to download: " + modName + " (Server returned code: " + dlResponse.statusCode() + ")");
                }
            }

            System.out.println("----------------------------\n");
            System.out.println("Sync Complete! Modpack is ready.");

            
        } catch (Exception e) {

            // If the server is offline or doesn't exist yet, we catch the error here so the app doesn't crash
            System.out.println("Could not connect to the server. It might be offline.\n" + e);
            System.out.println("ERROR: " + e);
        }
    }
}