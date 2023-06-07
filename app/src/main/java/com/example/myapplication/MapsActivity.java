//головна activity з мапою в додатку
package com.example.myapplication;


import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.myapplication.databinding.ActivityMapsBinding;
import com.example.myapplication.directionhelpers.FetchURL;
import com.example.myapplication.directionhelpers.TaskLoadedCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.data.kml.KmlLayer;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, AdapterView.OnItemSelectedListener, TaskLoadedCallback, GoogleMap.OnMarkerClickListener {
    private final int ACCESS_LOCATION_REQUEST_CODE = 10001; //код який відповідає наданню запиту на надсилання геолокації в додатку
    static GoogleMap mMap;
    Context context;
    ImageButton settingsButton;
    public static int style = 1;
    Marker tmp;
    private Polyline currentPolyline;
    Location geolocation;
    DatabaseHelper databaseHelper;
    FusedLocationProviderClient fusedLocationProviderClient;
    // буфер для додання одного або 12ти найближчих маркерів
    //MarkerOptions[] markers= new MarkerOptions[12]; //використовувати в циклі foreach o->o.remove
    //можна замінити на mMap.clear() для витирання всієї карти враховуючи контури якщо будуть якісь додані
    KmlLayer layer;
    //    String[] styles={"Класична карта", "Silver", "Dark", "Night", "Aubergine"};
//    int[] images={R.drawable.icon1style1,R.drawable.icon1style2,R.drawable.icon1style3, R.drawable.icon1style4,R.drawable.icon1style5};//додати потім ще два стиля і додати в масив
    ArrayList<CustomItem> styleList, modeList;

    @Override
    public void onTaskDone(Object... values) {
        if (currentPolyline != null)
            currentPolyline.remove();
        currentPolyline = mMap.addPolyline((PolylineOptions) values[0]);
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        if(tmp!=null) tmp.remove();
        // Creating a marker
        MarkerOptions markerOptions = new MarkerOptions();

        // Setting the position for the marker
        markerOptions.position(marker.getPosition());
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation();
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION_REQUEST_CODE);
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION_REQUEST_CODE);
                }
            }
        }
        final Location[] location = new Location[1];
        Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();
        locationTask.addOnCompleteListener(this, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                location[0] = task.getResult();
            } else {
                Log.e("TAG", "Failed to get location");
            }
        });
        // Setting the title for the marker.
        // This will be displayed on taping the marker

        new FetchURL(MapsActivity.this).execute(getUrl(new LatLng(geolocation.getLatitude(),geolocation.getLongitude()), marker.getPosition(), "driving"), "driving");
        // Animating to the touched position
        mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));

        // Placing a marker on the touched position
        tmp=mMap.addMarker(markerOptions);
        return false;
    }

    enum modeSelected {
        OneClosestShelter,
        ClosestShelterForAllStyles,
        FullMap
    }

    Object Mode = modeSelected.OneClosestShelter; //режим який обирається з енуму після обрання пукнту з режимів роботи

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.example.myapplication.databinding.ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);


            settingsButton = findViewById(R.id.button3);
            settingsButton.setOnClickListener(v -> {
                Intent intent = new Intent(context, SettingsMenu.class);
                startActivity(intent);
            });

            //надання fusedLocation- останньої геолокації яка була зареєстрована в пристрої (метод googleMaps.getMyLocation - deprecated)
            /* Для серверної частини (якщо така буде в майбутньому задля обрахування найближчих сховищ та оновлення списку не на стороні клієнта)
            // В РОЗРОБЦІ
            // Executor executor = Executors.newSingleThreadExecutor();
            //                        executor.execute(() -> {
            //                            OkHttpClient client = new OkHttpClient();
            //                            Request request = new Request.Builder()
            //                                    .url("http://localhost:3000/nearestShelter?lng="+location.getLongitude()+"&lat="+location.getLatitude())
            //                                    .build();
            //                            try {
            //                                Response response= client.newCall(request).execute();
            //                                String jsonResponse = response.body().string();
            //                                System.out.println(jsonResponse);
            //                                JSONObject jsonObject = new JSONObject(jsonResponse);
            //                            } catch (IOException | JSONException e) {
            //                                throw new RuntimeException(e);
            //                            }
            //                            // обробляємо відповідь
            //                      });
             */


        context = this;
        databaseHelper = new DatabaseHelper(context,mMap);//оголошення змінної *ЯКА Б* мала допомагати з підключенням до бази даних та включенням інформації (полів) з неї
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        initModeList();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation();
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION_REQUEST_CODE);
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION_REQUEST_CODE);
                }
            }
            return;
        }
        Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();
        locationTask.addOnCompleteListener(this, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Location location = task.getResult();
                geolocation=task.getResult();
                //оброблення останньої наданої локації місцезнаходження користувача
                if (Mode == modeSelected.OneClosestShelter) {
                    databaseHelper.findNearestMarker(location,getApplicationContext());
                }
                if (Mode == modeSelected.ClosestShelterForAllStyles) {
                    databaseHelper.findNearestMarkerByStyleUrls(location,getApplicationContext());
                }
                if (Mode == modeSelected.FullMap) {

                }
            } else {
                Log.e("TAG", "Failed to get location");
            }
        });

        mMap.setOnMapClickListener(latLng -> {
            if(tmp!=null) tmp.remove();
            // Creating a marker
            MarkerOptions markerOptions = new MarkerOptions();

            // Setting the position for the marker
            markerOptions.position(latLng);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    enableUserLocation();
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION_REQUEST_CODE);
                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION_REQUEST_CODE);
                    }
                }
                return;
            }
            final Location[] location = new Location[1];
            Task<Location> locTask = fusedLocationProviderClient.getLastLocation();
            locTask.addOnCompleteListener(this, task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            location[0] = task.getResult();
                        } else {
                            Log.e("TAG", "Failed to get location");
                        }
                    });
            // Setting the title for the marker.
            // This will be displayed on taping the marker
            markerOptions.title(latLng.latitude + " : " + latLng.longitude).alpha(0);

            new FetchURL(MapsActivity.this).execute(getUrl(new LatLng(geolocation.getLatitude(),geolocation.getLongitude()), latLng, "driving"), "driving");
            // Animating to the touched position
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

            // Placing a marker on the touched position
            tmp=googleMap.addMarker(markerOptions);
        });
        Spinner modeSpinner = findViewById(R.id.spinner2);
        CustomAdapter modesAdapter=new CustomAdapter(context,modeList);
        modeSpinner.setAdapter(modesAdapter);
        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                CustomItem clickedItem= (CustomItem) adapterView.getItemAtPosition(i);
                String clickedStyleName=clickedItem.getCustomName();
                Toast.makeText(MapsActivity.this,clickedStyleName+" обрано!", Toast.LENGTH_SHORT).show();
                switch (i) {
                    case 0:
                        if(layer!=null){
                            layer.removeLayerFromMap();
                        }
                        Mode=modeSelected.OneClosestShelter;
                        databaseHelper.findNearestMarker(geolocation,context);
                        break;
                    case 1:
                        if(layer!=null){
                            layer.removeLayerFromMap();
                        }
                        Mode=modeSelected.ClosestShelterForAllStyles;
                        databaseHelper.findNearestMarkerByStyleUrls(geolocation,context);
                        break;
                    case 2:
                        if(layer!=null){
                            layer.removeLayerFromMap();
                        }
                        Mode=modeSelected.FullMap;
                        loadLayer();
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                if(layer!=null){
                    layer.removeLayerFromMap();
                }
                Mode=modeSelected.FullMap;
            }
        });


        initStyleList();
        Spinner styleSpinner = findViewById(R.id.spinner);
        CustomAdapter adapter=new CustomAdapter(context, styleList);
        styleSpinner.setAdapter(adapter);
        styleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CustomItem clickedItem= (CustomItem) parent.getItemAtPosition(position);
                String clickedStyleName=clickedItem.getCustomName();
                Toast.makeText(MapsActivity.this,clickedStyleName+" обрано!", Toast.LENGTH_SHORT).show();
                switch (position) {
                        case 0:
                            style=1;
                            changeStyle(R.raw.custom_map_style,R.raw.markersstyle1);
                            break;
                        case 1:
                            style=2;
                            changeStyle(R.raw.custom_map_style2,R.raw.markersstyle2);
                            break;
                        case 2:
                            style=3;
                            changeStyle(R.raw.custom_map_style3,R.raw.markersstyle3);
                            break;
                        case 3:
                            style=4;
                            changeStyle(R.raw.custom_map_style4,R.raw.markersstyle4);
                            break;
                        case 4:
                            style=5;
                            changeStyle(R.raw.custom_map_style5,R.raw.markersstyle5);
                            break;
                    }
                }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                style=1;
                changeStyle(R.raw.custom_map_style,R.raw.markersstyle1);
            }
        });


        //наведення карти на Вінницьку область
        //мб більший зум зробити і ще давати координати користувача, замість статичних?
        googleMap.setMinZoomPreference(7.5f);
        LatLng Vinnitsia = new LatLng(49.102275, 28.675528);
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(Vinnitsia));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation();
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION_REQUEST_CODE);
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION_REQUEST_CODE);
                }
            }
            return;
        }
        googleMap.setMyLocationEnabled(true);
    }

    private void loadLayer() {
        switch (style) {
            case 1:
                try {
                    layer = new KmlLayer(mMap, R.raw.markersstyle1, getApplicationContext());
                    layer.addLayerToMap();
                } catch (XmlPullParserException | IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case 2:
                try {
                    layer = new KmlLayer(mMap, R.raw.markersstyle2, getApplicationContext());
                    layer.addLayerToMap();
                } catch (XmlPullParserException | IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case 3:
                try {
                    layer = new KmlLayer(mMap, R.raw.markersstyle3, getApplicationContext());
                    layer.addLayerToMap();
                } catch (XmlPullParserException | IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case 4:
                try {
                    layer = new KmlLayer(mMap, R.raw.markersstyle4, getApplicationContext());
                    layer.addLayerToMap();
                } catch (XmlPullParserException | IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case 5:
                try {
                    layer = new KmlLayer(mMap, R.raw.markersstyle5, getApplicationContext());
                    layer.addLayerToMap();
                } catch (XmlPullParserException | IOException e) {
                    throw new RuntimeException(e);
                }
                break;
        }
    }

    private String getUrl(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=ENTER_YOUR_API_KEY";
        return url;
    }

    private void initModeList(){
        modeList= new ArrayList<>();
        String[] modes={"Найближче сховище","Найближчі сховища кожного типу", "Повна мапа сховищ"};
        int[] images={R.drawable.map_marker,R.drawable.map_marker_multiple,R.drawable.map};
        for (int i = 0; i < modes.length; i++) {
            modeList.add(new CustomItem(modes[i],images[i]));
        }
    }
    private void initStyleList() {
        styleList=new ArrayList<>();
        String[] styles={"Класична карта", "Silver", "Aubergine", "Night","Dark"};
        int[] images={R.drawable.icon10style1,R.drawable.icon10style2,R.drawable.icon10style5, R.drawable.icon10style4,R.drawable.icon10style3};//додати потім ще два стиля і додати в масив
        for (int i = 0; i < styles.length; i++) {
            styleList.add(new CustomItem(styles[i],images[i]));
        }
    }







    @Override
    protected void onDestroy() {
//        databaseHelper.close();
        super.onDestroy();
    }

    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        //Перемістив nMap звідси наверх, не крашиться, але все потрібно перезапуск програми для того щоб все працювало...
        //...тому думаю проблема не в цій штуковині, но це треба ще провірити
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACCESS_LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                enableUserLocation();
            }
            else{
                //вивести повідомлення що користувач не зможе користуватись всіма функціями додатку не надавши доступу до місцезнаходження
                new AlertDialog.Builder(this)
                        .setTitle("Permission needed")
                        .setMessage("This permisiion is needed because you won't have all option enabled")
                        .setNegativeButton("ok", (dialog, which) -> dialog.dismiss())
                        .create().show();
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        //для зміни стилів
        //порядок: (порядок вкотре чомусь збився, хоча ролі особої не грає, хіба що змінити ім'я файлів)
        //1 звичайний вид мапи (контурний, обрано в додатку за замовчуванням)
        //2.Dark
        //3.Aubergine
        //4.Night
        //5.Silver

        switch (i) {
            case 0:
                style=1;
                changeStyle(R.raw.custom_map_style,R.raw.markersstyle1);
                break;
            case 1:
                style=2;
                changeStyle(R.raw.custom_map_style2,R.raw.markersstyle2);
                break;
            case 2:
                style=3;
                changeStyle(R.raw.custom_map_style3,R.raw.markersstyle3);
                break;
            case 3:
                style=4;
                changeStyle(R.raw.custom_map_style4,R.raw.markersstyle4);
                break;
            case 4:
                style=5;
                changeStyle(R.raw.custom_map_style5,R.raw.markersstyle5);
                break;
        }
    }

    public void changeStyle(int style, int kmz){
        if(layer!=null)
            layer.removeLayerFromMap();
        setMapStyle(MapStyleOptions.loadRawResourceStyle(context,style));
        if(Mode==modeSelected.FullMap) {
            try {
                layer = new KmlLayer(mMap, kmz, getApplicationContext());
                layer.addLayerToMap();
            } catch (XmlPullParserException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        style=1;
        changeStyle(R.raw.custom_map_style,R.raw.markersstyle1);
    }


    private void setMapStyle(MapStyleOptions style) {
        if (mMap != null) {
            mMap.setMapStyle(style);//застосування обаного стилю
        }
    }
}

