package com.example.keelinofarrell.taxiapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.ImageView;
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

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback , RoutingListener {

    private GoogleMap mMap;
    private SupportMapFragment mMapFrag;
    private String driveId, currentUserId, customerId, driverId, userDriverOrCustomer;
    private TextView location, distance, date, username, phone;
    private ImageView imageUser;
    private DatabaseReference driveHistory;
    private LatLng destinationLatLng, pickupLatLng;
    private List<Polyline> polylines;
    private static final int[] COLOURS = new int[]{R.color.primary_dark_material_light};
    private String username1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);

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
                            }
                        }
                        if(child.getKey().equals("time")){
                            date.setText(getDate(Long.valueOf(child.getValue().toString())));
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

    private void getUserInfo(String otherDriverOrUser, String otherUserId) {
        DatabaseReference otherUser = FirebaseDatabase.getInstance().getReference().child("Users").child(otherDriverOrUser).child(otherUserId);
        otherUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name") != null ){
                        username1 = map.get("name").toString();
                        username.setText(username1);

                    }
                    if(map.get("number") != null ){
                        phone.setText(map.get("number").toString());
                    }
                    if(map.get("profileImageUrl") != null ){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(imageUser);
                    }
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

        mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_action_name)));
        mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destintion").icon(BitmapDescriptorFactory.fromResource(R.mipmap.destination)));


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
