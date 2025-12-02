package com.example.SmartAirGroup2.Helpers;

import java.util.HashMap;
import java.util.Map;

public class TriageIncident {
    public String PEF;
    public String guidance;
    public String response;
    public String time;
    public Map<String, Boolean> redflags = new HashMap<>();


    public TriageIncident() {
    }
}
