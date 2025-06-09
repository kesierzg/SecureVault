package com.kesierzg.securevault;

import com.kesierzg.securevault.model.NoteEntry;
import com.kesierzg.securevault.model.PasswordEntry;
import com.kesierzg.securevault.service.ExportService;
import com.kesierzg.securevault.service.ImportService;
import com.kesierzg.securevault.service.VaultService;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import java.io.File;

public class Main extends javafx.application.Application {

    private final File vaultFile = new File(System.getProperty("user.dir"), "vault.json");
    private VaultService vault;

    private TableView<PasswordEntry> passwordTable = new TableView<>();
    private TableView<NoteEntry> notesTable = new TableView<>();
    private boolean hideSensitive = true;

    @Override
    public void start(Stage primaryStage) {
        VBox loginBox = new VBox(10);
        loginBox.setPadding(new Insets(20));

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Wprowadź hasło główne");

        Button loginButton = new Button("Zaloguj");
        Label loginError = new Label();

        loginBox.getChildren().addAll(new Label("Hasło główne:"), passwordField, loginButton, loginError);

        Scene loginScene = new Scene(loginBox, 300, 150);
        primaryStage.setScene(loginScene);
        primaryStage.setTitle("SecureVault");
        primaryStage.show();

        loginButton.setOnAction(e -> {
            String masterPassword = passwordField.getText();
            if (masterPassword.isEmpty()) return;

            try {
                if (vaultFile.exists()) {
                    vault = VaultService.loadFromFile(vaultFile, masterPassword);
                } else {
                    vault = new VaultService(masterPassword);
                }
            } catch (RuntimeException ex) {
                loginError.setText("Niepoprawne hasło lub uszkodzona baza danych");
                return;
            }

            showMainUI(primaryStage);
        });
    }

    private void showMainUI(Stage primaryStage) {
        TabPane tabPane = new TabPane();

        Tab passwordsTab = new Tab("Hasła");
        passwordsTab.setContent(createPasswordsTabContent());
        passwordsTab.setClosable(false);

        Tab notesTab = new Tab("Notatki");
        notesTab.setContent(createNotesTabContent());
        notesTab.setClosable(false);

        tabPane.getTabs().addAll(passwordsTab, notesTab);

        Scene scene = new Scene(tabPane, 700, 400);
        primaryStage.setScene(scene);
        primaryStage.show();

        Tab settingsTab = new Tab("Ustawienia");
        settingsTab.setContent(createSettingsTabContent());
        settingsTab.setClosable(false);

        tabPane.getTabs().add(settingsTab);
    }

    private VBox createPasswordsTabContent() {
        TableColumn<PasswordEntry, String> websiteCol = new TableColumn<>("Strona");
        websiteCol.setCellValueFactory(new PropertyValueFactory<>("website"));

        TableColumn<PasswordEntry, String> usernameCol = new TableColumn<>("Użytkownik");
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<PasswordEntry, String> passwordCol = new TableColumn<>("Hasło");
        passwordCol.setCellValueFactory(new PropertyValueFactory<>("password"));

        passwordTable.getColumns().setAll(websiteCol, usernameCol, passwordCol);

        Button addBtn = new Button("Dodaj");
        Button removeBtn = new Button("Usuń");
        Button editBtn = new Button("Edytuj");

        CheckBox hideCheck = new CheckBox("Ukryj dane");
        hideCheck.setSelected(true);
        hideCheck.selectedProperty().addListener((obs, oldV, newV) -> {
            hideSensitive = newV;
            refreshPasswordTable();
        });

        HBox buttonsBox = new HBox(10, addBtn, removeBtn, editBtn, hideCheck);
        buttonsBox.setPadding(new Insets(10));

        addBtn.setOnAction(e -> {
            PasswordEntry entry = showPasswordEntryDialog(null);
            if (entry != null) {
                vault.addEntry(entry.getWebsite(), entry.getUsername(), entry.getPassword());
                vault.saveToFile(vaultFile);
                refreshPasswordTable();
            }
        });

        removeBtn.setOnAction(e -> {
            PasswordEntry selected = passwordTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                vault.removeEntryByWebsite(selected.getWebsite());
                vault.saveToFile(vaultFile);
                refreshPasswordTable();
            }
        });

