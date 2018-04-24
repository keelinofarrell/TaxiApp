package com.example.keelinofarrell.taxiapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private Button mDriver, mCustomer;
    private TextView mPadding, mWhat;
    private ImageView mTaxi;
    LinearLayout mLayout;
    Animation lefttoright;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDriver = (Button)findViewById(R.id.driver);
        mCustomer = (Button)findViewById(R.id.customer);
        mPadding = (TextView)findViewById(R.id.padding);
        mWhat = (TextView)findViewById(R.id.what);
        mTaxi = (ImageView)findViewById(R.id.mytaxi);
        mLayout = (LinearLayout)findViewById(R.id.layoutslide);

        lefttoright = AnimationUtils.loadAnimation(this, R.anim.lefttoright);

        mLayout.setAnimation(lefttoright);


        startService(new Intent(MainActivity.this, ClosedMap.class));
        mDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,DriverLogin.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,CustomerLogin.class);
                startActivity(intent);
                finish();
                return;
            }
        });

    }
}
