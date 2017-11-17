package com.example.zhouying18.myptrdemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.textView1).setOnClickListener(this);
        findViewById(R.id.textView2).setOnClickListener(this);
        findViewById(R.id.textView3).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.textView1:
                startActivity(new Intent(this,LoadMoreActivity.class));
                break;
            case R.id.textView2:
                startActivity(new Intent(this,LoadMoreActivity2.class));
                break;
            case R.id.textView3:
                startActivity(new Intent(this,LoadMoreActivity3.class));
                break;
        }
    }
}
