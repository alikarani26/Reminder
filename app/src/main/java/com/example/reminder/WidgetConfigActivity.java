package com.example.reminder;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class WidgetConfigActivity extends Activity {

    int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    ListView listView;
    ArrayList<String> listNamesArray = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Önce iptal sonucunu hazırla (Kullanıcı vazgeçerse boş widget oluşmasın)
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_widget_config);

        listView = findViewById(R.id.config_list_view);

        // 2. Widget ID yakalama
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        // 3. SharedPreferences'dan mevcut listeleri çek
        SharedPreferences sp = getSharedPreferences("ReminderData", MODE_PRIVATE);
        int listCount = sp.getInt("list_count", 0);

        for (int i = 1; i <= listCount; i++) {
            String title = sp.getString("title_" + i, null);
            if (title != null) {
                listNamesArray.add(title);
            }
        }

        // 4. Listeyi ekrana bas ve renkleri beyaza mürle
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listNamesArray) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(Color.WHITE);
                return view;
            }
        };
        listView.setAdapter(adapter);

        if (listNamesArray.isEmpty()) {
            Toast.makeText(this, "Henüz bir liste oluşturmamışsın dosti!", Toast.LENGTH_LONG).show();
        }

        // 5. SEÇİM YAPILDIĞINDA (Kritik Bölge - Dinamik İndeksleme)
        listView.setOnItemClickListener((parent, view, position, id) -> {

            // Tekrar okuma yapıyoruz ki güncel veriye ulaşalım
            int currentCount = sp.getInt("list_count", 0);
            int realIndex = -1;
            int visibleCounter = 0;

            // Ekranda GÖRDÜĞÜMÜZ 'position'ın hafızadaki gerçek 'i' karşılığını bulma operasyonu
            for (int i = 1; i <= currentCount; i++) {
                if (sp.contains("title_" + i)) {
                    if (visibleCounter == position) {
                        realIndex = i; // Nokta atışı: Gerçek anahtarı yakaladık!
                        break;
                    }
                    visibleCounter++;
                }
            }

            if (realIndex != -1) {
                // Seçilen indeksi .commit() ile diske anında kazı
                sp.edit().putInt("widget_list_selection_" + appWidgetId, realIndex).commit();

                // Widget'ı derhal güncellemeye zorla
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
                ReminderWidget.updateAppWidget(this, appWidgetManager, appWidgetId);

                // Başarı sonucunu bildir ve sayfayı kapat
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            } else {
                Toast.makeText(this, "Hata: Liste eşleşemedi!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}