package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.myapplication.R;

import java.util.ArrayList;

//це адаптер для того аби виводити крім тексту картинку або картинку і текст
//використання: позначити вибір режимів роботи та стилів картинками
//ПОТРЕБУЄ ДОРОБКИ
//використовує xml файл spinner_item.xml
public class CustomAdapter extends ArrayAdapter<CustomItem> {
    Context context;
    String[] styles;
    int[] images;


    public CustomAdapter(Context context, ArrayList<CustomItem> itemArrayList) {
        super(context,0, itemArrayList);
    }
    @NonNull
    @Override
    public View getDropDownView(int positions, View convertView, @NonNull ViewGroup parent) {
        return initView(positions, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return initView(position, convertView, parent);
    }

    private View initView(int position,View convertView, ViewGroup parent){
        if(convertView==null){
            convertView=LayoutInflater.from(getContext()).inflate(R.layout.spinner_item,parent,false);
        }
        ImageView imageView=convertView.findViewById(R.id.imageView);
        TextView textView=convertView.findViewById(R.id.textView);
        CustomItem currentItem=getItem(position);
        if(currentItem!=null) {
            imageView.setImageResource(currentItem.getCustomNum());
            textView.setText(currentItem.getCustomName());
        }
        return convertView;
    }
}
