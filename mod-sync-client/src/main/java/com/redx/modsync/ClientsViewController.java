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

public class ClientsViewController {

    @FXML
    private ListView<String> pendingUploadsList;

    @FXML
    private ListView<String> serverModsList;

    @FXML
    private Button pushChangesButton;

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
                    }
                }
            }
            
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void loadServerMods() {
        // TODO: Hook this up to the actual server sync logic next
        serverModsList.getItems().add("Example_Mod_v1.jar");
    }

    @FXML
    private void handlePushChanges() {
        System.out.println("Pushing " + pendingUploadsList.getItems().size() + " mods to server...");
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
                    // Mark the server mod visually for pending deletion
                    modName.setStyle("-fx-strikethrough: true; -fx-text-fill: red;");
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
                
                // Reset styles when scrolling so JavaFX recycles cells correctly
                if (!actionButton.getText().equals("Delete") || !modName.getStyle().contains("strikethrough")) {
                    modName.setStyle("");
                }
                
                setGraphic(layout);
            }
        }
    
    }
}