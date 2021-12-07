package com.picel.nubijaroute;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    Button btnOpenSearch;
    public MainActivity() throws IOException {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fab = findViewById(R.id.fab_search);
        fab.setOnClickListener((v) -> {
            Intent intent = new Intent(MainActivity.this, Search.class);
            startActivity(intent);
        });

        ActivityResultLauncher<String[]> locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts
                                .RequestMultiplePermissions(), result -> {
                            Boolean fineLocationGranted = result.getOrDefault(
                                    Manifest.permission.ACCESS_FINE_LOCATION, false);
                            Boolean coarseLocationGranted = result.getOrDefault(
                                    Manifest.permission.ACCESS_COARSE_LOCATION, false);
                            if (fineLocationGranted != null && fineLocationGranted) {
                                // Precise location access granted.
                            } else if (coarseLocationGranted != null && coarseLocationGranted) {
                                // Only approximate location access granted.
                            } else {
                                // No location access granted.
                            }
                        }
                );
        locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });

        MapView mapView = new MapView(this);

        ViewGroup mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);

        mapView.setZoomLevel(4, true);

        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);

        LocationApi apiData = new LocationApi();
        ArrayList<location> dataArr = apiData.getData();

        ArrayList<MapPOIItem> markerArr = new ArrayList<MapPOIItem>();
        for (location data : dataArr) {
            MapPOIItem marker = new MapPOIItem();
            marker.setMapPoint(MapPoint.mapPointWithGeoCoord(data.getLatitude(), data.getLongitude()));
            marker.setItemName(data.getName());
            markerArr.add(marker);
        }
        mapView.addPOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));
    }
}

class location {
    String name;
    Double latitude;
    Double longitude;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    @Override
    public String toString() {
        return "location{" +
                "name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}

class LocationApi {
    String apiUrl = "https://api.odcloud.kr/api/15000545/v1/uddi:lgt2dy2p-wwh7-jrxr-o85n-fxxskri7gpjs_201912181628?page=1&perPage=283";
    String apiKey = "YspDtRxm%2B7MqAdaeOj1uyk9wXh6QHVgPcgWYYTcUXAZwr2X3wJdQB7ndtrjqCnadP7t6EwvctxZH7XA%2FKlhH9A%3D%3D";
    public ArrayList<location> getData() {
        //return data와 관련된 부분
        ArrayList<location> dataArr = new ArrayList<location>();

        //네트워킹 작업은 메인스레드에서 처리하면 안된다. 따로 스레드를 만들어 처리하자
        Thread t = new Thread() {
            @Override
            public void run() {
                try {

                    //url과 관련된 부분
                    String fullurl = apiUrl + "&returnType=XML&serviceKey=" + apiKey;
                    URL url = new URL(fullurl);
                    InputStream is = url.openStream();

                    //xmlParser 생성
                    XmlPullParserFactory xmlFactory = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = xmlFactory.newPullParser();
                    parser.setInput(is, "utf-8");

                    //xml과 관련된 변수들
                    boolean bName = false, bLat = false, bLong = false, bLoc = false;
                    String name = "", latitude = "", longitude = "", location = "";

                    //본격적으로 파싱
                    while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                        int type = parser.getEventType();
                        location data = new location();

                        //태그 확인
                        if (type == XmlPullParser.START_TAG) {
                            if (parser.getName().equals("col")) {
                                if (parser.getAttributeValue(0).equals("터미널명"))
                                    bName = true;
                                else if (parser.getAttributeValue(0).equals("LOCX"))
                                    bLat = true;
                                else if (parser.getAttributeValue(0).equals("LOCY"))
                                    bLong = true;
                                else if (parser.getAttributeValue(0).equals("주소"))
                                    bLoc = true;
                            }
                        }
                        //내용 확인
                        else if (type == XmlPullParser.TEXT) {
                            if (bLoc)  {
                                location = parser.getText();
                                bLoc = false;
                            }
                            if (bName) {
                                name = parser.getText();
                                bName = false;
                            } else if (bLat) {
                                latitude = parser.getText();
                                bLat = false;
                            } else if (bLong) {
                                longitude = parser.getText();
                                bLong = false;
                            }
                        }
                        //내용 다 읽었으면 데이터 추가
                        else if (type == XmlPullParser.END_TAG && parser.getName().equals("item")) {
                            if (!location.equals("미정") && !location.equals("군부대")){
                                data.setName(name);
                                data.setLatitude(Double.valueOf(latitude));
                                data.setLongitude(Double.valueOf(longitude));

                                dataArr.add(data);
                            }
                        }
                        type = parser.next();
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        };
        try {
            t.start();
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return dataArr;
    }
}
