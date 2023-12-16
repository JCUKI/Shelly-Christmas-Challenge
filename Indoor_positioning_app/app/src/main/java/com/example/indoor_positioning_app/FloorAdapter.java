package com.example.indoor_positioning_app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

//Implemented according to: https://guides.codepath.com/android/using-the-recyclerview
public class FloorAdapter extends RecyclerView.Adapter<FloorAdapter.ViewHolder> {
    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView nameTextView;
        public Button messageButton;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            nameTextView = (TextView) itemView.findViewById(R.id.floor_name);
            messageButton = (Button) itemView.findViewById(R.id.message_button);
        }
    }

    // Store a member variable for the floor
    private List<Floor> mFloors;

    // Pass in the contact array into the constructor
    public FloorAdapter(List<Floor> floors) {
        mFloors = floors;
    }

    // Usually involves inflating a layout from XML and returning the holder
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.item_floor, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(contactView);
        return viewHolder;
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the data model based on position
        Floor floor = mFloors.get(position);

        // Set item views based on your views and data model
        TextView textView = holder.nameTextView;
        textView.setText(floor.getName());
        Button button = holder.messageButton;
        button.setText("Show");
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return mFloors.size();
    }
}