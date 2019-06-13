package com.dyd.publiclib;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.dyd.libsource.dropdownbox.MySpinner;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MySpinner spinner = (MySpinner) findViewById(R.id.spinner);
        String[] typeArrays = getResources().getStringArray(R.array.identify_types);
        spinner.setItems(typeArrays);
        spinner.setSelectedIndex(0);
        spinner.setTextColor(getResources().getColor(R.color.colorPrimary));
        spinner.setOnItemSelectedListener(new MySpinner.OnItemSelectedListener<String>() {

            @Override
            public void onItemSelected(MySpinner view, int position, long id, String item) {

            }
        });

        spinner.setOnNothingSelectedListener(new MySpinner.OnNothingSelectedListener() {

            @Override
            public void onNothingSelected(MySpinner spinner) {
                spinner.getSelectedIndex();
            }
        });
    }
}
