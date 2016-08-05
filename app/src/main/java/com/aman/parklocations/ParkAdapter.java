package com.aman.parklocations;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aman.parklocations.model.Park;

import java.util.List;

public class ParkAdapter extends RecyclerView.Adapter<ParkAdapter.ParkViewHolder> {
    private List<Park> parks;

    public ParkAdapter(List<Park> parks) {
        this.parks = parks;
    }

    @Override
    public ParkViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View listItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_park, parent, false);
        return new ParkViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(ParkViewHolder holder, int position) {
        Park park = parks.get(position);
        holder.bindData(park);
    }

    @Override
    public int getItemCount() {
        return parks.size();
    }


    class ParkViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private LinearLayout layout;
        private TextView parkName;
        private TextView parkMgr;
        private TextView parkEmail;
        private TextView parkPhone;

        public ParkViewHolder(View itemView) {
            super(itemView);

            layout = (LinearLayout)itemView.findViewById(R.id.list_item_layout);
            parkName = (TextView)itemView.findViewById(R.id.parkName);
            parkMgr = (TextView)itemView.findViewById(R.id.parkMgr);
            parkEmail = (TextView)itemView.findViewById(R.id.parkEmail);
            parkPhone = (TextView)itemView.findViewById(R.id.parkPhone);

            layout.setOnClickListener(this);
        }

        public void bindData(Park park) {
            parkName.setText(park.getName());
            parkMgr.setText(park.getManagerName());
            parkEmail.setText(park.getEmail());
            parkPhone.setText(park.getPhone());
        }

        @Override
        public void onClick(View view) {
            Log.v("Adapter", "onClick() called on " + ((TextView)((LinearLayout)view).getChildAt(0)).getText());

        }
    }
}

