package com.example.keelinofarrell.taxiapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DriverSettings extends AppCompatActivity {

    private EditText mName, mNumber, mCar;
    private Button mConfirm, mBack;
    private FirebaseAuth mAuth;
    private DatabaseReference mDriverDatabase;
    private String userId;
    private String mName1, mNumber1, mCar1;
    private ImageView mProfileImage;
    private Uri resultUri;
    private String mProfileUrl;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_settings);

        mName = (EditText)findViewById(R.id.name);
        mNumber = (EditText)findViewById(R.id.number);

        mConfirm = (Button)findViewById(R.id.confirm);
        mBack = (Button)findViewById(R.id.back);

        mProfileImage = (ImageView)findViewById(R.id.profileImage);

        mCar = (EditText)findViewById(R.id.car);


        //get the current user and pass into a String reference
        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        //pass userId into database reference
        mDriverDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId);

        getUserInfo();

        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });

        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveUserInfo();
            }
        });

        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                return;
            }
        });


    }

    private void getUserInfo(){
        mDriverDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()  && dataSnapshot.getChildrenCount() > 0){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        mName1 = map.get("name").toString();
                        mName.setText(mName1);
                    }

                    if(map.get("number")!=null){
                        mNumber1 = map.get("number").toString();
                        mNumber.setText(mNumber1);
                    }

                    if(map.get("car")!=null){
                        mCar1 = map.get("car").toString();
                        mCar.setText(mCar1);
                    }

                    //use Glide to set the image
                    if(map.get("profileImageUrl")!=null){
                        mProfileUrl = map.get("profileImageUrl").toString();
                        Glide.with(getApplication()).load(mProfileUrl).into(mProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //saving the user info to the database
    private void saveUserInfo() {

        mName1 = mName.getText().toString();
        mNumber1 = mNumber.getText().toString();
        mCar1 = mCar.getText().toString();
        Map userInfo = new HashMap();
        userInfo.put("name", mName1);
        userInfo.put("number", mNumber1);
        userInfo.put("car", mCar1);
        mDriverDatabase.updateChildren(userInfo);

        if(resultUri != null){
            StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profile_images").child(userId);
            Bitmap bitmap = null;

            //Pass resultURI into a bitmap
            //gets image from uri location
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //compress image
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);
            //move image into an array, how you save images in Firebase
            byte[] data = baos.toByteArray();
            UploadTask uploadTask = filePath.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    finish();
                    return;
                }
            });

            //add onclicklisteners to see if upload was successful
            //add URL to the Firebase database
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //get download URL
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    Map newImage = new HashMap();
                    newImage.put("profileImageUrl", downloadUrl.toString());
                    mDriverDatabase.updateChildren(newImage);

                    finish();
                    return;
                }
            });
        }else {

            finish();
        }


    }


    //method for getting the profile image that is chosen
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == Activity.RESULT_OK){
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            //set image view to image we got from this activity result
            mProfileImage.setImageURI(resultUri);
        }
    }
}

