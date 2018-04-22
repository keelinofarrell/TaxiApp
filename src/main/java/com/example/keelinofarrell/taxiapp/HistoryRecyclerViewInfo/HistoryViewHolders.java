package com.example.keelinofarrell.taxiapp.HistoryRecyclerViewInfo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.example.keelinofarrell.taxiapp.HistorySingleActivity;
import com.example.keelinofarrell.taxiapp.R;

/**
 * Created by keelin.ofarrell on 02/02/2018.
 */

public class HistoryViewHolders extends RecyclerView.ViewHolder implements View.OnClickListener {

    public TextView driveId, time;

    public HistoryViewHolders(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);

        driveId = (TextView) itemView.findViewById(R.id.driveId);
        time = (TextView) itemView.findViewById(R.id.time);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(view.getContext(), HistorySingleActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("driveId", ((TextView)view.findViewById(R.id.driveId)).getText().toString());
        intent.putExtras(bundle);
        view.getContext().startActivity(intent);
    }
}
