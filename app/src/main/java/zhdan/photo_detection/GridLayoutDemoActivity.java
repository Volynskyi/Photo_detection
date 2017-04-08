package zhdan.photo_detection;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import org.opencv.photo.Photo;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class GridLayoutDemoActivity extends AppCompatActivity {
    DBHelper dbHelper;
    SQLiteDatabase database;
    public static final String POSITION = "position";


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid_layout_demo);
        dbHelper = new DBHelper(getApplicationContext());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_grid_layout_demo, menu);
        MenuItem itemSearch = menu.findItem(R.id.item_search1);
        SearchView searchView = (SearchView) itemSearch.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query) {
                ArrayList<String> photosPath = new ArrayList<>();

                database = dbHelper.getReadableDatabase();

                String selection = "name = ?";
                String[] selectionArgs = new String[] { query };
                database = dbHelper.getReadableDatabase();
                Cursor cursor = database.query(DBHelper.TABLE_NAME, null, selection, selectionArgs, null, null,
                        null);

                if(cursor.moveToFirst()){
                    int pathIndex = cursor.getColumnIndex(DBHelper.KEY_PATH);
                    do{
                        photosPath.add(cursor.getString(pathIndex));
                    }while (cursor.moveToNext());
                }
                else{
                    Toast.makeText(getApplicationContext(), "No photos found", Toast.LENGTH_SHORT).show();
                    cursor.close();
                    return false;
                }

                GridView gridView = (GridView) findViewById(R.id.gridView);

                // устанавливаем адаптер через экземпляр класса ImageAdapter
                gridView.setAdapter(new ImageAdapter(getApplicationContext(), photosPath));

                gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        // посылаем идентификатор картинки в FullScreenActivity
                        Intent i = new Intent(getApplicationContext(),
                                FullImageActivity.class);
                        // передаем индекс массива
                        i.putExtra(POSITION, position);
                        startActivity(i);
                    }
                });

                cursor.close();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        database.close();
    }
}
