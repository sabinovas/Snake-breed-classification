package com.example.venomvision;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.example.venomvision.mainPageActivity.HistoryActivity;
import com.example.venomvision.mainPageActivity.SnakeDetection;

public class MainActivity extends AppCompatActivity {

    RelativeLayout contentView, pests;
    CardView snakeCard, historyCard;
    Button detectBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //detectBtn = (Button) findViewById(R.id.detectBtn);
        snakeCard = (CardView) findViewById(R.id.snakeCard);
        historyCard = (CardView) findViewById(R.id.historyCard);

        startActivties();



    }

    private void startActivties() {

        snakeCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SnakeDetection.class));
            }
        });

        historyCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, HistoryActivity.class));
            }
        });

    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}