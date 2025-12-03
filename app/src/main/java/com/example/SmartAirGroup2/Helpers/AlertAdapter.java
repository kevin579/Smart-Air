package com.example.SmartAirGroup2.Helpers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import com.example.SmartAirGroup2.ParentDashboard.Alert;
import com.example.SmartAirGroup2.R;
import com.google.android.material.card.MaterialCardView;
import androidx.core.content.ContextCompat;

/**
 * A RecyclerView adapter for displaying a list of {@link Alert} objects.
 * This adapter is responsible for creating and binding the views for each alert item
 * in the RecyclerView. It also applies specific styling to alerts based on their title
 * to indicate different levels of severity (e.g., warning, error).
 */
public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder>{
    /**
     * The list of alerts to be displayed by the adapter.
     */
    private List<Alert> alerts;

    /**
     * Constructs a new AlertAdapter.
     *
     * @param alert A list of {@link Alert} objects to be displayed.
     */
    public AlertAdapter(List<Alert> alert){
        this.alerts = alert;
    }

    /**
     * Called when RecyclerView needs a new {@link AlertViewHolder} of the given type to represent
     * an item.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new AlertViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert, parent, false);
        return new AlertViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the {@link AlertViewHolder#itemView} to reflect the item at the given
     * position. It sets the alert's title, message, and timestamp. It also dynamically changes
     * the card's border and title color based on the alert's title to visually distinguish
     * between different alert types (e.g., warnings for low medicine, errors for PEF safety alerts).
     *
     * @param holder   The AlertViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position){
        Alert alert = alerts.get(position);
        Context ctx = holder.itemView.getContext();

        holder.textView_Title.setText(alert.title);
        holder.textView_Message.setText(alert.message);
        holder.textView_Time.setText(alert.getFormattedTime());

        // Reset to default styles first
        int defaultBorder = ContextCompat.getColor(ctx, android.R.color.darker_gray);
        int defaultTitle = ContextCompat.getColor(ctx, android.R.color.black);
        holder.card_Alert.setStrokeColor(defaultBorder);
        holder.textView_Title.setTextColor(defaultTitle);

        // Apply conditional styling for specific alert types
        if ("Medicine Low".equals(alert.title)||"Medicine Expired".equals(alert.title)) {
            int border = ContextCompat.getColor(ctx, R.color.alert_warning_border);
            int titleColor = ContextCompat.getColor(ctx, R.color.alert_warning_text);
            holder.card_Alert.setStrokeColor(border);
            holder.textView_Title.setTextColor(titleColor);
        } else if ("PEF Safety Alert".equals(alert.title)) {
            int border = ContextCompat.getColor(ctx, R.color.alert_error_border);
            int titleColor = ContextCompat.getColor(ctx, R.color.alert_error_text);
            holder.card_Alert.setStrokeColor(border);
            holder.textView_Title.setTextColor(titleColor);
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount(){
        return alerts.size();
    }

    /**
     * A ViewHolder describes an item view and metadata about its place within the RecyclerView.
     * It holds the references to the UI components for each item in the list.
     */
    static class AlertViewHolder extends RecyclerView.ViewHolder{
        TextView textView_Title;
        TextView textView_Message;
        TextView textView_Time;
        MaterialCardView card_Alert;


        /**
         * Constructs a new AlertViewHolder.
         *
         * @param itemView The view that you inflated in {@link #onCreateViewHolder(ViewGroup, int)}.
         */
        public AlertViewHolder(@NonNull View itemView){
            super(itemView);
            textView_Title =    itemView.findViewById(R.id.tvAlertTitle);
            textView_Message =    itemView.findViewById(R.id.tvAlertMessage);
            textView_Time =    itemView.findViewById(R.id.tvAlertTime);
            card_Alert = itemView.findViewById(R.id.cardAlert);

        }
    }


}
