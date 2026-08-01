package com.redx.modsync;
import java.io.File;
import com.google.gson.Gson;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

    // It looks at a folder, finds only the .jar files, and hashes them.
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

    public static void main(String[] args) {
        
        // Dynamically gets the users path
        String userHome = System.getProperty("user.home");
        String clientModsFolder = userHome + "/.modsync-client/mods";
        
        System.out.println("Starting folder scan...");
        Map<String, String> localState = scanModsFolder(clientModsFolder);
        
        System.out.println("\nScan complete! Found " + localState.size() + " mods.");

        // Create the Gson tool
        Gson gson = new Gson();

        // Convert our dictionary into a JSON string
        String jsonOutput = gson.toJson(localState);

        System.out.println("\nHere is the JSON ready for the server:");
        System.out.println(jsonOutput);

        // Send the data to the server
        String serverUrl = "http://localhost:8080/api/sync"; 

        // Create the "Mailman" that will deliver our request.
        HttpClient client = HttpClient.newHttpClient();

        // Build the actual request (The Address, the Label, and the Letter).
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(serverUrl))
                                         .header("Content-Type", "application/json")
                                         .POST(HttpRequest.BodyPublishers.ofString(jsonOutput))
                                         .build();

        // Try to send the request and wait for the server to reply.
        try {
            System.out.println("\nSending data to the server...");
            
            // This sends the data and saves whatever the server says back.
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("The server replied with code: " + response.statusCode());
            System.out.println("Server message: " + response.body());
            
        } catch (Exception e) {
            // If the server is offline or doesn't exist yet, we catch the error here so the app doesn't crash.
            System.out.println("Could not connect to the server. It might be offline.");
        }
    }
}