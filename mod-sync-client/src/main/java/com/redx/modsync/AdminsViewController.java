package com.redx.modsync;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminsViewController {

    @FXML
    private ListView<String> pendingUploadsList;

    @FXML
    private ListView<String> serverModsList;

    @FXML
    private Button pushChangesButton;

    @FXML
    private javafx.scene.control.PasswordField adminTokenField;

    // We use these to remember the data behind the UI
    private final Map<String, String> pendingPaths = new HashMap<>();
    private final List<String> modsToDelete = new ArrayList<>();

    @FXML
    public void initialize() {
        setupDragAndDrop();
        setupCustomCells();
        loadServerMods();
    }

    private void setupCustomCells() {
        // Tells the lists to use our custom layout instead of plain text
        pendingUploadsList.setCellFactory(param -> new ModCell(true));
        serverModsList.setCellFactory(param -> new ModCell(false));
    }

    // This is so that the user can drag and drop the mods
    private void setupDragAndDrop() {

        pendingUploadsList.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
           
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();

        });

        pendingUploadsList.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            
            if (db.hasFiles()) {
                success = true;
                for (File file : db.getFiles()) {
                    if (file.getName().endsWith(".jar") && !pendingUploadsList.getItems().contains(file.getName())) {
                        
                        // We store just the file name now so the UI looks cleaner
                        pendingUploadsList.getItems().add(file.getName());
    
                        // Remembers the actual path for OkHttp
                        pendingPaths.put(file.getName(), file.getAbsolutePath());

                    }
                }
            }
            
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void loadServerMods() {
        serverModsList.getItems().clear();
        
        // Fetch the live list from the Spring Boot server
        List<String> liveMods = AdminUploader.getServerMods();
        
        // Populate the JavaFX list
        for (String mod : liveMods) {
            serverModsList.getItems().add(mod);
        }
    }


    // This sends the mods to the server and updates the servers mods
    @FXML
    private void handlePushChanges() {
        String token = adminTokenField.getText();

        if (token == null || token.isEmpty()) {
            System.out.println("Error: Please enter the admin password!");
            return;
        }

        System.out.println("Executing Push Changes to Server...");

        // Execute Deletions
        for (String filename : modsToDelete) {
            System.out.println("Deleting: " + filename);
            AdminUploader.deleteMod(filename, token);
        }

        // Execute Uploads
        for (String filename : pendingUploadsList.getItems()) {
            String fullPath = pendingPaths.get(filename);
            System.out.println("Uploading: " + filename);
            AdminUploader.uploadMod(fullPath, token);
        }

        // Clean up the UI after a successful sync
        pendingUploadsList.getItems().clear();
        pendingPaths.clear();
        modsToDelete.clear();
        
        // Reload the right column to prove the server updated
        loadServerMods(); 
    }

    /**
     * CUSTOM CELL FACTORY CLASS
     * This builds the specific UI for every single row in the lists.
     */
    private class ModCell extends ListCell<String> {
        private final HBox layout = new HBox(10);
        private final Label modName = new Label();
        private final Button actionButton = new Button();
        private final Region spacer = new Region();

        public ModCell(boolean isUploadList) {
            super();
            
            // This spacer pushes the button all the way to the right edge
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            if (isUploadList) {
                actionButton.setText("X");
                actionButton.setStyle("-fx-background-color: transparent; -fx-text-fill:  #cc0000; -fx-font-weight: bold;");
                
                actionButton.setOnAction(event -> {
                    // Completely remove the item from the staging area
                    getListView().getItems().remove(getItem());
                });

            } else {
                actionButton.setText("X");
                actionButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #cc0000; -fx-font-weight: bold;");
                
                actionButton.setOnAction(event -> {
                    String item = getItem();

                    // Toggle logic: add/remove from the deletion list if they click it multiple times
                    if (!modsToDelete.contains(item)) {
                        modsToDelete.add(item);

                        // Mark the server mod visually for pending deletion
                    modName.setStyle("-fx-strikethrough: true; -fx-text-fill: red;");

                    } else {
                        modsToDelete.remove(item);
                        modName.setStyle("");

                    }

                });
            }
            
            layout.getChildren().addAll(modName, spacer, actionButton);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
                setGraphic(null);

            } else {
                modName.setText(item);
                
                // Ensure the red strikethrough stays if scrolling off-screen
                if (modsToDelete.contains(item)) {
                    modName.setStyle("-fx-strikethrough: true; -fx-text-fill: red;");
                } else {
                    modName.setStyle("");
                }
                
                setGraphic(layout);
            }
        }
    
    }
}