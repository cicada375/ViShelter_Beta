package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.example.myapplication.R;

public class SettingsMenu extends AppCompatActivity {
    private static final String CHANNEL_ID = "MY_CHANNEL_ID"; // Unique ID for the notification channel
    private static final int NOTIFICATION_ID = 123; // Unique ID for the notification

    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.button2);
        button.setOnClickListener(view -> {
            Handler handler = new Handler();
            handler.postDelayed(this::showNotification, 5000); // виведення сповіщення через 5 секунд
        });
    }
    //TODO:
    private void showNotification() {
        // створення менеджеру сповіщень
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // створення каналу для сповіщення (required for Android Oreo and later)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "My Channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("\uD83D\uDEA8ПОВІТРЯНА ТРИВОГА❗️\uD83D\uDEA8")
                .setContentText("Найближче сховище за адресою: ")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        // вивести сповіщення
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
}
