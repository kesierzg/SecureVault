package com.kesierzg.securevault.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.kesierzg.securevault.model.PasswordEntry;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ExportService {

    public void exportToBitwarden(List<PasswordEntry> entries, File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode();
        root.putArray("folders");

        ArrayNode items = mapper.createArrayNode();

        for (PasswordEntry entry : entries) {
            ObjectNode item = mapper.createObjectNode();
            item.put("type", 1);
            item.put("name", entry.getWebsite());

            ObjectNode login = mapper.createObjectNode();
            login.put("username", entry.getUsername());
            login.put("password", entry.getPassword());

            ArrayNode uris = mapper.createArrayNode();
            uris.add(entry.getWebsite());
            login.set("uris", uris);

            item.set("login", login);
            item.put("notes", "");
            item.put("favorite", false);

            items.add(item);
        }
        root.set("items", items);
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
    }
}