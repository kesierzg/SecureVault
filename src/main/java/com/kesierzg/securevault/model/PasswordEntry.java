package com.kesierzg.securevault.model;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PasswordEntry {
    private String website;
    private String username;
    private String password;

    @JsonCreator
    public PasswordEntry(
            @JsonProperty("website") String website,
            @JsonProperty("username") String username,
            @JsonProperty("password") String password
    ) {
        this.website = website;
        this.username = username;
        this.password = password;
    }

    public String getWebsite() { return website; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}