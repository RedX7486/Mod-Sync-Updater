package com.redx.modsync;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import jakarta.annotation.PostConstruct;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;


// @RestController tells Spring Boot: "This class handles internet traffic"
@RestController
// @RequestMapping("/api") means every web address in this file starts with /api
@RequestMapping("/api")
public class SyncController {

    // This is our cache
    private Map<String,String> cachedServerMods;
    private final AdminController adminController;

    // Spring Boot automatically provides the adminController when it builds this class
    public SyncController(AdminController adminController) {
        this.adminController = adminController;
    }

    // Lets the admins of the mod pack upload mods
    @PostMapping("/admin/upload")
    public ResponseEntity<String> uploadMasterMod(@RequestHeader("X-Admin-Token") String token,
                                                  @RequestParam("file") MultipartFile file) {
        
        // Security Check via AdminController
        if (!adminController.isAuthorized(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access Denied: Invalid Admin Token!");
        }

        try {
            // Save the uploaded file
            String serverFolder = System.getProperty("user.home") + "/.modsync-server/master_mods/";
            Path targetPath = Paths.get(serverFolder).resolve(file.getOriginalFilename()).normalize();
            
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Admin uploaded: " + file.getOriginalFilename());
            
            initializeCache();
            
            return ResponseEntity.ok("File securely uploaded to server!");
            
        } catch (Exception e) {
            System.out.println("Upload failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Server failed to save the file.");
        }
    }

    // Lets the admins delete files of the mod pack
    @PostMapping("/admin/delete/{filename}")
    public ResponseEntity<String> deleteMasterMod(@RequestHeader("X-Admin-Token") String token,
                                                  @PathVariable String filename) {
        
        if (!adminController.isAuthorized(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access Denied: Invalid Admin Token!");
        }

        try {
            String serverFolder = System.getProperty("user.home") + "/.modsync-server/master_mods/";
            java.io.File fileToDelete = new java.io.File(serverFolder + filename);
            
            if (fileToDelete.exists() && fileToDelete.delete()) {
                System.out.println("Admin deleted mod: " + filename);
                
                initializeCache();
                
                return ResponseEntity.ok("Deleted successfully.");
            }
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }





    // This runs when the server boots up
    @PostConstruct
    public void initializeCache() {
        System.out.println("SERVER BOOTING: Scanning master folder...");
        
        // Dynamically gets the users path
        String userHome = System.getProperty("user.home");
        String masterFolder = userHome + "/.modsync-server/master_mods";

        // Creates the directory if it does not exist
        java.io.File directory = new java.io.File(masterFolder);
        if (!directory.exists()) {
            directory.mkdirs();
            System.out.println("Created new master folder at: " + masterFolder);
        }
        
        // We save the results into our permanent RAM variable.
        this.cachedServerMods = ServerScanner.scanModsFolder(masterFolder);
        System.out.println("CACHE READY: Saved " + cachedServerMods.size() + " mods into memory.");
    }

    // Lets the client download the mods
    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadMod(@PathVariable String filename) {
        
        try {
            // Get the servers master folder
            String serverFolder = System.getProperty("user.home") + "/.modsync-server/master_mods/";
            Path filePath = Paths.get(serverFolder).resolve(filename).normalize();
            
            // Turn the file into a Spring Resource
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                return ResponseEntity.ok()
                                     .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                                     .body(resource);

            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // The receptionist
    @PostMapping("/sync")
    public SyncResponse receiveClientMods(@RequestBody Map<String,String> clientMods) {
        
        List<String> downloadList = new ArrayList<>();
        List<String> deleteList = new ArrayList<>();

        // We look at every single mod cacheServerMods has
        for (Map.Entry<String,String> serverMod : cachedServerMods.entrySet()) {
            String modName = serverMod.getKey();
            String serverHash = serverMod.getValue();
            
            // If the client does NOT have the mod, or the hashes do not match
            if (!clientMods.containsKey(modName) || !clientMods.get(modName).equals(serverHash)) {
                // Add it to the download list
                downloadList.add(modName);
            }
        }

        // Looks at which mods to delete
        for (String clientModName : clientMods.keySet()) {
            if (!cachedServerMods.containsKey(clientModName)) {
                deleteList.add(clientModName);
            }
        }

        if (downloadList.size() == 1){
            System.out.println("Client needs " + (downloadList.size() + deleteList.size())  + " update.");
        } else {
            System.out.println("Client needs " + (downloadList.size() + deleteList.size()) + " updates.");
        }
        
        return new SyncResponse(downloadList, deleteList);
    }


}