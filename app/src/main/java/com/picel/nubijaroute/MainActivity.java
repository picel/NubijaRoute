package com.picel.nubijaroute;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
    String BASE_URL = "https://dapi.kakao.com/";
    String API_KEY = "7cfac32a296f5a75f19396b68f167a94";
    public MainActivity() throws IOException {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        GpsTracker gpsTracker = new GpsTracker(MainActivity.this);
        double currentlatitude = gpsTracker.getLatitude();
        double currentlongitude = gpsTracker.getLongitude();

        MapView mapView = new MapView(this);

        ViewGroup mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);

        mapView.setZoomLevel(4, true);

        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);

        LocationApi apiData = new LocationApi();
        ArrayList<location> dataArr = apiData.getData();

        double min = 1.0;
        double tmp1, tmp2, tmpsum, minLat = 0, minLong = 0;
        String minName = "";

        ArrayList<MapPOIItem> markerArr = new ArrayList<MapPOIItem>();
        for (location data : dataArr) {
            MapPOIItem marker = new MapPOIItem();
            double tmpLatitude = data.getLatitude();
            double tmpLongitude = data.getLongitude();
            String tmpname = data.getName();
            marker.setMapPoint(MapPoint.mapPointWithGeoCoord(tmpLatitude, tmpLongitude));
            tmp1 = tmpLatitude - currentlatitude;
            tmp2 = tmpLongitude - currentlongitude;
            tmpsum = Math.pow(tmp1, 2) + Math.pow(tmp2, 2);

            if (min > tmpsum){
                min = tmpsum;
                minName = tmpname;
                minLat = tmpLatitude;
                minLong = tmpLongitude;
            }

            marker.setItemName(tmpname);
            markerArr.add(marker);
        }
        mapView.addPOIItems(markerArr.toArray(new MapPOIItem[markerArr.size()]));

        FloatingActionButton fab = findViewById(R.id.fab_search);
        String finalMinName = minName;
        double finalMinLat = minLat;
        double finalMinLong = minLong;
        fab.setOnClickListener((v) -> {
            Intent intent = new Intent(MainActivity.this, Result.class);
            intent.putExtra("key01", finalMinName);
            intent.putExtra("destLat", finalMinLat);
            intent.putExtra("destLong", finalMinLong);
            intent.putExtra("curLat", currentlatitude);
            intent.putExtra("curLong", currentlongitude);
            startActivity(intent);
        });

        Toast toast = Toast.makeText(this.getApplicationContext(), "가장 가까운 정류장" + minName + "입니다.", Toast.LENGTH_LONG);
        toast.show();
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

class ResultSearchKeyword{

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

class GpsTracker extends Service implements LocationListener {
    private final Context mContext;
    Location location;
    double latitude;
    double longitude;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60;
    protected LocationManager locationManager;

    public GpsTracker(Context context) {
        this.mContext = context;
        getLocation();
    }

    public Location getLocation() {
        try {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (!isGPSEnabled && !isNetworkEnabled) {
            } else {
                int hasFineLocationPermission = ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION);
                int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION);
                if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED && hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
                } else return null;
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    if (locationManager != null) {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        if (locationManager != null) {
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.d("@@@", "" + e.toString());
        }
        return location;
    }

    public double getLatitude() {
        if (location != null) {
            latitude = location.getLatitude();
        }
        return latitude;
    }

    public double getLongitude() {
        if (location != null) {
            longitude = location.getLongitude();
        }
        return longitude;
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public void stopUsingGPS() {
        if (locationManager != null) {
            locationManager.removeUpdates(GpsTracker.this);
        }
    }
}

