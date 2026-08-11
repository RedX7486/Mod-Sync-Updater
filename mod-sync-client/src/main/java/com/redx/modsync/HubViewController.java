package com.redx.modsync;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.application.Platform;

public class HubViewController {

    // Notice this now holds our Modpack object instead of just a String
    @FXML
    private ListView<Modpack> modpackList;

    @FXML
    public void initialize() {
        setupCustomCells();
        
        // Adding dummy data to test the new layout visually
        modpackList.getItems().add(new Modpack("My Awesome Server", 42, "150 MB", "XYZ123", true));
        modpackList.getItems().add(new Modpack("Friend's Survival SMP", 12, "45 MB", "ABC987", false));
    }

    private void setupCustomCells() {
        modpackList.setCellFactory(param -> new ModpackCell());
    }

    @FXML
    private void handleJoinModpack() {
        System.out.println("Opening Join Modpack Dialog...");
    }

    @FXML
    private void handleCreateModpack() {
        System.out.println("Opening Create Modpack Dialog...");
    }

    /**
     * CUSTOM CELL FACTORY CLASS
     * Builds an explorer-style row for every modpack in the list.
     */
    private class ModpackCell extends ListCell<Modpack> {
        private final HBox layout = new HBox(15);
        private final Label nameLabel = new Label();
        private final Label roleLabel = new Label();
        private final Label modsLabel = new Label();
        private final Label sizeLabel = new Label();
        private final Region spacer = new Region();
        private final Button copyCodeButton = new Button();

        public ModpackCell() {
            super();
            
            // Align everything vertically in the center of the row
            layout.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            // Assign fixed widths to match the FXML headers exactly!
            nameLabel.setPrefWidth(250);
            nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
            
            roleLabel.setPrefWidth(100);
            roleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cccccc;");
            
            modsLabel.setPrefWidth(100);
            modsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cccccc;");
            
            sizeLabel.setPrefWidth(100);
            sizeLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cccccc;");
            
            copyCodeButton.setPrefWidth(100);
            copyCodeButton.setStyle("-fx-background-color: #555555; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
            
            layout.getChildren().addAll(nameLabel, roleLabel, modsLabel, sizeLabel, spacer, copyCodeButton);
            layout.setStyle("-fx-padding: 8px 10px 8px 10px;");
        }

        @Override
        protected void updateItem(Modpack item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                nameLabel.setText(item.getName());
                roleLabel.setText(item.isAdmin() ? "Admin" : "Player");
                modsLabel.setText(String.valueOf(item.getModCount()));
                sizeLabel.setText(item.getTotalSize());
                
                // Set up the secure copy button
                copyCodeButton.setText("Copy Code");
                copyCodeButton.setOnAction(event -> {
                    Clipboard clipboard = Clipboard.getSystemClipboard();
                    ClipboardContent content = new ClipboardContent();
                    content.putString(item.getJoinCode());
                    clipboard.setContent(content);
                    
                    copyCodeButton.setText("Copied!");
                    copyCodeButton.setStyle("-fx-background-color: #2e8b57; -fx-text-fill: white; -fx-font-weight: bold;");
                    
                    new Thread(() -> {
                        try { Thread.sleep(2000); } catch (InterruptedException e) {}
                        Platform.runLater(() -> {
                            copyCodeButton.setText("Copy Code");
                            copyCodeButton.setStyle("-fx-background-color: #555555; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                        });
                    }).start();
                });
                
                setGraphic(layout);
            }
        }
    }
}