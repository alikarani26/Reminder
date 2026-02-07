package com.example.reminder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class NewListPage extends AppCompatActivity{

    EditText titleInput;
    Button saveButton, deleteButton, addItemButton, backButton;
    LinearLayout itemContainers;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_list_page);
        initViews();
        addItemButton.setOnClickListener(v -> {
            addNewItem();
        });

        saveButton.setOnClickListener(v -> {
            saveAll();
        });

        deleteButton.setOnClickListener(v -> {
            deleteAll();
        });

        backButton.setOnClickListener(v -> {
            goBack();
        });
    }

    private void initViews(){
        titleInput = findViewById(R.id.main_title);
        saveButton = findViewById(R.id.btn_save);
        deleteButton = findViewById(R.id.btn_delete);
        addItemButton = findViewById(R.id.add_item);
        itemContainers = findViewById(R.id.items_container);
        backButton = findViewById(R.id.btn_back);
    }

    private void addNewItem(){
        EditText newItem = new EditText(this);
        newItem.setHint("Yeni Madde");
        newItem.setTextColor(getResources().getColor(android.R.color.white));
        newItem.setHintTextColor(getResources().getColor(android.R.color.darker_gray));

        newItem.setBackgroundResource(R.drawable.white_border_shape);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 140);
        params.setMargins(0,15,0,15);
        newItem.setLayoutParams(params);
        newItem.setPadding(40,0,0,0);

        itemContainers.addView(newItem);
        newItem.requestFocus();
    }

    private void saveAll() {
        String title = titleInput.getText().toString().trim();
        StringBuilder sb = new StringBuilder();

        // 1. Maddeleri topla ve sonuna [0] ekle (KRİTİK DOKUNUŞ)
        for (int i = 0; i < itemContainers.getChildCount(); i++) {
            EditText et = (EditText) itemContainers.getChildAt(i);
            String text = et.getText().toString().trim();
            if (!text.isEmpty()) {
                // Widget'ın tik atabilmesi için sonuna [0] mühürlüyoruz
                sb.append(text).append("[0]##");
            }
        }

        if (title.isEmpty() || sb.length() == 0) {
            return;
        }

        android.content.SharedPreferences sp = getSharedPreferences("ReminderData", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = sp.edit();

        int currentCount = sp.getInt("list_count", 0);
        int newIndex = currentCount + 1;

        editor.putString("title_" + newIndex, title);
        editor.putString("items_" + newIndex, sb.toString());
        editor.putInt("list_count", newIndex);

        editor.apply();

        // Not: finish() zaten seni bir önceki sayfaya (MainActivity) atar.
        // Altındaki Intent ile tekrar MainActivity başlatmana gerek yok,
        // yoksa arka planda üst üste binmiş sayfalar oluşur.
        finish();
    }

    private void deleteAll(){
        titleInput.setText("");
        itemContainers.removeAllViews();
        addNewItem();
    }

    private void goBack(){
        Intent intent = new Intent(NewListPage.this, MainActivity.class);
        startActivity(intent);
    }
}
