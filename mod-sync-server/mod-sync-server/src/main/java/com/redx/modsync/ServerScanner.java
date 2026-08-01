package com.redx.modsync;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class ServerScanner {

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
    public static Map<String,String> scanModsFolder(String folderPath) {
        Map<String,String> localMods = new HashMap<>();
        File folder = new File(folderPath);

        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile() && file.getName().endsWith(".jar")) {
                    try {
                        localMods.put(file.getName(), getFileChecksum(file));

                    } catch (Exception e) {
                        System.out.println("Failed to scan " + file.getName() + ": " + e.getMessage());
                    }
                }
            }
        }

        return localMods;
    }
}