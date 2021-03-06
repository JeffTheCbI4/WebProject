package com.example.myapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapp.web.WebActivity;

public class MainActivity extends AppCompatActivity {

    Boolean stbCode;
    Button controlButton;
    Button archiveButton;
    Button webButton;
    DatabaseHelper databaseHelper;
    ForCreateDB createDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        controlButton = (Button) findViewById(R.id.control);
        archiveButton = (Button) findViewById(R.id.archive);
        webButton = (Button) findViewById(R.id.web);

        createDB = new ForCreateDB(getApplicationContext());
        databaseHelper = new DatabaseHelper(getApplicationContext());
    }

    public void control(View view){
            Intent intent = new Intent(getApplicationContext(), STBActivity.class);
            stbCode = true;
            intent.putExtra("code", stbCode);
            startActivity(intent);
    }

    public void archive(View view){
        Intent intent = new Intent(this, STBActivity.class);
        stbCode = false;
        intent.putExtra("code", stbCode);
        startActivity(intent);
    }

    public void web(View view){
        Intent intent = new Intent(this, WebActivity.class);
        stbCode = false;
        startActivity(intent);
    }


    @Override
    public void onDestroy(){
        super.onDestroy();
    }
}
