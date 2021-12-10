package com.picel.nubijaroute;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Result extends AppCompatActivity {
    TextView result_textView, resultShow2;
    Button btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result);

        result_textView = (TextView)findViewById(R.id.resultShow);
        resultShow2 = (TextView)findViewById(R.id.resultShow2);
        btn = findViewById(R.id.warp);

        Intent receive_intent = getIntent();

        String temp = "현재 위치에서 가장 가까운 정류장은 ";
        String temp2 = receive_intent.getStringExtra("key01") + "입니다.";

        result_textView.setText(temp);
        resultShow2.setText(temp2);
        double destLat = receive_intent.getDoubleExtra("destLat", 0);
        double destLong = receive_intent.getDoubleExtra("destLong", 0);
        double curLat = receive_intent.getDoubleExtra("curLat", 0);
        double curLong = receive_intent.getDoubleExtra("curLong", 0);

        String url = "kakaomap://route?sp=" + curLat + "," + curLong + "&ep=" + destLat + "," + destLong + "&by=FOOT";

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(myIntent);
            }
        });
    }

}
