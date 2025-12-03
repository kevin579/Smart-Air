package com.example.SmartAirGroup2.Helpers;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * A helper class for managing a simple integer state using SharedPreferences.
 * This class provides a convenient way to save and retrieve a single integer value
 * associated with a specific preference file name. It is useful for persisting
 * simple flags or states across app sessions, such as the position of a ViewPager.
 */
public class SaveState {

    Context context;
    String saveName;
    SharedPreferences sp;

    /**
     * Constructs a new SaveState helper.
     *
     * @param context  The application context, used to access SharedPreferences.
     * @param saveName The name of the preference file where the state will be stored.
     */
    public SaveState(Context context, String saveName) {
        this.context = context;
        this.saveName = saveName;
        sp = context.getSharedPreferences(saveName, Context.MODE_PRIVATE);
    }

    /**
     * Saves an integer value to SharedPreferences.
     * The value is stored with the hardcoded key "Key".
     *
     * @param value The integer value to be saved.
     */
    public void setState(int value){
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("Key", value);
        editor.apply();
    }

    /**
     * Retrieves the saved integer value from SharedPreferences.
     *
     * @return The saved integer value. If no value is found for the key "Key",
     *         it returns the default value of 0.
     */
    public int getState(){
        return sp.getInt("Key", 0);
    }
}
