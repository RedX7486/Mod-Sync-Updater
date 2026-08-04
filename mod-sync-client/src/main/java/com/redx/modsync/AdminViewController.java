package com.redx.modsync;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import java.io.File;

public class AdminViewController {

    @FXML
    private ListView<String> pendingUploadsList;

    @FXML
    private ListView<String> serverModsList;

    @FXML
    private Button pushChangesButton;

    @FXML
    public void initialize() {
        setupDragAndDrop();
        loadServerMods();
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
                    if (file.getName().endsWith(".jar") && !pendingUploadsList.getItems().contains(file.getAbsolutePath())) {
                        pendingUploadsList.getItems().add(file.getAbsolutePath());
                    }
                }
            }
            
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void loadServerMods() {
        // We will hook this up to your actual server sync logic next
        serverModsList.getItems().add("Example_Mod_v1.jar");
    }

    @FXML
    private void handlePushChanges() {
        System.out.println("Pushing " + pendingUploadsList.getItems().size() + " mods to server...");
    }
}