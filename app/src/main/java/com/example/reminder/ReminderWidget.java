package com.example.reminder;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import java.text.DateFormat;
import java.util.Date;

public class ReminderWidget extends AppWidgetProvider {

    public static final String ACTION_TOGGLE_ITEM = "com.example.reminder.ACTION_TOGGLE_ITEM";
    public static final String ACTION_NEXT_LIST = "com.example.reminder.ACTION_NEXT_LIST";
    public static final String EXTRA_ITEM_TEXT = "com.example.reminder.EXTRA_ITEM_TEXT";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // --- 1. GÜNLÜK SIFIRLAMA KONTROLÜ ---
        checkAndResetDailyTicks(context);

        SharedPreferences sp = context.getSharedPreferences("ReminderData", Context.MODE_PRIVATE);
        int selectedIndex = sp.getInt("widget_list_selection_" + appWidgetId, -1);

        String title;
        if (selectedIndex != -1) {
            title = sp.getString("title_" + selectedIndex, "Bulunamadı: title_" + selectedIndex);
        } else {
            title = "Seçim Kayıtlı Değil (-1)";
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        views.setTextViewText(R.id.widget_title, title);

        // --- 2. LİSTE SERVİSİ ---
        Intent intent = new Intent(context, WidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        views.setRemoteAdapter(R.id.widget_listview, intent);

        // --- 3. TIKLAMA ŞABLONU (Maddeler için) ---
        Intent clickIntent = new Intent(context, ReminderWidget.class);
        clickIntent.setAction(ACTION_TOGGLE_ITEM);
        clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent clickPI = PendingIntent.getBroadcast(context, appWidgetId, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        views.setPendingIntentTemplate(R.id.widget_listview, clickPI);

        // --- 4. BAŞLIK TIKLAMA (Sonraki Liste için) ---
        Intent nextIntent = new Intent(context, ReminderWidget.class);
        nextIntent.setAction(ACTION_NEXT_LIST);
        nextIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent nextPI = PendingIntent.getBroadcast(context, appWidgetId + 100, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        views.setOnClickPendingIntent(R.id.widget_title, nextPI);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    // --- SİNSİ GÜN DEĞİŞİMİ SAVAŞÇISI ---
    private static void checkAndResetDailyTicks(Context context) {
        SharedPreferences sp = context.getSharedPreferences("ReminderData", Context.MODE_PRIVATE);
        String lastResetDate = sp.getString("last_reset_date", "");
        String today = DateFormat.getDateInstance().format(new Date());

        if (!today.equals(lastResetDate)) {
            int count = sp.getInt("list_count", 0);
            SharedPreferences.Editor editor = sp.edit();
            for (int i = 1; i <= count; i++) {
                String items = sp.getString("items_" + i, "");
                if (!items.isEmpty()) {
                    // Widget'ın anladığı dilden: [1]'leri (tikli) [0] (tiksiz) yapıyoruz
                    editor.putString("items_" + i, items.replace("[1]", "[0]"));
                }
            }
            editor.putString("last_reset_date", today);
            editor.commit(); // commit() kullanarak anında diske mürle
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return;

        SharedPreferences sp = context.getSharedPreferences("ReminderData", Context.MODE_PRIVATE);

        if (ACTION_TOGGLE_ITEM.equals(intent.getAction())) {
            String rawItem = intent.getStringExtra(EXTRA_ITEM_TEXT);
            int listIndex = sp.getInt("widget_list_selection_" + appWidgetId, -1);

            if (listIndex != -1 && rawItem != null) {
                String key = "items_" + listIndex;
                String rawData = sp.getString(key, "");

                if (!rawData.isEmpty()) {
                    String newItem = rawItem.endsWith("[0]") ? rawItem.replace("[0]", "[1]") : rawItem.replace("[1]", "[0]");
                    String updatedData = rawData.replace(rawItem + "##", newItem + "##");

                    sp.edit().putString(key, updatedData).commit();
                    mgr.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_listview);
                }
            }
        }

        if (ACTION_NEXT_LIST.equals(intent.getAction())) {
            int listCount = sp.getInt("list_count", 0);
            if (listCount > 0) {
                int currentIndex = sp.getInt("widget_list_selection_" + appWidgetId, 1);
                // Listenin hafızada olup olmadığını kontrol eden döngü (opsiyonel ama sağlıklı)
                int nextIndex = (currentIndex % listCount) + 1;

                sp.edit().putInt("widget_list_selection_" + appWidgetId, nextIndex).commit();
                updateAppWidget(context, mgr, appWidgetId);
                mgr.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_listview);
            }
        }
    }
}