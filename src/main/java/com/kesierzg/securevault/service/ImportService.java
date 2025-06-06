package com.kesierzg.securevault.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kesierzg.securevault.model.PasswordEntry;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImportService {

    public List<PasswordEntry> importFromBitwarden(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(file);
        List<PasswordEntry> result = new ArrayList<>();

        JsonNode items = root.get("items");
        if (items != null && items.isArray()) {
            for (JsonNode item : items) {
                if (item.get("type").asInt() == 1 && item.has("login")) { // 1 = login type
                    String site = item.get("name").asText("");
                    String username = item.get("login").get("username").asText("");
                    String password = item.get("login").get("password").asText("");

                    result.add(new PasswordEntry(site, username, password));
                }
            }
        }
        return result;
    }
}