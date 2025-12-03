package com.example.SmartAirGroup2.Helpers;

import java.util.ArrayList;

/**
 * Represents a parent user, extending the base {@link User} class.
 * This class includes a list of associated children's usernames.
 */
public class Parent extends User {
    private ArrayList<String> children = new ArrayList<String>();

    /**
     * Default constructor required for Firebase deserialization.
     */
    public Parent() {}

    /**
     * Constructs a new Parent object with the specified details.
     *
     * @param uname    The username of the parent.
     * @param name     The full name of the parent.
     * @param email    The email address of the parent.
     * @param password The password for the parent's account.
     * @param type     The user type, typically "parent".
     */
    public Parent(String uname, String name, String email, String password, String type) {
        super(uname,name,email,password,type);
    }

    /**
     * Returns the list of children's usernames associated with this parent.
     *
     * @return An {@link ArrayList} of strings, where each string is a child's username.
     */
    public ArrayList<String> getChildren() { return children; }

    /**
     * Adds a child's username to this parent's list of children.
     *
     * @param uname The username of the child to add.
     */
    public void addChild(String uname) { this.children.add(uname); }

    /**
     * Removes a child's username from this parent's list of children.
     *
     * @param uname The username of the child to remove.
     */
    public void removeChild(String uname) { this.children.remove(uname); }
}
