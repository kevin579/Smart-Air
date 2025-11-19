package com.example.SmartAirGroup2;


public class Alert {
    public String title; // briefly explain alert type. (e.g. "Medicine Low")
    public String message; // alert details. (e.g. "Ventolin for perry is running low")
    public String child_name;
    public long timestamp; // timestamp.

    public Alert(){

    }

    public Alert(String title, String message,long timestamp){
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
    }

}
