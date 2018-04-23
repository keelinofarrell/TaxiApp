package com.example.keelinofarrell.taxiapp;

import android.*;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Looper;
import android.os.TestLooperManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ticketmaster.api.discovery.DiscoveryApi;
import com.google.android.gms.location.LocationCallback;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback, RoutingListener {

    private GoogleMap mMap;
    Location mLastLocation, location;
    LocationRequest mLocationRequest;
    SupportMapFragment mapFragment;
    FusedLocationProviderClient mFusedLocationClient;
    private Button mLogout, mSettings, mDriveStatus, mHistory;
    RequestQueue queue;
    private String customerId = "";
    private String destination;
    private boolean isLoggingOut = false;
    private LinearLayout mCustomerInfo;
    private ImageView mCustomerProfileImage;
    private TextView mCustomerName, mCustomerNumber, mCustomerDestination;
    private int status = 0;
    private LatLng destinationLatLng, eventLatLng;
    private List<Polyline> polylines;
    private static final int[] COLOURS = new int[]{R.color.primary_dark_material_light};
    private LatLng pickupLatLng, userLatLng;
    public String apikey = "mzOuM4tYy3IrWOM3sOHsGaABAsHWNCo3";
    public DiscoveryApi api = new DiscoveryApi(apikey);
    ArrayList<Marker> markers;
    Marker userMarker;
    private Switch mswitch;
    private float drivedistance;
    private String distance;
    private double journeyPrice;
    boolean mswitch1 = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        polylines = new ArrayList<>();


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        mapFragment.getMapAsync(this);


        mCustomerInfo = (LinearLayout) findViewById(R.id.customerInfo);
        mCustomerProfileImage = (ImageView) findViewById(R.id.customerProfileImage);
        mCustomerName = (TextView) findViewById(R.id.customerName);
        mCustomerNumber = (TextView) findViewById(R.id.customerNumber);
        mCustomerDestination = (TextView) findViewById(R.id.customerDestination);
        mSettings = (Button) findViewById(R.id.settings);
        mDriveStatus = (Button) findViewById(R.id.status);
        mHistory = (Button) findViewById(R.id.history);
        mswitch = (Switch) findViewById(R.id.Wswitch);
        mswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    connectDriver();
                    mswitch1 = true;
                } else {
                    disconnectDriver();
                }
            }
        });

        pickupLatLng = new LatLng(0.0, 0.0);


        mDriveStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (status) {
                    case 1:
                        status = 2;
                        erasePolyLines();
                        if (destinationLatLng.latitude != 0.0 && destinationLatLng.longitude != 0.0) {
                            getRouteToMarker(destinationLatLng);
                        }
                        mDriveStatus.setText("Drive is Complete");
                        break;

                    case 2:
                        recordDrive();
                        endDrive();
                        break;
                }
            }
        });

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DriverMapActivity.this, DriverSettings.class);
                startActivity(intent);
                finish();
                return;
            }
        });


        mLogout = (Button) findViewById(R.id.logout);
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isLoggingOut = true;
                disconnectDriver();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(DriverMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DriverMapActivity.this, History.class);
                intent.putExtra("customersOrDriver", "Drivers");
                startActivity(intent);
                return;
            }
        });


        //showAllCustomers();
        getTheCustomer();
        getTheEvents();

    }

    private void showAllCustomers() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("CustomerAvailable");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {

                    Map<String, Object> mcustomers = (Map<String, Object>) dataSnapshot.getValue();  // i think it´s necessary to use array
                    for (int i = 0; i < mcustomers.size(); i++) {
                        double locationLat = 0;
                        double locationLng = 0;
                        if (mcustomers.get(0) != null) {
                            locationLat = Double.parseDouble(mcustomers.get(0).toString());

                        }
                        if (mcustomers.get(1) != null) {
                            locationLng = Double.parseDouble(mcustomers.get(1).toString());
                        }

                        userLatLng = new LatLng(locationLat, locationLng);
                        userMarker = mMap.addMarker(new MarkerOptions().position(userLatLng).title("User").icon(BitmapDescriptorFactory.fromResource(R.mipmap.usermarker)));

                    }
                } else {

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getTheEvents() {
        queue = Volley.newRequestQueue(getApplicationContext());
        markers = new ArrayList<>();
        String url = "https://app.ticketmaster.com/discovery/v2/events.json?countryCode=IE&size=133&startDateTime=2018-04-17T17:52:00Z&endDateTime=2018-04-24T17:53:00Z&apikey=mzOuM4tYy3IrWOM3sOHsGaABAsHWNCo3";
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url, (JSONObject) null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                /// Parse JSON
                try {
                    JSONObject embedded = response.getJSONObject("_embedded");
                    JSONArray eventsRequested = embedded.getJSONArray("events");
                    int length = eventsRequested.length();
                    for (int index = 0; index < length; index++) {
                        JSONObject Tevent = eventsRequested.getJSONObject(index);
                        String eventName = Tevent.getString("name");
                        System.out.println("eventName:" + eventName);
                        JSONObject dates = Tevent.getJSONObject("dates");
                        JSONObject start = dates.getJSONObject("start");
                        String startDateTime = start.getString("dateTime");
                        JSONObject venueEvent = Tevent.getJSONObject("_embedded");
                        JSONArray venues = venueEvent.getJSONArray("venues");


                        int venuesLength = venues.length();
                        for (int venuesIndex = 0; venuesIndex < venuesLength; venuesIndex++) {

                            if (venues.getJSONObject(venuesIndex).has("location")) {
                                JSONObject venue = venues.getJSONObject(venuesIndex);
                                JSONObject venueLocation = venue.getJSONObject("location");
                                double venueLng = Double.parseDouble(venueLocation.get("longitude").toString());
                                double venueLat = Double.parseDouble(venueLocation.get("latitude").toString());
                                LatLng venueCoordinates = new LatLng(venueLat, venueLng);
                                Marker marker = mMap.addMarker(new MarkerOptions().position(venueCoordinates).title(eventName).snippet(startDateTime).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_map)));
                                markers.add(marker);

                                //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(venueCoordinates, 10));
                                //list.add(eventName + " (start at " + startDateTime + ")");
                            } else {
                                System.out.println("no location");
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(jsObjRequest);
    }


    private void getTheCustomer() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest").child("customerRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    status = 1;
                    customerId = dataSnapshot.getValue().toString();
                    getTheCustomerPickupLocation();
                    getTheCustomerDestination();
                    getTheCustomerInfo();
                } else//called every time customer is removed(cancels)
                {
                    endDrive();

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });


    }

    private void getTheCustomerDestination() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest");
        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("destination") != null) {
                        destination = map.get("destination").toString();
                        mCustomerDestination.setText("Going To: " + destination);
                    }
                    //if there is no destination set
                    else {
                        mCustomerDestination.setText("Destination: ...");

                    }

                    Double destinationLat = 0.0;
                    Double destinationLng = 0.0;
                    if (map.get("destinationLat") != null) {
                        destinationLat = Double.valueOf(map.get("destinationLat").toString());

                    }
                    if (map.get("destinationLng") != null) {
                        destinationLng = Double.valueOf(map.get("destinationLng").toString());
                        destinationLatLng = new LatLng(destinationLat, destinationLng);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });


    }

    private void getTheCustomerInfo() {
        mCustomerInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null) {
                        mCustomerName.setText(map.get("name").toString());
                    }

                    if (map.get("number") != null) {
                        mCustomerNumber.setText(map.get("number").toString());
                    }

                    //use Glide to set the image
                    if (map.get("profileImageUrl") != null) {
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mCustomerProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void endDrive() {
        mDriveStatus.setText("Got the Customer");
        erasePolyLines();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("customerRequest");
        driverRef.removeValue();


        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(customerId);
        customerId = "";
        drivedistance = 0;

        //remove drivers marker
        if (pickupMarker != null) {
            pickupMarker.remove();
        }
        customerId = "";
        if (pickupMarker != null) {
            pickupMarker.remove();
        }
        if (customerPickupLocationRefListener != null) {
            customerPickupLocationRef.removeEventListener(customerPickupLocationRefListener);
        }
        mCustomerInfo.setVisibility(View.GONE);
        mCustomerName.setText("");
        mCustomerNumber.setText("");
        mCustomerDestination.setText("Going to: not specified");
        mCustomerProfileImage.setImageResource(R.drawable.userdefault);
    }

    //method to allow us to record the history of the drives
    public void recordDrive() {

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("history");
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("history");
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("history");
        //get a unique key for the database
        String requestId = historyRef.push().getKey();
        driverRef.child(requestId).setValue(true);
        customerRef.child(requestId).setValue(true);


        //populate historyRef with a info
        HashMap historyMap = new HashMap();
        historyMap.put("driver", userId);
        historyMap.put("customer", customerId);
        historyMap.put("rating", 0);
        historyMap.put("time", getCurrentTime());
        historyMap.put("destination", destination);
        historyMap.put("location/from/lat", pickupLatLng.latitude);
        historyMap.put("location/from/lng", pickupLatLng.longitude);
        historyMap.put("location/to/lat", destinationLatLng.latitude);
        historyMap.put("location/to/lng", destinationLatLng.longitude);
        historyMap.put("distance", drivedistance);
        historyRef.child(requestId).updateChildren(historyMap);

    }

    //get the timestamp of the drive
    private Long getCurrentTime() {
        Long timestamp = System.currentTimeMillis() / 1000;
        return timestamp;
    }


    Marker pickupMarker;
    private DatabaseReference customerPickupLocationRef;
    private ValueEventListener customerPickupLocationRefListener;

    private void getTheCustomerPickupLocation() {
        //listener that will be listing for "customerRequest" location
        customerPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerId).child("l");
        customerPickupLocationRefListener = customerPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !customerId.equals("")) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());

                    }
                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    //add marker to map to show where driver is
                    pickupLatLng = new LatLng(locationLat, locationLng);

                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup Location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_user)));
                    getRouteToMarker(pickupLatLng);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //taken from GitHub to get the route between driver and pickup location
    private void getRouteToMarker(LatLng pickupLatLng) {
        if (pickupLatLng != null && mLastLocation != null) {
            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(false)
                    .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), pickupLatLng)
                    .build();
            routing.execute();
        }
    }




    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        mLocationRequest = LocationRequest.create();
        //update every 1000 milliseconds
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        //set accuracy to be somewhat exact
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            } else {
                checkLocationPermission();
            }
        }

    }






    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            for (Location location : locationResult.getLocations()) {

                if (!customerId.equals("") && mLastLocation != null && location != null) {
                    drivedistance += mLastLocation.distanceTo(location) / 1000;
                }


                mLastLocation = location;

                //LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                //mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
                        .zoom(13)                   // Sets the zoom
                        .bearing(90)                // Sets the orientation of the camera to east
                        .tilt(40)// Sets the tilt of the camera to 30 degrees
                        .build();                   // Creates a CameraPosition from the builder
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                mMap.getUiSettings().setAllGesturesEnabled(true);
                mMap.getUiSettings().setZoomGesturesEnabled(true);


                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("DriverAvailable");
                DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("DriverWorking");
                GeoFire geoFireAvailable = new GeoFire(refAvailable);
                GeoFire geoFireWorking = new GeoFire(refWorking);


                switch (customerId) {
                    case "":
                        geoFireWorking.removeLocation(userId);
                        geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                        break;
                    default:
                        geoFireAvailable.removeLocation(userId);
                        geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));

                        break;

                }
            }

        }


    };


    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Must give permissions")
                        .setMessage("Must give permission")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create().show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);

                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }


    private void connectDriver(){
        checkLocationPermission();
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);

    }


    private void disconnectDriver(){
        if(mFusedLocationClient != null){
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("DriverAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);
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
