package com.example.keelinofarrell.taxiapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WelcomeActivity extends AppCompatActivity {

    LinearLayout l1, l2;
    TextView mName, mSlogan;
    Button mGo;
    Animation uptodown, downtoup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        mGo = (Button)findViewById(R.id.letgo);
        mName = (TextView)findViewById(R.id.name);
        mSlogan = (TextView)findViewById(R.id.slogan);
        l1 = (LinearLayout)findViewById(R.id.linear11);
        l2 = (LinearLayout)findViewById(R.id.linear12);

        uptodown = AnimationUtils.loadAnimation(this, R.anim.uptodown);

        l1.setAnimation(uptodown);
        l2.setAnimation(downtoup);

        mGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });
    }
}
