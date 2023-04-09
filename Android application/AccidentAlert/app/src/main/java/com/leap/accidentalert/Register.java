package com.leap.accidentalert;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class Register extends AppCompatActivity {

    Button b1, b2, b3;
    EditText e1;
    ListView listView;
    SQLiteOpenHelper s1;
    SQLiteDatabase sqLitedb;
    DatabaseHandler myDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        e1 = findViewById(R.id.phone);
        b1 = findViewById(R.id.add);
        listView = findViewById(R.id.list);

        myDB = new DatabaseHandler(this);


        b1.setOnClickListener(v -> {
            String sr = e1.getText().toString();
            if(sr.isEmpty()) {
                Toast.makeText(this, "Please enter a number", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(Register.this, myDB.addData(sr)?"Data Added..":"Unsuccessful", Toast.LENGTH_SHORT).show();
            e1.setText("");
            loadData();
        });


        loadData();

    }



    private void loadData() {
        TextView emptyView =findViewById(R.id.emptyView);
        ArrayList<String> theList = new ArrayList<>();
        Cursor data = myDB.getListContents();
        if (data.getCount() == 0) {
            listView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }
        else {
            while (data.moveToNext())   theList.add(data.getString(1));
            listView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 0,theList) {
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    View listItem = convertView;
                    if(listItem == null)
                        listItem = LayoutInflater.from(Register.this).inflate(R.layout.listitem,parent,false);


                    TextView name = (TextView) listItem.findViewById(R.id.item_name);
                    name.setText(theList.get(position));

                   listItem.findViewById(R.id.delete_button).setOnClickListener(v->{
                       sqLitedb = myDB.getWritableDatabase();
                       DeleteData(theList.get(position));
                       Toast.makeText(Register.this, "Data Deleted", Toast.LENGTH_SHORT).show();
                       loadData();
                   });
                    return listItem;
                }
            };

            listView.setAdapter(adapter);

        }
    }

    private boolean DeleteData(String x) {
        return sqLitedb.delete(DatabaseHandler.TABLE_NAME, DatabaseHandler.COL2 + "=?", new String[]{x}) > 0;
    }


}