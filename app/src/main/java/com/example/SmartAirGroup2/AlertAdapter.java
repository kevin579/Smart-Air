package com.example.SmartAirGroup2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.google.android.material.card.MaterialCardView;
import androidx.core.content.ContextCompat;


public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder>{
    private List<Alert> alerts;

    public AlertAdapter(List<Alert> alert){
        this.alerts = alert;
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position){
        Alert alert = alerts.get(position);
        Context ctx = holder.itemView.getContext();


        holder.textView_Title.setText(alert.title);
        holder.textView_Message.setText(alert.message);
        holder.textView_Time.setText(alert.getFormattedTime());

        int defaultBorder = ContextCompat.getColor(ctx, android.R.color.darker_gray);
        int defaultTitle = ContextCompat.getColor(ctx, android.R.color.black);
        holder.card_Alert.setStrokeColor(defaultBorder);
        holder.textView_Title.setTextColor(defaultTitle);

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

    @Override
    public int getItemCount(){
        return alerts.size();
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder{
        TextView textView_Title;
        TextView textView_Message;
        TextView textView_Time;
        MaterialCardView card_Alert;


        public AlertViewHolder(@NonNull View itemView){
            super(itemView);
            textView_Title =    itemView.findViewById(R.id.tvAlertTitle);
            textView_Message =    itemView.findViewById(R.id.tvAlertMessage);
            textView_Time =    itemView.findViewById(R.id.tvAlertTime);
            card_Alert = itemView.findViewById(R.id.cardAlert);

        }
    }


}
