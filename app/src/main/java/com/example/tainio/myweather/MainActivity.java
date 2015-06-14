package com.example.tainio.myweather;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Locale;


public class MainActivity extends ActionBarActivity {

    private String data;
    private LocationManager locationmanager = null;
    private String selectcity;
    TextView lmtv=null;
    TextView rtv=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Spinner spin = (Spinner) findViewById(R.id.spinner);
        Button button = (Button) findViewById(R.id.resultbutton);
        lmtv = (TextView) findViewById(R.id.lmtv);
        rtv = (TextView) findViewById(R.id.resultView);
        locationmanager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getApplicationContext(), selectcity,Toast.LENGTH_SHORT).show();
                //Android 4.0 이상 부터는 네트워크를 이용할 때 반드시 Thread 사용해야 함
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        data = getXmlData();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                rtv.setText(data);
                            }
                        });
                    }
                }).start();
            }
        });

        ArrayAdapter adapter = ArrayAdapter.createFromResource(this,
                R.array.city, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(adapter);

        spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectcity = parent.getItemAtPosition(position).toString();
                rtv.setText(selectcity);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Toast.makeText(parent.getContext(), "not selected",Toast.LENGTH_SHORT).show();
            }
        });  //spin setOnItemSelectedListener
    }//onCreate

    String getXmlData(){
        StringBuilder buffer=new StringBuilder();

        String queryUrl="http://api.openweathermap.org/data/2.5/weather?q="   //요청 URL
                + selectcity                        //spinner에서 select된 city
                +"&mode=xml";

        try {
            URL url= new URL(queryUrl); //문자열로 된 요청 url을 URL 객체로 생성.
            InputStream is= url.openStream();  //url위치로 입력스트림 연결

            XmlPullParserFactory factory= XmlPullParserFactory.newInstance();
            XmlPullParser xpp= factory.newPullParser();
            xpp.setInput( new InputStreamReader(is, "UTF-8") );  //inputstream 으로부터 xml 입력받기

            String tag;
            xpp.next();
            int eventType= xpp.getEventType();

            while( eventType != XmlPullParser.END_DOCUMENT ){
                switch( eventType ){
                    case XmlPullParser.START_DOCUMENT:
                        buffer.append("start Weather XML parsing...\n\n");
                        break;
                    case XmlPullParser.START_TAG:
                        tag= xpp.getName();    //테그 이름 얻어오기
                        switch (tag) {
                            case "city":
                                buffer.append("도시명 : ");
                                xpp.next();
                                buffer.append(selectcity);
                                buffer.append("\n");
                                break;
                            case "temperature":
                                buffer.append("절대온도 : ");
                                xpp.next();
                                buffer.append(xpp.getAttributeValue(null, "value"));
                                buffer.append("\n");
                                break;
                            case "humidity":
                                buffer.append("습도 :");
                                xpp.next();
                                buffer.append(xpp.getAttributeValue(null, "value"));
                                buffer.append("%");
                                buffer.append("\n");
                                break;
                            case "pressure":
                                buffer.append("기압 :");
                                xpp.next();
                                buffer.append(xpp.getAttributeValue(null, "value"));
                                buffer.append("\n");
                                break;
                            case "clouds":
                                buffer.append("구름 :");
                                xpp.next();
                                buffer.append(xpp.getAttributeValue(null, "name"));
                                buffer.append("\n");
                                break;
                        }
                        break;
                    case XmlPullParser.TEXT:
                        break;
                    case XmlPullParser.END_TAG:
                        tag= xpp.getName();    //테그 이름 얻어오기
                        if(tag.equals("item")) buffer.append("\n"); // 첫번째 검색결과종료..줄바꿈
                        break;
                }

                eventType= xpp.next();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        buffer.append("end Weather XML parsing...\n");

        return buffer.toString(); //StringBuffer 문자열 객체 반환
    } //getxmldata() end

    public void onPause(){
        super.onPause();
        locationmanager.removeUpdates(netListener);
    }

    public void onResume(){
        super.onResume();
        locationmanager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 10, netListener);
    }

    LocationListener netListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();

            String address = getAddress(lat,lng);
            lmtv.setText(address);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    /** 위도와 경도 기반으로 주소를 리턴하는 메서드*/
    public String getAddress(double lat, double lng){
        String address = null;

        //위치정보를 활용하기 위한 구글 API 객체
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        //주소 목록을 담기 위한 HashMap
        List<Address> list = null;

        try{
            list = geocoder.getFromLocation(lat, lng, 1);
        } catch(Exception e){
            e.printStackTrace();
        }

        if(list == null){
            Toast.makeText(getApplicationContext(), "주소 못찾음",Toast.LENGTH_SHORT).show();
            return null;
        }

        if(list.size() > 0){
            Address addr = list.get(0);
            address = addr.getCountryName() + " "
                    + addr.getPostalCode() + " "
                    + addr.getLocality() + " "
                    + addr.getThoroughfare() + " "
                    + addr.getFeatureName();
        }

        return address;
    }

}
