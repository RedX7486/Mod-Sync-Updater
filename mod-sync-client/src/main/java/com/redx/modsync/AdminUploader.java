package com.redx.modsync;

import okhttp3.*;
import java.io.File;
import java.io.IOException;

public class AdminUploader {

    // TODO: Change this to the servers actual IP address when deployed
    private static final String SERVER_URL = "http://localhost:8080/api/admin"; 
    
    // OkHttp is designed to share one single instance for the whole app
    private static final OkHttpClient client = new OkHttpClient();

    // Uploads the mod pack when the user is an admins to the server
    public static boolean uploadMod(String filePath, String adminToken) {
        File modFile = new File(filePath);
        
        if (!modFile.exists()) {
            System.out.println("Error: Mod file does not exist at " + filePath);
            return false;
        }

        // Package the file as a "multipart" form
        RequestBody fileBody = RequestBody.create(modFile, MediaType.parse("application/java-archive"));
        
        MultipartBody requestBody = new MultipartBody.Builder()
                                                     .setType(MultipartBody.FORM)
                                                     .addFormDataPart("file", modFile.getName(), fileBody)
                                                     .build();

        // Build the request with the secret token
        Request request = new Request.Builder()
                                     .url(SERVER_URL + "/upload")
                                     .addHeader("X-Admin-Token", adminToken)
                                     .post(requestBody)
                                     .build();

        // Send it to the server
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                System.out.println("Success! Uploaded: " + modFile.getName());
                return true;

            } else {
                System.out.println("Upload failed. Server said: " + response.body().string());
                return false;

            }

        } catch (IOException e) {
            System.out.println("Network error: " + e.getMessage());
            return false;
        }
    }

    // This deletes the mods on request of the admins
    public static boolean deleteMod(String filename, String adminToken) {
        
        // Empty body because we just need to hit the endpoint
        RequestBody emptyBody = RequestBody.create(new byte[0], null);

        Request request = new Request.Builder()
                                     .url(SERVER_URL + "/delete/" + filename)
                                     .addHeader("X-Admin-Token", adminToken)
                                     .post(emptyBody)
                                     .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                System.out.println("Success! Deleted: " + filename + " from server.");
                return true;

            } else {
                System.out.println("Delete failed. Server said: " + response.code());
                return false;

            }
            
        } catch (IOException e) {
            System.out.println("Network error: " + e.getMessage());
            return false;
        }
    }
}