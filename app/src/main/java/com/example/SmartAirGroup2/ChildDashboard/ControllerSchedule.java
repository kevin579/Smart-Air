package com.example.SmartAirGroup2.ChildDashboard;

import java.util.Map;
/**
 * ControllerSchedule
 *
 * Data model representing a parent's planned controller medication schedule
 * for a single child. This object is stored in Firebase under:
 *
 *   categories/users/children/{childUname}/controller_schedule
 *
 * Fields:
 * - medication: Human-readable label for the controller medication
 *               (e.g., "Flovent 110 µg"). Currently used for display only.
 * - timesPerDay: Number of planned controller doses per active day
 *                (e.g., 2 times per day).
 * - daysOfWeek: Map from weekday key ("MON", "TUE", ..., "SUN") to boolean.
 *               true  = this day is part of the controller plan
 *               false = no controller expected on this day
 *
 * This model is used by both parent and provider sides:
 * - Parent side: editable in the "Manage Controller Schedule" card.
 * - Provider side: read-only display, so providers can see the plan
 *                  that the family is supposed to follow.
 */
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
