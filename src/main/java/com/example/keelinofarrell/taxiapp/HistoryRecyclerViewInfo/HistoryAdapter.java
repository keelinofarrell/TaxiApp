package com.example.keelinofarrell.taxiapp.HistoryRecyclerViewInfo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.keelinofarrell.taxiapp.R;

import java.util.List;

import static com.example.keelinofarrell.taxiapp.R.layout.itemhistory;

/**
 * Created by keelin.ofarrell on 02/02/2018.
 */

public class HistoryAdapter extends RecyclerView.Adapter<HistoryViewHolders> {

    private List<HistoryObject> itemList;
    private Context context;

    public HistoryAdapter(List<HistoryObject> itemList, Context context) {
        this.itemList = itemList;
        this.context = context;
    }


    @Override
    public HistoryViewHolders onCreateViewHolder(ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(itemhistory, null);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);
        HistoryViewHolders hvh = new HistoryViewHolders(layoutView);
        return hvh;
    }

    @Override
    public void onBindViewHolder(HistoryViewHolders holder, int position) {
        holder.driveId.setText(itemList.get(position).getDriveId());
        if(itemList.get(position).getTime()!=null){
            holder.time.setText(itemList.get(position).getTime());
        }

    }

    @Override
    public int getItemCount() {
        return 0;
    }
}
