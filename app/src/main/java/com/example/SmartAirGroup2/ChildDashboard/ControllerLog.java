package com.example.SmartAirGroup2.ChildDashboard;
/**
 * ControllerLog
 *
 * Lightweight model for a single controller medication use event
 * recorded for a child. These events are stored in Firebase under:
 *
 *   categories/users/children/{childUname}/logs/controller_log/{logId}
 *
 * Expected fields in Firebase:
 * - timestamp: Long. Epoch time in milliseconds when the controller
 *              medication was taken. Used to determine which calendar
 *              day the dose belongs to.
 * - medication: String. Name of the medication recorded at the time
 *               of use (e.g., "Flovent"). Currently not used as a
 *               strict filter in adherence calculation, but kept for
 *               display and possible future per-medication adherence.
 *
 * The adherence engine uses only the timestamp to aggregate how many
 * controller doses were actually taken on each day within a time window.
 */

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
