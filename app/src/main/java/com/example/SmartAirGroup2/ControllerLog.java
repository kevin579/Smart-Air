package com.example.SmartAirGroup2;

public class ControllerLog {

    public long timestamp;
    public String medication;

    public ControllerLog() {

    }

    public ControllerLog(long timestamp, String medication) {
        this.timestamp = timestamp;
        this.medication = medication;
    }
}
