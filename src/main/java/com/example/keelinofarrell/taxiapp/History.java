package com.example.keelinofarrell.taxiapp;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.keelinofarrell.taxiapp.HistoryRecyclerViewInfo.HistoryAdapter;
import com.example.keelinofarrell.taxiapp.HistoryRecyclerViewInfo.HistoryObject;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class History extends AppCompatActivity {

    private RecyclerView mHistoryRecycler;
    private RecyclerView.Adapter mHistoryAdapter;
    private RecyclerView.LayoutManager mHistoryLayoutManager;
    private String customersOrDriver, userId;
    private TextView mBalance;
    double balance;

    private Button mPayout;
    private EditText mPayoutEt;

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

        mPayoutEt = (EditText)findViewById(R.id.payoutEmail);

        mPayout = (Button)findViewById(R.id.payoutButton);

        if(customersOrDriver.equals("Drivers")){
            mBalance.setVisibility(View.VISIBLE);

        }

        mPayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                payoutRequest();
            }
        });


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
                   balance = 0.0;
                   double journeyPrice = 0.0;
                   String price1 = "";

                    if(dataSnapshot.child("time").getValue() != null){
                        timestamp = Long.valueOf(dataSnapshot.child("time").getValue().toString());
                    }

                    if(dataSnapshot.child("CustomerPaid").getValue() != null ){
                        if(dataSnapshot.child("distance").getValue() != null && dataSnapshot.child("price") != null){
                            price1 = dataSnapshot.child("price").getValue().toString();
                            journeyPrice = Double.valueOf(price1);
                            balance += journeyPrice;
                            mBalance.setText("Total Earned: €" + new DecimalFormat("##.##").format(balance));

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


    public static final MediaType MEDIA_TYPE = MediaType.parse("application/json");
    ProgressDialog progress;
    private void payoutRequest() {
        progress = new ProgressDialog(this);
        progress.setTitle("Proccessing your payout");
        progress.setMessage("Please wait");
        progress.setCancelable(false);
        progress.show();

        final OkHttpClient client = new OkHttpClient();

        JSONObject postData = new JSONObject();
        try {
            postData.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
            postData.put("email", mPayoutEt.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(MEDIA_TYPE, postData.toString());

        final Request request = new Request.Builder()
                .url("https://us-central1-taxiapp-48933.cloudfunctions.net/payout")
                .post(body)
                .addHeader("Content-type", "application/json")
                .addHeader("cache-control", "no-cache")
                .addHeader("Authorization", "Your token")
                .build();

        //client to make the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                progress.dismiss();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                int responseCode = response.code();
                if(response.isSuccessful()){
                    switch(responseCode){
                        case 200:
                            Snackbar.make(findViewById(R.id.layout), "Payout successful", Snackbar.LENGTH_LONG).show();
                            break;
                        case 500:
                            Snackbar.make(findViewById(R.id.layout), "Error: Could not complete payout", Snackbar.LENGTH_LONG).show();
                            break;
                        default:
                            Snackbar.make(findViewById(R.id.layout), "Error: Could not complete payout", Snackbar.LENGTH_LONG).show();
                            break;
                    }

                }
                else
                    Snackbar.make(findViewById(R.id.layout), "Error: couldn't complete the transaction", Snackbar.LENGTH_LONG).show();
                    progress.dismiss();
            }
        });

    }

    private ArrayList resultHistory = new ArrayList<HistoryObject>();

    private ArrayList<HistoryObject> getDataSetHistory() {
        return resultHistory;
    }
}
