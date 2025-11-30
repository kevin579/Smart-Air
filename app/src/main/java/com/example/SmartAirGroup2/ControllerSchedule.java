package com.example.SmartAirGroup2;

import java.util.Map;

public class ControllerSchedule {
    public String medication;
    public int timesPerDay;
    public Map<String, Boolean> daysOfWeek;

    public ControllerSchedule(){

    }
    public ControllerSchedule(String medication,
                              int timesPerDay,
                              Map<String, Boolean> daysOfWeek) {
        this.medication = medication;
        this.timesPerDay = timesPerDay;
        this.daysOfWeek = daysOfWeek;
    }
}