        editBtn.setOnAction(e -> {
            PasswordEntry selected = passwordTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                PasswordEntry edited = showPasswordEntryDialog(selected);
                if (edited != null) {
                    vault.editEntry(selected.getWebsite(), edited.getWebsite(), edited.getUsername(), edited.getPassword());
                    vault.saveToFile(vaultFile);
                    refreshPasswordTable();
                }
            }
        });

        refreshPasswordTable();

        VBox vbox = new VBox(passwordTable, buttonsBox);
        VBox.setVgrow(passwordTable, Priority.ALWAYS);
        return vbox;
    }

    private void refreshPasswordTable() {
        passwordTable.getItems().clear();
        for (PasswordEntry e : vault.getEntries()) {
            if (hideSensitive) {
                passwordTable.getItems().add(new PasswordEntry(e.getWebsite(), "******", "******"));
            } else {
                passwordTable.getItems().add(e);
            }
        }
    }

    private PasswordEntry showPasswordEntryDialog(PasswordEntry existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existing == null ? "Dodaj wpis" : "Edytuj wpis");

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
        grid.addRow(1, new Label("Użytkownik:"), usernameField);
        grid.addRow(2, new Label("Hasło:"), passwordField);

        Button okBtn = new Button("OK");
        Button cancelBtn = new Button("Anuluj");

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
            websiteField.clear();
            usernameField.clear();
            passwordField.clear();
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

    private VBox createNotesTabContent() {
        TableColumn<NoteEntry, String> noteTitleCol = new TableColumn<>("Nazwa");
        noteTitleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<NoteEntry, String> noteContentCol = new TableColumn<>("Treść");
        noteContentCol.setCellValueFactory(new PropertyValueFactory<>("content"));

        notesTable.getColumns().setAll(noteTitleCol, noteContentCol);

        Button addNoteBtn = new Button("Dodaj");
        Button removeNoteBtn = new Button("Usuń");
        Button editNoteBtn = new Button("Edytuj");
        CheckBox hideCheck = new CheckBox("Ukryj dane");
        hideCheck.setSelected(true);
        hideCheck.selectedProperty().addListener((obs, oldV, newV) -> {
            refreshNotesTable(newV);
        });
        refreshNotesTable(hideCheck.isSelected());

        HBox notesButtonsBox = new HBox(10, addNoteBtn, removeNoteBtn, editNoteBtn, hideCheck);
        notesButtonsBox.setPadding(new Insets(10));

        addNoteBtn.setOnAction(e -> {
            NoteEntry note = showNoteEntryDialog(null);
            if (note != null) {
                vault.addNote(note.getTitle(), note.getContent());
                vault.saveToFile(vaultFile);
                refreshNotesTable(hideCheck.isSelected());
            }
        });

        removeNoteBtn.setOnAction(e -> {
            NoteEntry selected = notesTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                vault.removeNoteByTitle(selected.getTitle());
                vault.saveToFile(vaultFile);
                refreshNotesTable(hideCheck.isSelected());
            }
        });

        editNoteBtn.setOnAction(e -> {
            NoteEntry selected = notesTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                NoteEntry edited = showNoteEntryDialog(selected);
                if (edited != null) {
                    vault.editNote(selected.getTitle(), edited.getTitle(), edited.getContent());
                    vault.saveToFile(vaultFile);
                    refreshNotesTable(hideCheck.isSelected());
                }
            }
        });

        VBox vbox = new VBox(notesTable, notesButtonsBox);
        VBox.setVgrow(notesTable, Priority.ALWAYS);
        return vbox;
    }

    private void refreshNotesTable(boolean hide) {
        notesTable.getItems().clear();
        for (NoteEntry e : vault.getNotes()) {
            if (hide) {
                notesTable.getItems().add(new NoteEntry(e.getTitle(), "******"));
            } else {
                notesTable.getItems().add(e);
            }
        }
    }

    private NoteEntry showNoteEntryDialog(NoteEntry existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existing == null ? "Dodaj notatkę" : "Edytuj notatkę");

        TextField titleField = new TextField();
        TextArea contentArea = new TextArea();
        contentArea.setWrapText(true);
        contentArea.setPrefRowCount(5);

        if (existing != null) {
            titleField.setText(existing.getTitle());
            contentArea.setText(existing.getContent());
        }

        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(10);
        grid.setPadding(new Insets(10));

        grid.addRow(0, new Label("Nazwa:"), titleField);
        grid.addRow(1, new Label("Treść (max 360 znaków):"), contentArea);

        Button okBtn = new Button("OK");
        Button cancelBtn = new Button("Anuluj");

        HBox buttons = new HBox(10, okBtn, cancelBtn);

        VBox vbox = new VBox(10, grid, buttons);
        vbox.setPadding(new Insets(10));

        okBtn.setOnAction(e -> {
            if (!titleField.getText().isEmpty()
                    && !contentArea.getText().isEmpty()
                    && contentArea.getText().length() <= 360) {
                dialog.close();
            }
        });

        cancelBtn.setOnAction(e -> {
            titleField.clear();
            contentArea.clear();
            dialog.close();
        });

        Scene scene = new Scene(vbox);
        dialog.setScene(scene);
        dialog.showAndWait();

        if (titleField.getText().isEmpty()
                || contentArea.getText().isEmpty()
                || contentArea.getText().length() > 360) {
            return null;
        }

        return new NoteEntry(titleField.getText(), contentArea.getText());
    }

    private VBox createSettingsTabContent() {
        Button importBtn = new Button("Importuj z Bitwardena (CSV)");
        Button exportBtn = new Button("Eksportuj do Bitwardena (CSV)");

        ImportService importService = new ImportService();
        ExportService exportService = new ExportService();

        importBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Wybierz plik CSV do importu");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File file = chooser.showOpenDialog(null);
            if (file != null) {
                try {
                    java.util.List<PasswordEntry> importedEntries = importService.importFromBitwarden(file);
                    for (PasswordEntry entry : importedEntries) {
                        vault.addEntry(entry.getWebsite(), entry.getUsername(), entry.getPassword());
                    }
                    vault.saveToFile(vaultFile);
                    refreshPasswordTable();
                } catch (Exception ex) {
                    showAlert("Błąd importu", "Nie udało się zaimportować pliku CSV.");
                }
            }
        });

        exportBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Wybierz miejsce do zapisania pliku CSV");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File file = chooser.showSaveDialog(null);
            if (file != null) {
                try {
                    exportService.exportToBitwarden(vault.getEntries(), file);
                } catch (Exception ex) {
                    showAlert("Błąd eksportu", "Nie udało się wyeksportować pliku CSV.");
                }
            }
        });

        VBox vbox = new VBox(15, importBtn, exportBtn);
        vbox.setPadding(new Insets(20));
        return vbox;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}