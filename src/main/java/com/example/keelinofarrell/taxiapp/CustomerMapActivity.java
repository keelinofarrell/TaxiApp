package com.example.keelinofarrell.taxiapp;

import android.*;
import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    SupportMapFragment mapFragment;
    private Button mLogout, mRequest, mSettings, mHistory;
    private LatLng pickupLocation;
    private boolean request = false;
    private Marker pickupMarker;
    private String destination, userId1;
    private FusedLocationProviderClient mFusedLocationClient;
    private LinearLayout mDriverInfo;
    private ImageView mDriverProfileImage;
    private TextView mDriverName, mDriverNumber, mDriverCar;
    private LatLng destinationLatLng;
    private RatingBar mRatingBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        mapFragment.getMapAsync(this);


        mDriverInfo = (LinearLayout) findViewById(R.id.driverInfo);
        mDriverProfileImage = (ImageView) findViewById(R.id.driverProfileImage);
        mDriverName = (TextView) findViewById(R.id.driverName);
        mDriverNumber = (TextView) findViewById(R.id.driverNumber);
        mDriverCar = (TextView) findViewById(R.id.driverCar);
        mSettings = (Button) findViewById(R.id.settings);
        mHistory = (Button) findViewById(R.id.history);
        mRatingBar = (RatingBar)findViewById(R.id.rating);

        destinationLatLng = new LatLng(0.0, 0.0);

        mLogout = (Button) findViewById(R.id.logout);
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mRequest = (Button) findViewById(R.id.request);
        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //to cancel the taxi
                //if they request to cancel then... if not find driver
                if (request) {
                    endDrive();
                } else {
                    request = true;
                    userId1 = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId1, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
                    pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Im Here!").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_user)));
                    mRequest.setText("Getting your driver...");

                    getClosestDriver();
                }
            }
        });

        mSettings = (Button) findViewById(R.id.settings);
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CustomerMapActivity.this, CustomerSettings.class);
                startActivity(intent);
                return;
            }
        });

        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CustomerMapActivity.this, History.class);
                intent.putExtra("customersOrDriver", "Customers");
                startActivity(intent);
                return;
            }
        });

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // Get info about the selected place.
                destination = place.getName().toString();
                destinationLatLng = place.getLatLng();
            }

            @Override
            public void onError(Status status) {
            }
        });


    }

    private int radius = 1;
    private boolean driverFound = false;
    private String driverFoundId;
    GeoQuery geoQuery;

    //get closest driver and create child that will tell driver who he must pick up
    private void getClosestDriver() {
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("DriverAvailable");
        GeoFire geoFire = new GeoFire(driverLocation);
        //creating query to search km around customers request
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            //anytime driver found within radius this is called, with key of driver and location
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && request) {
                    driverFound = true;
                    driverFoundId = key;

                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest");
                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap map = new HashMap();
                    map.put("customerRideId", customerId);
                    map.put("destination", destination);
                    map.put("destinationLat", destinationLatLng.latitude);
                    map.put("destinationLng", destinationLatLng.longitude);
                    driverRef.updateChildren(map);

                    //get driver location for the customer
                    getDriverLocation();
                    getDriverInfo();
                    getDriveEnded();
                    mRequest.setText("Looking for Driver Location");
                }


            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                //if driver isnt found radius goes up by 1 and function starts again
                if (!driverFound) {
                    radius++;
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void getDriverInfo() {
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {

                    if (dataSnapshot.child("name") != null) {
                        mDriverName.setText(dataSnapshot.child("name").getValue().toString());
                    }

                    if (dataSnapshot.child("number") != null) {
                        mDriverNumber.setText(dataSnapshot.child("number").getValue().toString());
                    }

                    if (dataSnapshot.child("car") != null) {
                        mDriverCar.setText(dataSnapshot.child("car").getValue().toString());
                    }


                    //use Glide to set the image
                    if (dataSnapshot.child("profileImageUrl") != null) {
                        Glide.with(getApplication()).load(dataSnapshot.child("profileImageUrl").getValue().toString()).into(mDriverProfileImage);
                    }

                    //get average rating for the driver
                    int ratingSum = 0;
                    float ratingTotal = 0;
                    float ratingAverage = 0;
                    for(DataSnapshot child : dataSnapshot.child("rating").getChildren()){
                        ratingSum = ratingSum + Integer.valueOf(child.getValue().toString());
                        ratingTotal ++;
                    }
                    if(ratingTotal != 0){
                        ratingAverage = ratingSum/ratingTotal;
                        mRatingBar.setRating(ratingAverage);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private DatabaseReference driveEndedRef;
    private ValueEventListener driveEndedRefListener;

    private void getDriveEnded() {
        driveEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest").child("customerRideId");
        driveEndedRefListener = driveEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

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

    private void endDrive() {
        request = false;
        geoQuery.removeAllListeners();
        driverLocationRef.removeEventListener(driverLocationRefListener);
        driveEndedRef.removeEventListener(driveEndedRefListener);

        //remove the customer id inside driver id
        if (driverFoundId != null) {
            DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest");
            driverRef.removeValue();
            driverFoundId = null;
        }
        //delete everything about the pickup
        driverFound = false;
        radius = 1;
        //erase the location
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);

        //remove drivers marker
        if (pickupMarker != null) {
            pickupMarker.remove();
        }

        mRequest.setText("Find A Driver");
        mDriverInfo.setVisibility(View.GONE);
        mDriverName.setText("");
        mDriverNumber.setText("");
        mDriverCar.setText("");
        mDriverProfileImage.setImageResource(R.drawable.userdefault);

    }

    private Marker mDriverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;

    private void getDriverLocation() {
        //create event listener after user found listens for changes in drivers working child in Firebase
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("DriverWorking").child(driverFoundId).child("l");
        driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            //every time location changes this is called
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && request) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLong = 0;
                    mRequest.setText("Driver Found");
                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());

                    }
                    if (map.get(1) != null) {
                        locationLong = Double.parseDouble(map.get(1).toString());
                    }
                    //add marker to map to show where driver is
                    LatLng driverLatLong = new LatLng(locationLat, locationLong);
                    if (mDriverMarker != null) {
                        mDriverMarker.remove();
                    }

                    //getting location of the customer
                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);


                    //getting the location of the driver
                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatLong.latitude);
                    loc2.setLongitude(driverLatLong.longitude);

                    //get distance between customer and driver
                    float distance = loc1.distanceTo(loc2);

                    if (distance < 100) {
                        mRequest.setText("Your Driver has Arrived");
                    } else {
                        mRequest.setText("We Found you a Driver!" + String.valueOf(distance));
                    }
                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLong).title("Your Driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_taxi)));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LocationRequest mLocationRequest = LocationRequest.create();
        //update every 1000 milliseconds
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        //set accuracy to be somewhat exact
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //check if permission has been granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            } else {
                checkPermission();
            }
        }else
            {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }


        }


    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {
                    mLastLocation = location;
                    System.out.println("Last location =" + location);
                    LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(13));

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerAvailable");

                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                }
            }
        }
    };


    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this).setTitle("must give permission").setMessage("must give permission")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        }).create().show();

            } else {
                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    //@Override
    public void onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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

    @Override
    protected void onStop(){
        super.onStop();
    }
}
