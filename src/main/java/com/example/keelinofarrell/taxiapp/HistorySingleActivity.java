package com.example.keelinofarrell.taxiapp;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback , RoutingListener {

    private GoogleMap mMap;
    private SupportMapFragment mMapFrag;
    private String driveId, currentUserId, customerId, driverId, userDriverOrCustomer, distance1;
    private TextView location, distance, date, username, phone;
    private ImageView imageUser;
    private DatabaseReference driveHistory;
    private LatLng destinationLatLng, pickupLatLng;
    private List<Polyline> polylines;
    private static final int[] COLOURS = new int[]{R.color.primary_dark_material_light};
    private String username1;
    private RatingBar ratingBar;
    private double journeyPrice;
    private Button mPayment;
    private Boolean customerPaid = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);

        Intent intent = new Intent(this, PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        startService(intent);

        polylines = new ArrayList<>();

        driveId = getIntent().getExtras().getString("driveId");

        mMapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFrag.getMapAsync(this);


        location = (TextView)findViewById(R.id.location);
        distance = (TextView)findViewById(R.id.distance);
        date = (TextView)findViewById(R.id.date);
        username = (TextView)findViewById(R.id.username);
        phone = (TextView)findViewById(R.id.userphone);

        imageUser = (ImageView)findViewById(R.id.userImage);

        ratingBar = (RatingBar)findViewById(R.id.rating);

        mPayment = (Button)findViewById(R.id.payment);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        driveHistory = FirebaseDatabase.getInstance().getReference().child("history").child(driveId);
        getDriveInfo();


    }

    private void getDriveInfo() {
        driveHistory.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot child : dataSnapshot.getChildren()){
                        if(child.getKey().equals("customer")){
                            customerId = child.getValue().toString();
                            if(!customerId.equals(currentUserId)){
                                userDriverOrCustomer = "Drivers";
                                getUserInfo("Customers" , customerId);
                            }
                        }
                        if(child.getKey().equals("driver")){
                            driverId = child.getValue().toString();
                            if(!driverId.equals(currentUserId)){
                                userDriverOrCustomer = "Customers";
                                getUserInfo("Drivers" ,driverId);
                                getCustomerRelatedObjects();
                            }
                        }
                        if(child.getKey().equals("time")){
                            date.setText(getDate(Long.valueOf(child.getValue().toString())));
                        }
                        if(child.getKey().equals("rating")){
                            ratingBar.setRating(Integer.valueOf(child.getValue().toString()));
                        }
                        if(child.getKey().equals("CustomerPaid")){
                            customerPaid = true;
                        }
                        if(child.getKey().equals("distance")){
                            distance1 = child.getValue().toString();
                            distance.setText(distance1.substring(0, Math.min(distance1.length(), 5)) + " km");
                            journeyPrice = Double.valueOf(distance1) * 0.5;

                        }
                        if(child.getKey().equals("destination")){
                            location.setText(child.getValue().toString());
                        }

                        if(child.getKey().equals("location")){
                            pickupLatLng = new LatLng(Double.valueOf(child.child("from").child("lat").getValue().toString()) , (Double.valueOf(child.child("from").child("lng").getValue().toString())));
                            destinationLatLng = new LatLng(Double.valueOf(child.child("to").child("lat").getValue().toString()) , (Double.valueOf(child.child("to").child("lng").getValue().toString())));
                            if(destinationLatLng != new LatLng(0,0)){
                                getRouteToMarker();
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getCustomerRelatedObjects() {
        ratingBar.setVisibility(View.VISIBLE);
        mPayment.setVisibility(View.VISIBLE);
        mPayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                paypalPayment();
            }
        });

        if(customerPaid){
            mPayment.setEnabled(false);

        }else{
            mPayment.setEnabled(true);
        }
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                driveHistory.child("rating").setValue(v);
                DatabaseReference mDriverDb = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("rating");
                mDriverDb.child(driveId).setValue(v);
            }
        });

    }


    private int PAYPAL_REQUEST_CODE = 1;
    private static PayPalConfiguration config = new PayPalConfiguration()
            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
            .clientId(PaypalConfiguration.PAYPAL_CLIENT_ID);

    private void paypalPayment() {

        PayPalPayment payment = new PayPalPayment(new BigDecimal(journeyPrice), "USD", "ScoopYouUp Payment", PayPalPayment.PAYMENT_INTENT_SALE);
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);

        //recieve if the payment was successful
        startActivityForResult(intent, PAYPAL_REQUEST_CODE);

    }

    //method for after we recieve if the payment was successful
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PAYPAL_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
                PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
                if(confirm != null){
                    try{
                        //GET DATA THAT IS RETURNED
                        JSONObject jsonObject = new JSONObject(confirm.toJSONObject().toString());

                        String paymentResponse = jsonObject.getJSONObject("response").getString("state");
                        if(paymentResponse.equals("approved")){
                            Toast.makeText(getApplicationContext(), "Payment Successful", Toast.LENGTH_LONG).show();
                            driveHistory.child("CustomerPaid").setValue(true);
                            mPayment.setEnabled(false);
                        }
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            }else{
                Toast.makeText(getApplicationContext(), "Payment Unsuccessful", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {

        stopService(new Intent(this, PayPalService.class));
        super.onDestroy();
    }

    private void getUserInfo(String otherDriverOrUser, String otherUserId) {
        DatabaseReference otherUser = FirebaseDatabase.getInstance().getReference().child("Users").child(otherDriverOrUser).child(otherUserId);
        otherUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null) {
                        username1 = map.get("name").toString();
                        username.setText(username1);
                        System.out.println("username = " + username);

                    }
                    if (map.get("number") != null) {
                        phone.setText(map.get("number").toString());
                    }
                    else
                    {
                        System.out.println("Number cannot be found");
                    }
                    if (map.get("profileImageUrl") != null) {
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(imageUser);
                    }
                }
                else{
                    System.out.println("does not exist in this map");
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

    //taken from GitHub to get the route between driver and pickup location
    private void getRouteToMarker() {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(pickupLatLng, destinationLatLng)
                .build();
        routing.execute();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    //draw route
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {

        //to zoom down onto the markers on the map
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(pickupLatLng);
        builder.include(destinationLatLng);
        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int padding = (int)(width*0.2);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);

        mMap.animateCamera(cameraUpdate);

        mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pin_pickup)));
        mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destintion").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pin_destination)));


        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLOURS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLOURS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRoutingCancelled() {

    }

    //clear route from map
    private void erasePolyLines(){
        for(Polyline line : polylines){
            line.remove();
        }
        polylines.clear();
    }
}
