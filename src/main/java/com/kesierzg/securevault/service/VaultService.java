package com.kesierzg.securevault.service;
import com.kesierzg.securevault.model.NoteEntry;
import java.util.List;
import java.util.ArrayList;
import com.kesierzg.securevault.model.PasswordEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.File;
import java.io.IOException;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;

public class VaultService {
    private final List<PasswordEntry> passwordEntries = new ArrayList<>();
    private final EncryptionService encryptionService = new EncryptionService();
    private SecretKeySpec key;
    private byte[] iv;
    private byte[] salt;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<NoteEntry> notes = new ArrayList<>();

    public List<NoteEntry> getNotes() {
        return new ArrayList<>(notes);
    }

    public void addNote(String title, String content) {
        notes.add(new NoteEntry(title, content));
    }

    public boolean removeNoteByTitle(String title) {
        return notes.removeIf(n -> n.getTitle().equals(title));
    }

    public boolean editNote(String oldTitle, String newTitle, String newContent) {
        for (int i = 0; i < notes.size(); i++) {
            NoteEntry n = notes.get(i);
            if (n.getTitle().equals(oldTitle)) {
                notes.set(i, new NoteEntry(newTitle, newContent));
                return true;
            }
        }
        return false;
    }

    public VaultService(String masterPassword) {
        try {
            this.salt = encryptionService.generateSalt();
            this.key = encryptionService.generateKeyFromPassword(masterPassword, salt);
            this.iv = encryptionService.generateIv();
        } catch (Exception e) {
            throw new RuntimeException("nie udao sie zaszyfrowac :(", e);
        }
    }

    public VaultService(String masterPassword, byte[] salt, byte[] iv) {
        try {
            this.salt = salt;
            this.key = encryptionService.generateKeyFromPassword(masterPassword, salt);
            this.iv = iv;
        } catch (Exception e) {
            throw new RuntimeException("nie udao sie zaszyfrowac :(", e);
        }
    }

    public void addEntry(String website, String username, String password) {
        try {
            String encUsername = encryptionService.encrypt(username, key, iv);
            String encPassword = encryptionService.encrypt(password, key, iv);
            passwordEntries.add(new PasswordEntry(website, encUsername, encPassword));
        } catch (Exception e) {
            throw new RuntimeException("nie udao sie zaszyfrowac :(", e);
        }
    }

    public List<PasswordEntry> getEntries() {
        List<PasswordEntry> decrypted = new ArrayList<>();
        for (PasswordEntry entry : passwordEntries) {
            try {
                String decUsername = encryptionService.decrypt(entry.getUsername(), key, iv);
                String decPassword = encryptionService.decrypt(entry.getPassword(), key, iv);
                decrypted.add(new PasswordEntry(entry.getWebsite(), decUsername, decPassword));
            } catch (Exception e) {
                throw new RuntimeException("nie udao sie rozszyfrowac :(", e);
            }
        }
        return decrypted;
    }

    public void clearAll() {
        passwordEntries.clear();
    }

    public void saveToFile(File file) {
        try {
            EncryptedVaultData data = new EncryptedVaultData(
                    salt,
                    iv,
                    passwordEntries,
                    notes
            );
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
        } catch (IOException e) {
            throw new RuntimeException("nie udao sie zapisac zmian", e);
        }
    }

    public static VaultService loadFromFile(File file, String masterPassword) {
        try {
            System.out.println("wczytujeee: " + file.getAbsolutePath());
            ObjectMapper mapper = new ObjectMapper();
            EncryptedVaultData data = mapper.readValue(file, EncryptedVaultData.class);
            VaultService vault = new VaultService(masterPassword, data.getSaltBytes(), data.getIvBytes());
            vault.passwordEntries.addAll(data.entries);
            if (data.notes != null) {
                vault.notes.clear();
                vault.notes.addAll(data.notes);
            }
            return vault;
        } catch (IOException e) {
            System.err.println("NiE wCzYtAnOoO s PoWodU blEeeEndUUuu :((" + e.getMessage());
            throw new RuntimeException("eRooR BaaaZyyy dAAnych", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EncryptedVaultData {
        public String salt;
        public String iv;
        public List<PasswordEntry> entries;
        public List<NoteEntry> notes;

        @JsonCreator
        public EncryptedVaultData(
                @JsonProperty("salt") String salt,
                @JsonProperty("iv") String iv,
                @JsonProperty("entries") List<PasswordEntry> entries,
                @JsonProperty("notes") List<NoteEntry> notes) {
            this.salt = salt;
            this.iv = iv;
            this.entries = entries;
            this.notes = notes != null ? notes : new ArrayList<>();
        }

        public EncryptedVaultData(byte[] salt, byte[] iv, List<PasswordEntry> entries, List<NoteEntry> notes) {
            this.salt = Base64.getEncoder().encodeToString(salt);
            this.iv = Base64.getEncoder().encodeToString(iv);
            this.entries = entries;
            this.notes = notes != null ? notes : new ArrayList<>();
        }
        @JsonIgnore
        public byte[] getSaltBytes() {
            return Base64.getDecoder().decode(salt);
        }
        @JsonIgnore
        public byte[] getIvBytes() {
            return Base64.getDecoder().decode(iv);
        }
    }

    public void changeMasterPassword(String newPassword) {
        try {
            List<PasswordEntry> decryptedEntries = getEntries();
            byte[] newSalt = encryptionService.generateSalt();
            byte[] newIv = encryptionService.generateIv();
            SecretKeySpec newKey = encryptionService.generateKeyFromPassword(newPassword, newSalt);
            List<PasswordEntry> reEncrypted = new ArrayList<>();
            for (PasswordEntry entry : decryptedEntries) {
                String encUsername = encryptionService.encrypt(entry.getUsername(), newKey, newIv);
                String encPassword = encryptionService.encrypt(entry.getPassword(), newKey, newIv);
                reEncrypted.add(new PasswordEntry(entry.getWebsite(), encUsername, encPassword));
            }
            this.passwordEntries.clear();
            this.passwordEntries.addAll(reEncrypted);
            this.key = newKey;
            this.iv = newIv;
            this.salt = newSalt;
        } catch (Exception e) {
            throw new RuntimeException("nie udao sie zminic hasua :(", e);
        }
    }

    public void removeEntry(PasswordEntry entry) {
        passwordEntries.remove(entry);
    }

    public boolean removeEntryByWebsite(String website) {
        return passwordEntries.removeIf(e -> e.getWebsite().equals(website));
    }

    public boolean editEntry(String oldWebsite, String newWebsite, String newUsername, String newPassword) {
        for (int i = 0; i < passwordEntries.size(); i++) {
            PasswordEntry e = passwordEntries.get(i);
            if (e.getWebsite().equals(oldWebsite)) {
                try {
                    String encUsername = encryptionService.encrypt(newUsername, key, iv);
                    String encPassword = encryptionService.encrypt(newPassword, key, iv);
                    passwordEntries.set(i, new PasswordEntry(newWebsite, encUsername, encPassword));
                    return true;
                } catch (Exception ex) {
                    throw new RuntimeException("nie udao sie zaszyfrofac pliku :(", ex);
                }
            }
        }
        return false;
    }
}