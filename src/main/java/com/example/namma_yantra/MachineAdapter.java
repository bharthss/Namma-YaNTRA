package com.example.namma_yantra;

import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class MachineAdapter extends RecyclerView.Adapter<MachineAdapter.ViewHolder> {

    List<String> list;

    public MachineAdapter(List<String> list) {
        this.list = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        Button bookBtn;

        public ViewHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.machineText);
            bookBtn = itemView.findViewById(R.id.bookBtn);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_machine, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        String machine = list.get(position);
        holder.text.setText(machine);

        holder.bookBtn.setOnClickListener(v -> {
            DatabaseReference db = FirebaseDatabase.getInstance().getReference("bookings");

            String id = db.push().getKey();
            db.child(id).setValue(machine);

            Toast.makeText(holder.itemView.getContext(), "Booked!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}