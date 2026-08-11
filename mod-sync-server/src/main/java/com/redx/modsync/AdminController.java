package com.redx.modsync;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    // Path is now explicitly INSIDE the master_mods folder
    private final String ADMIN_FILE_PATH = System.getProperty("user.home") + "/.modsync-server/master_mods/admins.json";
    private final Gson gson = new Gson();

    
    // Reads in the current admins
    private List<String> loadAdmins() {
        File file = new File(ADMIN_FILE_PATH);

        if (!file.exists()) {
            // First-time setup: If the file doesn't exist, create it with a default master password
            List<String> defaultAdmins = new ArrayList<>();

            // TODO: Change the password to the users password
            defaultAdmins.add("master-password-123");
            saveAdmins(defaultAdmins);
            System.out.println("- Created default admins.json! Master password is: master-password-123");
            return defaultAdmins;
        }

        try (FileReader reader = new FileReader(file)) {
            // TypeToken is required by Gson to properly read an Array of Strings
            return gson.fromJson(reader, new TypeToken<>(){}.getType());
            
        } catch (Exception e) {
            System.out.println("Error reading admins: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Saves the admins
    private void saveAdmins(List<String> admins) {

        try (FileWriter writer = new FileWriter(ADMIN_FILE_PATH)) {
            gson.toJson(admins, writer);

        } catch (Exception e) {
            System.out.println("Error saving admins: " + e.getMessage());
        }
    }

    // Checks if they are authorized
    public boolean isAuthorized(String token) {
        return loadAdmins().contains(token);
    }

    
    // Adds new addmins to the mod pack
    @PostMapping("/add")
    public ResponseEntity<String> addAdmin(@RequestHeader("X-Admin-Token") String currentToken,
                                           @RequestParam String newToken) {
        
        if (!isAuthorized(currentToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access Denied");
        }

        List<String> admins = loadAdmins();
        if (!admins.contains(newToken)) {
            admins.add(newToken);
            saveAdmins(admins);

            return ResponseEntity.ok("Added new admin token successfully.");
        }

        return ResponseEntity.badRequest().body("Admin already exists.");
    }

    // Removes an admin from the mod pack
    @PostMapping("/remove")
    public ResponseEntity<String> removeAdmin(@RequestHeader("X-Admin-Token") String currentToken,
                                              @RequestParam String targetToken) {
        
        if (!isAuthorized(currentToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access Denied");
        }

        List<String> admins = loadAdmins();
        
        // Prevent deleting the very last admin, which would lock everyone out
        if (admins.size() <= 1) {
            return ResponseEntity.badRequest().body("Cannot delete the final admin.");
        }

        if (admins.remove(targetToken)) {
            saveAdmins(admins);
            return ResponseEntity.ok("Admin token removed.");
        }

        return ResponseEntity.notFound().build();
    }
}