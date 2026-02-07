package com.example.reminder;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import java.util.ArrayList;

public class WidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ReminderWidgetItemFactory(this.getApplicationContext(), intent);
    }
}

class ReminderWidgetItemFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context context;
    private int appWidgetId;
    private ArrayList<String> dataList = new ArrayList<>();

    public ReminderWidgetItemFactory(Context context, Intent intent) {
        this.context = context;
        this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {}

    @Override
    public void onDataSetChanged() {
        dataList.clear();
        SharedPreferences sp = context.getSharedPreferences("ReminderData", Context.MODE_PRIVATE);

        // Config'den gelen seçili listeyi alıyoruz
        int selectedIndex = sp.getInt("widget_list_selection_" + appWidgetId, -1);

        if (selectedIndex != -1) {
            String rawData = sp.getString("items_" + selectedIndex, "");
            if (!rawData.isEmpty()) {
                String[] items = rawData.split("##");
                for (String item : items) {
                    if (!item.isEmpty()) {
                        dataList.add(item);
                    }
                }
            }
        }
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position >= dataList.size()) return null;

        // BURADAKİ ID'LERİ XML DOSYANDAKİLERLE (widget_item.xml) EŞLEŞTİRİYORUZ
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_item);

        String rawItem = dataList.get(position); // Örn: "Ekmek[0]"
        boolean isChecked = rawItem.endsWith("[1]");

        // Temiz metin çıkarma
        String cleanText = rawItem;
        if (rawItem.contains("[")) {
            cleanText = rawItem.substring(0, rawItem.indexOf("["));
        }

        // 1. Metni yaz ve rengini BEYAZ yap (Görünmeme sorunu için garanti)
        // Eğer XML'de id farklıysa (örn: item_text), burayı ona göre güncelle
        views.setTextViewText(R.id.widget_text, cleanText);
        views.setTextColor(R.id.widget_text, Color.WHITE);

        // 2. Tik resmini ayarla
        if (isChecked) {
            views.setImageViewResource(R.id.widget_check, android.R.drawable.checkbox_on_background);
        } else {
            views.setImageViewResource(R.id.widget_check, android.R.drawable.checkbox_off_background);
        }

        // 3. Tıklama olayı (Tüm satıra tıklanabilir yapıyoruz)
        Intent fillInIntent = new Intent();
        fillInIntent.putExtra(ReminderWidget.EXTRA_ITEM_TEXT, rawItem);
        // XML'deki en dış kapsayıcı ID'si (widget_layout_parent)
        views.setOnClickFillInIntent(R.id.widget_layout_parent, fillInIntent);

        return views;
    }

    @Override
    public int getCount() { return dataList.size(); }
    @Override
    public long getItemId(int position) { return position; }
    @Override
    public RemoteViews getLoadingView() { return null; }
    @Override
    public int getViewTypeCount() { return 1; }
    @Override
    public boolean hasStableIds() { return true; }
    @Override
    public void onDestroy() { dataList.clear(); }
}