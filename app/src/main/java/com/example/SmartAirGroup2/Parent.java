package com.example.SmartAirGroup2;
import java.util.ArrayList;
public class Parent extends User{
    private ArrayList<String> children = new ArrayList<String>();

    public Parent() {}

    public Parent(String uname, String name, String email, String password, String type) {
        super(uname,name,email,password,type);

    }


    public ArrayList<String> getChildren() { return children; }
    public void addChild(String uname) { this.children.add(uname); }
    public void removeChild(String uname) { this.children.remove(uname); }


}

