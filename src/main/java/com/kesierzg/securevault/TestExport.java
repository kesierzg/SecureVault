package com.kesierzg.securevault;
import com.kesierzg.securevault.model.PasswordEntry;
import com.kesierzg.securevault.service.ExportService;
import com.kesierzg.securevault.service.VaultService;
import java.io.File;

public class TestExport {
    public static void main(String[] args) {
        VaultService vault = new VaultService("testmaster");
        vault.addEntry("https://gmail.com", "jan.kowalski", "superhaslo123");
        vault.addEntry("https://facebook.com", "janek", "1234");
        vault.addEntry("https://wykop.pl", "kasztan", "georgebushdid911");

        ExportService exporter = new ExportService();
        try {
            exporter.exportToBitwardenFormat(vault.getEntries(), new File("export-bitwarden.json"));
            System.out.println("zeskportowano");
        } catch (Exception e) {
            System.out.println("błooo000nt eksportu xD " + e.getMessage());
        }
    }
}