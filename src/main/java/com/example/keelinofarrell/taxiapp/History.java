package com.example.keelinofarrell.taxiapp;

import android.annotation.SuppressLint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TextView;

import com.example.keelinofarrell.taxiapp.HistoryRecyclerViewInfo.HistoryAdapter;
import com.example.keelinofarrell.taxiapp.HistoryRecyclerViewInfo.HistoryObject;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;


public class History extends AppCompatActivity {

    private RecyclerView mHistoryRecycler;
    private RecyclerView.Adapter mHistoryAdapter;
    private RecyclerView.LayoutManager mHistoryLayoutManager;
    private String customersOrDriver, userId;
    private TextView mBalance;
    private double balance = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_history);

        mHistoryRecycler = (RecyclerView)findViewById(R.id.historyRecycler);
        //ensure scroll is fluid
        mHistoryRecycler.setNestedScrollingEnabled(false);

        mHistoryRecycler.setHasFixedSize(true);

        mHistoryLayoutManager = new LinearLayoutManager(History.this);
        mHistoryRecycler.setLayoutManager(mHistoryLayoutManager);
        mHistoryAdapter = new HistoryAdapter(getDataSetHistory(), History.this);
        mHistoryRecycler.setAdapter(mHistoryAdapter);

        customersOrDriver = getIntent().getExtras().getString("customersOrDriver");

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        getUserHistoryIds();

        mBalance = (TextView)findViewById(R.id.balance);

        if(customersOrDriver.equals("Drivers")){
            mBalance.setVisibility(View.VISIBLE);
        }



    }

    private void getUserHistoryIds() {
        DatabaseReference userHistDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(customersOrDriver).child(userId).child("history");
        userHistDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    //for each data snap shot it will get one child and pass it into history
                    //and then go to the next and pass to history etc.
                    for(DataSnapshot history : dataSnapshot.getChildren() ){
                        GetDriveInfo(history.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void GetDriveInfo(String driveKey) {
        DatabaseReference historyDatabase = FirebaseDatabase.getInstance().getReference().child("history").child(driveKey);
        historyDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                   String driveId = dataSnapshot.getKey();
                   Long timestamp = 0L;
                   String mDistance = "";
                   double journeyPrice = 0.0;

                    if(dataSnapshot.child("time").getValue() != null){
                        timestamp = Long.valueOf(dataSnapshot.child("time").getValue().toString());
                    }

                    if(dataSnapshot.child("CustomerPaid").getValue() != null && dataSnapshot.child("DriverPaid") == null){
                        if(dataSnapshot.child("distance").getValue() !=null){
                            mDistance = dataSnapshot.child("distance").getValue().toString();
                            journeyPrice = (Double.valueOf(mDistance) * 0.4);
                            balance += journeyPrice;
                            mBalance.setText("Balance: €" + String.valueOf(balance));
                        }

                    }

                   HistoryObject historyObject = new HistoryObject(driveId, getDate(timestamp));
                   resultHistory.add(historyObject);
                   mHistoryAdapter.notifyDataSetChanged();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private String getDate(Long timestamp) {
        //get the default time zone of the phone
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timestamp*1000);
        String date = DateFormat.format("dd-MM-yyyy hh:mm", cal).toString();
        return date;
    }

    private ArrayList resultHistory = new ArrayList<HistoryObject>();

    private ArrayList<HistoryObject> getDataSetHistory() {
        return resultHistory;
    }
}
