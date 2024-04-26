package com.genesyst.lab.firstyolo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

public class ConfidListActivity extends AppCompatActivity {
    private ListView ConfidList;
    private Button CloseBtn;
    private ArrayList<String> ConfidenceSumm = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confid_list);

        Intent intent = getIntent();
        this.ConfidenceSumm = (ArrayList<String>) intent.getSerializableExtra("data");

        this.SetView();
    }

    private void SetView(){
        this.ConfidList = (ListView)findViewById(R.id.ConfidList);
        this.CloseBtn = (Button) findViewById(R.id.CloseBtn);

        this.SetAction();
    }

    private void SetAction(){
        ArrayAdapter adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                this.ConfidenceSumm);
        this.ConfidList.setAdapter(adapter);

        this.CloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}