package com.example.SmartAirGroup2.models;

public class User {
    private String uname;

    private String name;
    private String email;

    private String password;

    private String type;

    public User() {}

    public User(String uname, String name, String email, String password, String type) {
        this.uname = uname;
        this.name = name;
        this.email = email;
        this.password = password;
        this.type = type;
    }


    public String getUname() { return uname; }
    public void setUname(String uname) { this.uname = uname; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

}

