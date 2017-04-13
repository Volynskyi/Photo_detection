package zhdan.photo_detection;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.SparseIntArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import java.util.ArrayList;

public class GridLayoutDemoActivity extends AppCompatActivity {
    private DBHelper dbHelper;
    private SQLiteDatabase database;
    private ImageAdapter imageAdapter;
    public static final String POSITION = "position";
    private ArrayList<PhotoData> photoDataList = new ArrayList<>();
    private ActionMode actionMode;
    private GridView gridView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid_layout_demo);
        dbHelper = new DBHelper(getApplicationContext());
        gridView = (GridView) findViewById(R.id.gridView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_grid_layout_demo, menu);
        MenuItem itemSearch = menu.findItem(R.id.item_search1);
        SearchView searchView = (SearchView) itemSearch.getActionView();
        searchView.setIconified(false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                photoDataList.clear();

                database = dbHelper.getReadableDatabase();

                String selection = "name = ?";
                String[] selectionArgs = new String[]{query};
                Cursor cursor = database.query(DBHelper.TABLE_NAME, null, selection, selectionArgs, null, null,
                        null);

                if (cursor.moveToFirst()) {
                    int pathIndex = cursor.getColumnIndex(DBHelper.KEY_PATH);
                    int idIndex = cursor.getColumnIndex(DBHelper.KEY_ID);

                    String path;
                    int id;
                    do {
                        path = cursor.getString(pathIndex);
                        id = cursor.getInt(idIndex);
                        PhotoData photoData = new PhotoData(path, id, false, false);
                        photoDataList.add(photoData);
                    } while (cursor.moveToNext());
                } else {
                    Toast.makeText(getApplicationContext(), "No photos found", Toast.LENGTH_SHORT).show();
                    cursor.close();
                    return false;
                }
                imageAdapter = new ImageAdapter(getApplicationContext(), photoDataList);

                // устанавливаем адаптер через экземпляр класса ImageAdapter
                gridView.setAdapter(imageAdapter);
                gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        if (actionMode == null) {
                            actionMode = startActionMode(callback);

                            for (int i = 0; i != photoDataList.size(); i++) {
                                photoDataList.get(i).setVisible(true);
                            }
                            photoDataList.get(position).setSelected(true);
                            imageAdapter.notifyDataSetChanged();
                        } else
                            actionMode.finish();
                        return true;
                    }
                });

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

    private ActionMode.Callback callback = new ActionMode.Callback() {

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_delete_grid_item, menu);
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.delete:
                    // Calls getSelectedIds method from ListViewAdapter Class
                    SparseIntArray selected = imageAdapter
                            .getSelectedIds();
                    // Captures all selected ids with a loop

                    int key;
                    for (int i = (selected.size() - 1); i >= 0; i--) {
                        key = selected.keyAt(i);
                        imageAdapter.removeFromDB(key);
                        // Remove selected items following the ids
                        photoDataList.remove(selected.get(key));
                    }

                    // Close CAB
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        public void onDestroyActionMode(ActionMode mode) {
            imageAdapter.removeSelection();
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (database != null) {
            database.close();
        }
    }
}
