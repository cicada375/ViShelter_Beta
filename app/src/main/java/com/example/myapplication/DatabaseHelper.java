package com.example.myapplication;

import static com.example.myapplication.MapsActivity.mMap;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;

import com.example.myapplication.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// клас для з'єднання з базою даних (не працює належним чином чомусь, під'єднується до бази даних і до таблиці, але проходячи курсором через рядки видає що таблиця має 0 рядків)
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "markersdb";

    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "Markers";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_LATITUDE = "lat";
    public static final String COLUMN_LONGITUDE = "lng";
    public static final String COLUMN_STYLEURL = "styleUrl";



    public DatabaseHelper(Context context, GoogleMap map) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        fillDatabase(context);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_NAME + " TEXT, " +
                COLUMN_DESCRIPTION + " TEXT, " +
                COLUMN_LATITUDE + " REAL, " +
                COLUMN_LONGITUDE + " REAL, " +
                COLUMN_STYLEURL + " TEXT);";
        db.execSQL(sql);
    }

    public void fillDatabase(Context context){
        // Копіюємо базу даних з ресурсів в папку додатку
        //МЕТОД ДЛЯ ДОДАННЯ БАЗИ ДАНИХ НА НОСІЙ, ДОДАТИ ПЕРЕВІРКУ ЧИ ІСНУЄ ФАЙЛ ПЕРЕД СТВОРЕННЯМ НОВОЇ,
        try {
            // Відкриваємо вхідний потік для бази даних у папці assets
            InputStream inputStream = context.getResources().openRawResource(R.raw.MarkersDB);
            // Визначаємо шлях для копіювання бази даних в папку додатку
            String outDir = "/data/user/0/com.example.myapplication/databases/markersdb.db";
            File dir = new File(outDir);
            if (!dir.exists()) {
                dir.mkdirs(); // Створюємо папку database, якщо вона не існує
            }
            String outFileName = context.getDatabasePath(DATABASE_NAME).getPath();
            // Створюємо вихідний потік для копіювання бази даних
            OutputStream outputStream = new FileOutputStream(outFileName);

            // Копіюємо дані з вхідного потоку в вихідний потік
            byte[] buffer = new byte[1024*4];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            // Закриваємо потоки
            outputStream.flush();
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
//        onCreate(db);
    }

    public void findNearestMarker(Location lastLocation, Context context) {
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();


        try (Cursor result = sqLiteDatabase.rawQuery("SELECT * FROM " + TABLE_NAME, null)) {
            if (result.getCount() != 0) {
                double minDistance = Double.MAX_VALUE;
                MarkerOptions nearestMarker = null;
                while (result.moveToNext()) {
                    String styleUrl=result.getString(result.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STYLEURL));
                    double lat = result.getDouble(result.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LATITUDE));
                    double lng = result.getDouble(result.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LONGITUDE));
                    int height = 100;
                    int width = 100;
                    Bitmap b = null;
                    Bitmap smallMarker;
                    float[] results = new float[1];
                    Location.distanceBetween(lastLocation.getLatitude(), lastLocation.getLongitude(), lat, lng, results);
                    double distance = results[0];
                    //треба зробити перебір елементів в середині циклу і надання найближчого маркера
                    if (distance < minDistance) {
                        minDistance = distance;
                        BitmapDrawable bitmapdraw = (BitmapDrawable)context.getResources().getDrawable(getIconForStyleUrl(styleUrl,context));
                        b = bitmapdraw.getBitmap();
                        smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
                        nearestMarker = new MarkerOptions()
                                .position(new LatLng(lat, lng))
                                .title(result.getString(result.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)))
                                .snippet(result.getString(result.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DESCRIPTION)))
                                .icon(BitmapDescriptorFactory.fromBitmap(smallMarker));

                    }
                }
                mMap.addMarker(nearestMarker);
            }

        }
    }


    public void findNearestMarkerByStyleUrls(Location mLastKnownLocation, Context context) {
        String[] styleUrls = {"1","2","3","4","5","6","7","8","9","10","11","12"};

        for (String styleUrl : styleUrls) {
            SQLiteDatabase db = this.getReadableDatabase();

            String[] projection = {
                    DatabaseHelper.COLUMN_NAME,
                    DatabaseHelper.COLUMN_DESCRIPTION,
                    DatabaseHelper.COLUMN_LATITUDE,
                    DatabaseHelper.COLUMN_LONGITUDE,
                    DatabaseHelper.COLUMN_STYLEURL
            };

            String selection = DatabaseHelper.COLUMN_STYLEURL + "=?";
            String[] selectionArgs = { styleUrl };

            String sortOrder = DatabaseHelper.COLUMN_NAME + " DESC";

            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
            );

            findNearestMarkerByUrl(cursor, mLastKnownLocation, context);
        }
    }

    private Marker findNearestMarkerByUrl(Cursor cursor, Location mLastKnownLocation, Context context) {
        double currentLat = mLastKnownLocation.getLatitude();
        double currentLng = mLastKnownLocation.getLongitude();
        double minDistance = Double.MAX_VALUE;
        Marker nearestMarker = null;


        while (cursor.moveToNext()) {
            String styleUrl = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STYLEURL));
            double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LATITUDE));
            double lng = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LONGITUDE));
            int height = 100;
            int width = 100;
            Bitmap b = null;
            Bitmap smallMarker;
            float[] results = new float[1];
            Location.distanceBetween(currentLat, currentLng, lat, lng, results);
            double distance = results[0];
            if (distance < minDistance) {
                minDistance = distance;
                BitmapDrawable bitmapdraw = (BitmapDrawable)context.getResources().getDrawable(getIconForStyleUrl(styleUrl,context));
                b = bitmapdraw.getBitmap();
                smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
                nearestMarker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(lat, lng))
                        .title(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)))
                        .snippet(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DESCRIPTION)))
                        .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)).alpha(0));


            }
        }
        nearestMarker.setAlpha(1);
        return nearestMarker;
    }

    private int getIconForStyleUrl(String styleUrl, Context context) { //провірити
        int iconNumber;
        try {
            iconNumber = Integer.parseInt(styleUrl);
        } catch (NumberFormatException e) {
            // Неможливо перетворити номер в ціле число
            return -1;
        }
        String iconName = "icon" + iconNumber+"style"+MapsActivity.style;
        return context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
    }



}

