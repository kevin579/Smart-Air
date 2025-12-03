package com.example.SmartAirGroup2.Helpers;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single triage incident, capturing key data points related to an asthma event.
 * This class is used to store and transport information about a user's symptoms and measurements
 * when they use the one-tap triage feature.
 */
public class TriageIncident {

    public String PEF;

    public String guidance;


    public String response;


    public String time;

    /**
     * A map of red flag symptoms observed during the incident.
     * The key is the symptom name (e.g., "Chest Pulling or Retraction") and the value is a boolean
     * indicating whether the symptom was present.
     */
    public Map<String, Boolean> redflags = new HashMap<>();

    /**
     * Default constructor.
     * Required for Firebase and other deserialization frameworks.
     */
    public TriageIncident() {
    }
}
