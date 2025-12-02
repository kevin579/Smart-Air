package com.example.SmartAirGroup2.ParentDashboard;

import com.example.SmartAirGroup2.ChildDashboard.ControllerLog;
import com.example.SmartAirGroup2.ChildDashboard.ControllerSchedule;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
/**
 * AdherenceCalculator
 *
 * Pure Java utility class that computes controller medication adherence
 * for a single child over a given time window, based on:
 *  - A planned ControllerSchedule
 *  - A list of ControllerLog entries
 *
 * The calculator works in three main steps:
 *
 * 1) Aggregate actual doses per day:
 *    - For each ControllerLog within [startMillis, endMillis], group
 *      by calendar date ("yyyy-MM-dd") to build:
 *        date -> actual count of controller uses
 *
 * 2) Walk day-by-day from start to end:
 *    - For each day:
 *        expected = schedule.timesPerDay if that weekday is active
 *                   in schedule.daysOfWeek, otherwise 0.
 *        actual   = count from the aggregated map for that date (default 0).
 *        used     = min(actual, expected) so "over-dosing" does not
 *                   inflate adherence above 100%.
 *        percent  = used / expected * 100 for days with expected > 0.
 *      The calculator also accumulates totalExpected and totalActualUsed
 *      across all days where expected > 0.
 *
 * 3) Compute overall adherence:
 *    - overallPercent = totalActualUsed / totalExpected * 100
 *      (if totalExpected == 0, overallPercent is 0.0).
 *
 * Output:
 * - AdherenceResult:
 *      overallPercent : adherence across the entire window
 *      dailyList      : per-day expected/actual/percent details
 *
 * This class is shared by:
 * - ParentAdherenceActivity: for parents to see their child’s adherence.
 * - ProviderAdherenceActivity: for providers to see the same metric
 *   in a read-only summary.
 */

public class AdherenceCalculator {
    public static class DailyAdherence {
        public String date;   // "yyyy-MM-dd"
        public int expected;
        public int actual;
        public double percent;

        public DailyAdherence(String date, int expected, int actual, double percent) {
            this.date = date;
            this.expected = expected;
            this.actual = actual;
            this.percent = percent;
        }
    }

    public static class AdherenceResult {
        public double overallPercent;
        public List<DailyAdherence> dailyList;

        public AdherenceResult(double overallPercent, List<DailyAdherence> dailyList) {
            this.overallPercent = overallPercent;
            this.dailyList = dailyList;
        }
    }

    public static AdherenceResult calculate(ControllerSchedule schedule, List<ControllerLog> logs, long startMillis, long endMillis) {
        SimpleDateFormat simple_date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Map<String, Integer> actualPerDay = new HashMap<String, Integer>();
        for (int i = 0; i < logs.size(); i++) {
            ControllerLog log = logs.get(i);
            long time = log.timestamp;

            if (time < startMillis || time > endMillis) {
                continue;
            }


            Date date = new Date(time);
            String dateKey = simple_date.format(date);   // i.e. "2025-11-29"

            Integer count = actualPerDay.get(dateKey);
            if (count == null) {
                count = 0;
            }
            count = count + 1;
            actualPerDay.put(dateKey, count);
        }
        List<DailyAdherence> dailyList = new ArrayList<DailyAdherence>();

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startMillis);
        setToDefaultDay(cal);   // turn time to 00:00:00

        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(endMillis);
        setToDefaultDay(endCal);

        int totalExpected = 0;
        int totalActualUsed = 0;

        while (!cal.after(endCal)) {

            Date currentDate = cal.getTime();
            String dateKey = simple_date.format(currentDate);

            // calculate expected
            int expected = 0;
            if (schedule != null && schedule.daysOfWeek != null) {
                String dowKey = getDayOfWeekKey(cal); // "MON"/"TUE"...
                Boolean active = schedule.daysOfWeek.get(dowKey);
                if (active != null && active.booleanValue()) {
                    expected = schedule.timesPerDay;
                }
            }

            // calculate actual
            int actual = 0;
            Integer count = actualPerDay.get(dateKey);
            if (count != null) {
                actual = count.intValue();
            }

            // should not exceed expected num
            int used = actual;
            if (used > expected) {
                used = expected;
            }

            double percent = 0.0;
            if (expected > 0) {
                percent = (used * 100.0) / expected;
                totalExpected = totalExpected + expected;
                totalActualUsed = totalActualUsed + used;
            }

            DailyAdherence dayResult = new DailyAdherence(dateKey, expected, actual, percent);
            dailyList.add(dayResult);

            // next day
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        double overall = 0.0;
        if (totalExpected > 0) {
            overall = (totalActualUsed * 100.0) / totalExpected;
        }

        AdherenceResult result = new AdherenceResult(overall, dailyList);
        return result;
    }

    private static void setToDefaultDay(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }
    private static String getDayOfWeekKey(Calendar cal) {
        int dow = cal.get(Calendar.DAY_OF_WEEK); // SUNDAY=1, MONDAY=2, ...
        if (dow == Calendar.MONDAY) {
            return "MON";
        } else if (dow == Calendar.TUESDAY) {
            return "TUE";
        } else if (dow == Calendar.WEDNESDAY) {
            return "WED";
        } else if (dow == Calendar.THURSDAY) {
            return "THU";
        } else if (dow == Calendar.FRIDAY) {
            return "FRI";
        } else if (dow == Calendar.SATURDAY) {
            return "SAT";
        } else {
            // SUNDAY
            return "SUN";
        }
    }


}
