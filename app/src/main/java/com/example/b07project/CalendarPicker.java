package com.example.b07project;

import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public  class CalendarPicker  {
    public void openCalendar(AppCompatActivity o, EditText t){
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date of Birth").build();

        picker.show(o.getSupportFragmentManager(), "DOB_PICKER");
        picker.addOnPositiveButtonClickListener(selection->{
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String formatted = sdf.format(new Date(selection));
            t.setText(formatted);
        });
    }
}
