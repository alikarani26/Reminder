package com.example.reminder;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class EditListPage extends AppCompatActivity {

    private EditText titleInput;
    private LinearLayout itemsContainer;
    private int listIndex = -1; // Hangi rafı düzenlediğimizi tutacak (Pusu savar)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_list_page);

        titleInput = findViewById(R.id.main_title);
        itemsContainer = findViewById(R.id.items_container);

        // 1. MainActivity'den gelen verileri ve İNDEKSİ yakala
        String currentTitle = getIntent().getStringExtra("current_title");
        String currentItems = getIntent().getStringExtra("current_items");
        listIndex = getIntent().getIntExtra("list_index", -1); // Hangi numaralı raf?

        // 2. Başlığı ve maddeleri yerine mürle
        if (currentTitle != null) titleInput.setText(currentTitle);

        if (currentItems != null) {
            String[] splitItems = currentItems.split("##");
            for (String item : splitItems) {
                if (!item.isEmpty()) {
                    addNewItemWithData(item);
                }
            }
        }

        // 3. Tetikleyiciler
        findViewById(R.id.add_item).setOnClickListener(v -> addNewItemWithData(""));
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_delete).setOnClickListener(v -> {
            deleteListComletely();
        });

        findViewById(R.id.btn_save).setOnClickListener(v -> saveUpdates());
    }

    private void addNewItemWithData(String text) {
        View itemView = getLayoutInflater().inflate(R.layout.list_item, null);
        EditText etItem = itemView.findViewById(R.id.et_item);
        etItem.setText(text);
        itemsContainer.addView(itemView);
    }

    private void saveUpdates() {
        if (listIndex == -1) {
            Toast.makeText(this, "Hata: Raf numarası bulunamadı!", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < itemsContainer.getChildCount(); i++) {
            View v = itemsContainer.getChildAt(i);
            EditText et = v.findViewById(R.id.et_item);
            String val = et.getText().toString().trim();
            if (!val.isEmpty()) {
                sb.append(val).append("##");
            }
        }

        SharedPreferences sp = getSharedPreferences("ReminderData", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        // KRİTİK HAMLE: Sadece bu listeye ait olan anahtarları güncelle!
        // "last_title" bitti, artık "title_X" devri başladı.
        editor.putString("title_" + listIndex, titleInput.getText().toString());
        editor.putString("items_" + listIndex, sb.toString());

        editor.apply();

        finish();
    }

    private void deleteListComletely(){
        if(listIndex == -1){
            return;
        }
        SharedPreferences sp = getSharedPreferences("ReminderData", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove("title_" + listIndex);
        editor.remove("items_" + listIndex);

        String rawNames = sp.getString("ListName","");
        if(!rawNames.isEmpty()){
            String[] names = rawNames.split("##");
            StringBuilder newNames = new StringBuilder();

            for(int i = 0;i <names.length;i++){
                if(i!=listIndex){
                    newNames.append(names[i]).append("##");
                }
            }
            editor.putString("ListNames", newNames.toString());
        }

        editor.apply();
        finish();
    }
}