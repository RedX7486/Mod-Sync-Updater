package com.redx.modsync;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import jakarta.annotation.PostConstruct;

// @RestController tells Spring Boot: "This class handles internet traffic."
@RestController
// @RequestMapping("/api") means every web address in this file starts with /api
@RequestMapping("/api")
public class SyncController {

    // This is our cache
    private Map<String,String> cachedServerMods;

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

    // The receptionist
    @PostMapping("/sync")
    public List<String> receiveClientMods(@RequestBody Map<String,String> clientMods) {
        
        List<String> modsToDownload = new ArrayList<>();

        // We look at every single mod cacheServerMods has
        for (Map.Entry<String,String> serverMod : cachedServerMods.entrySet()) {
            String modName = serverMod.getKey();
            String serverHash = serverMod.getValue();
            
            // If the client does NOT have the mod, or the hashes do not match
            if (!clientMods.containsKey(modName) || !clientMods.get(modName).equals(serverHash)) {
                // Add it to the download list
                modsToDownload.add(modName);
            }
        }

        if (modsToDownload.size() == 1){
            System.out.println("Client needs " + modsToDownload.size() + " update.");
        } else {
            System.out.println("Client needs " + modsToDownload.size() + " updates.");
        }
        
        return modsToDownload;
    }
}