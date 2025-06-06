package com.kesierzg.securevault;
import com.kesierzg.securevault.model.PasswordEntry;
import com.kesierzg.securevault.service.ImportService;
import java.io.File;
import java.util.List;

public class TestImport {
    public static void main(String[] args) {
        File file = new File("export-bitwarden.json");

        ImportService importService = new ImportService();
        try {
            List<PasswordEntry> entries = importService.importFromBitwarden(file);
            System.out.println("Zaimportowano: " + entries.size() + " wpisów");

            for (PasswordEntry entry : entries) {
                System.out.printf(" %s | %s | %s%n",
                        entry.getWebsite(),
                        entry.getUsername(),
                        entry.getPassword());
            }

        } catch (Exception e) {
            System.out.println("bło000oont importu: " + e.getMessage());
        }
    }
}