package com.kesierzg.securevault;

import com.kesierzg.securevault.model.PasswordEntry;
import com.kesierzg.securevault.service.VaultService;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

public class Main extends Application {

    private final File vaultFile = new File(System.getProperty("user.dir"), "vault.json");
    private VaultService vault;
    private TableView<PasswordEntry> tableView = new TableView<>();
    private boolean hideSensitive = true;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SecureVault");
        PasswordField loginField = new PasswordField();
        Button loginButton = new Button("Zaloguj");
        VBox loginBox = new VBox(10, new Label("Podaj hasło:"), loginField, loginButton);
        loginBox.setPadding(new Insets(20));
        loginBox.setVisible(true);

        TableColumn<PasswordEntry, String> websiteCol = new TableColumn<>("Website");
        websiteCol.setCellValueFactory(new PropertyValueFactory<>("website"));

        TableColumn<PasswordEntry, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<PasswordEntry, String> passwordCol = new TableColumn<>("Password");
        passwordCol.setCellValueFactory(new PropertyValueFactory<>("password"));

        tableView.getColumns().addAll(websiteCol, usernameCol, passwordCol);

        Button addBtn = new Button("dodaj");
        Button removeBtn = new Button("usuń");
        Button editBtn = new Button("edytuj");

        CheckBox hideCheck = new CheckBox("ukryj dane");
        hideCheck.setSelected(true);
        hideCheck.selectedProperty().addListener((obs, oldV, newV) -> {
            hideSensitive = newV;
            refreshTable();
        });

        HBox buttonsBox = new HBox(10, addBtn, removeBtn, editBtn, hideCheck);
        buttonsBox.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setCenter(loginBox);
        root.setBottom(buttonsBox);
        BorderPane.setMargin(tableView, new Insets(10));

        addBtn.setOnAction(e -> {
            PasswordEntry entry = showEntryDialog(null);
            if (entry != null) {
                vault.addEntry(entry.getWebsite(), entry.getUsername(), entry.getPassword());
                vault.saveToFile(vaultFile);
                System.out.println("zapisano baze w " + vaultFile.getAbsolutePath());
                refreshTable();
            }
        });

        removeBtn.setOnAction(e -> {
            PasswordEntry selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                vault.removeEntryByWebsite(selected.getWebsite());
                vault.saveToFile(vaultFile);
                System.out.println("zapisano baze w " + vaultFile.getAbsolutePath());
                refreshTable();
            }
        });

        editBtn.setOnAction(e -> {
            PasswordEntry selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                PasswordEntry edited = showEntryDialog(selected);
                if (edited != null) {
                    vault.editEntry(selected.getWebsite(), edited.getWebsite(), edited.getUsername(), edited.getPassword());
                    vault.saveToFile(vaultFile);
                    System.out.println("zapisano baze w " + vaultFile.getAbsolutePath());
                    refreshTable();
                }
            }
        });

        Scene scene = new Scene(root, 700, 400);
        primaryStage.setScene(scene);
        loginButton.setOnAction(e -> {
            String password = loginField.getText();
            if (vaultFile.exists()) {
                try {
                    vault = VaultService.loadFromFile(vaultFile, password);
                } catch (Exception ex) {
                    loginField.clear();
                    return;
                }
            } else {
                vault = new VaultService(password);
            }
            root.setCenter(tableView);
            root.setBottom(buttonsBox);
            refreshTable();
        });
        primaryStage.setOnCloseRequest(event -> vault.saveToFile(vaultFile));
        primaryStage.show();
    }

    private void refreshTable() {
        tableView.getItems().clear();
        for (PasswordEntry e : vault.getEntries()) {
            if (hideSensitive) {
                tableView.getItems().add(new PasswordEntry(e.getWebsite(), "******", "******"));
            } else {
                tableView.getItems().add(e);
            }
        }
    }

    private PasswordEntry showEntryDialog(PasswordEntry existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existing == null ? "dodaj" : "edytuj");

        TextField websiteField = new TextField();
        TextField usernameField = new TextField();
        TextField passwordField = new TextField();

        if (existing != null) {
            websiteField.setText(existing.getWebsite());
            usernameField.setText(existing.getUsername());
            passwordField.setText(existing.getPassword());
        }

        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(10);
        grid.setPadding(new Insets(10));

        grid.addRow(0, new Label("Strona:"), websiteField);
        grid.addRow(1, new Label("Nazwa użytkownika:"), usernameField);
        grid.addRow(2, new Label("Hasuo:"), passwordField);

        Button okBtn = new Button("OK");
        Button cancelBtn = new Button("ANULUJ");
        HBox buttons = new HBox(10, okBtn, cancelBtn);

        VBox vbox = new VBox(10, grid, buttons);
        vbox.setPadding(new Insets(10));

        okBtn.setOnAction(e -> {
            if (!websiteField.getText().isEmpty()
                    && !usernameField.getText().isEmpty()
                    && !passwordField.getText().isEmpty()) {
                dialog.close();
            }
        });

        cancelBtn.setOnAction(e -> {
            websiteField.setText("");
            dialog.close();
        });

        Scene scene = new Scene(vbox);
        dialog.setScene(scene);
        dialog.showAndWait();

        if (websiteField.getText().isEmpty()
                || usernameField.getText().isEmpty()
                || passwordField.getText().isEmpty()) {
            return null;
        }

        return new PasswordEntry(websiteField.getText(), usernameField.getText(), passwordField.getText());
    }

    public static void main(String[] args) {
        launch(args);
    }
}