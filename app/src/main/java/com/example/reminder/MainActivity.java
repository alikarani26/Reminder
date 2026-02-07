package com.example.reminder;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_new).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NewListPage.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndResetDailyTicks();
        loadAndShowBubbles();
    }

    private void checkAndResetDailyTicks() {
        SharedPreferences sp = getSharedPreferences("ReminderData", MODE_PRIVATE);
        String lastResetDate = sp.getString("last_reset_date", "");
        String today = DateFormat.getDateInstance().format(new Date());

        if (!today.equals(lastResetDate)) {
            int count = sp.getInt("list_count", 0);
            SharedPreferences.Editor editor = sp.edit();
            for (int i = 1; i <= count; i++) {
                String items = sp.getString("items_" + i, "");
                if (!items.isEmpty()) {
                    editor.putString("items_" + i, items.replace("[1]", "[0]"));
                }
            }
            editor.putString("last_reset_date", today);
            editor.apply();
        }
    }

    private void loadAndShowBubbles() {
        LinearLayout mainContainer = findViewById(R.id.main_container);
        mainContainer.removeAllViews();

        SharedPreferences sp = getSharedPreferences("ReminderData", MODE_PRIVATE);
        int listCount = sp.getInt("list_count", 0);

        for (int i = 1; i <= listCount; i++) {
            final int currentIndex = i;
            String title = sp.getString("title_" + i, null);
            String items = sp.getString("items_" + i, null);

            if (title != null && items != null) {
                View bubble = getLayoutInflater().inflate(R.layout.bubble_template, null);

                TextView txtTitle = bubble.findViewById(R.id.txt_title);
                CheckBox chk1 = bubble.findViewById(R.id.txt_madde1);
                CheckBox chk2 = bubble.findViewById(R.id.txt_madde2);

                txtTitle.setText(title);
                String[] splitItems = items.split("##");

                // Madde 1
                if (splitItems.length > 0) {
                    setupCheckBox(chk1, splitItems[0], currentIndex, 0);
                } else { chk1.setVisibility(View.GONE); }

                // Madde 2
                if (splitItems.length > 1) {
                    setupCheckBox(chk2, splitItems[1], currentIndex, 1);
                } else { chk2.setVisibility(View.GONE); }

                // BALON TIKLAMA LOJİĞİ (Düzenleme Sayfasına Uçuş)
                bubble.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, EditListPage.class);
                    intent.putExtra("current_title", title);
                    intent.putExtra("current_items", items);
                    intent.putExtra("list_index", currentIndex);
                    startActivity(intent);
                });

                mainContainer.addView(bubble);
            }
        }
    }

    private void setupCheckBox(CheckBox chk, String rawData, int listIdx, int itemIdx) {
        // Güvenlik kontrolü: Eğer veri [0] veya [1] ile bitmiyorsa pusuya düşme!
        boolean hasTag = rawData.endsWith("[0]") || rawData.endsWith("[1]");

        boolean isChecked = rawData.endsWith("[1]");
        String cleanText;

        if (hasTag) {
            cleanText = rawData.substring(0, rawData.length() - 3);
        } else {
            cleanText = rawData; // Eğer tag yoksa olduğu gibi yaz, patlama!
        }

        chk.setText(cleanText);
        chk.setOnCheckedChangeListener(null);
        chk.setChecked(isChecked);

        chk.setFocusable(false);
        chk.setFocusableInTouchMode(false);

        applyStrikeThrough(chk, isChecked);

        chk.setOnCheckedChangeListener((buttonView, checked) -> {
            applyStrikeThrough(chk, checked);
            saveTickStatus(listIdx, itemIdx, checked);
        });
    }

    private void applyStrikeThrough(CheckBox chk, boolean isChecked) {
        if (isChecked) {
            chk.setPaintFlags(chk.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            chk.setPaintFlags(chk.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
    }

    private void saveTickStatus(int listIdx, int itemIdx, boolean isChecked) {
        SharedPreferences sp = getSharedPreferences("ReminderData", MODE_PRIVATE);
        String items = sp.getString("items_" + listIdx, "");
        if (!items.isEmpty()) {
            String[] splitItems = items.split("##");
            if (itemIdx < splitItems.length) {
                String text = splitItems[itemIdx].substring(0, splitItems[itemIdx].length() - 3);
                splitItems[itemIdx] = text + (isChecked ? "[1]" : "[0]");

                StringBuilder sb = new StringBuilder();
                for (String s : splitItems) { sb.append(s).append("##"); }
                sp.edit().putString("items_" + listIdx, sb.toString()).apply();
                // MainActivity.java içindeki saveTickStatus metodunun sonuna ekle:

// Widget'ları uyandır: "Hafızadaki veriler değişti, kendini yenile!" emri gönderiyoruz.
                AppWidgetManager am = AppWidgetManager.getInstance(this);
                int[] ids = am.getAppWidgetIds(new android.content.ComponentName(this, ReminderWidget.class));
                am.notifyAppWidgetViewDataChanged(ids, R.id.widget_listview);

// Başlıkların da güncellenmesi gerekiyorsa (opsiyonel ama garanti olur):
                for (int id : ids) {
                    ReminderWidget.updateAppWidget(this, am, id);
                }
            }
        }
    }
}