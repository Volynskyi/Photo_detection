package zhdan.photo_detection;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import java.util.ArrayList;

public class GridLayoutDemoActivity extends AppCompatActivity {
    private final String CURRENT_QUERY = "currentQuery";
    private final String ACTION_MODE_WORKS = "actionModeWorks";
    private final String PHOTO_DATA_LIST = "photoDataList";

    public static final String POSITION = "position";

    private DBHelper dbHelper;
    private SQLiteDatabase database;
    private ImageAdapter imageAdapter;
    private ActionMode actionMode;
    private GridView gridView;
    private String currentQuery;
    private boolean actionModeWorks;

    static ArrayList<PhotoData> photoDataList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid_layout);
        dbHelper = new DBHelper(getApplicationContext());
        gridView = (GridView) findViewById(R.id.gridView);
        database = dbHelper.getWritableDatabase();
        if (savedInstanceState != null) {
            currentQuery = savedInstanceState.getString(CURRENT_QUERY);
            actionModeWorks = savedInstanceState.getBoolean(ACTION_MODE_WORKS);
            photoDataList = savedInstanceState.getParcelableArrayList(PHOTO_DATA_LIST);
            setAdapterAndListenersForGrid();
            if (actionModeWorks) {
                actionMode = startActionMode(callback);
            }
            imageAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_grid_layout_demo, menu);
        MenuItem itemSearch = menu.findItem(R.id.item_search1);
        SearchView searchView = (SearchView) itemSearch.getActionView();
        searchView.setIconified(false);
        if (!TextUtils.isEmpty(currentQuery)) {
            searchView.setQuery(currentQuery, true);
            searchView.clearFocus();
        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentQuery = query;
                getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                photoDataList = new ArrayList<>();
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
                        photoDataList.add(new PhotoData(path, id, false, false));
                    } while (cursor.moveToNext());
                    cursor.close();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.toast_no_photos_after_search, Toast.LENGTH_SHORT).show();
                    cursor.close();
                    return false;
                }
                setAdapterAndListenersForGrid();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return true;
    }

    private void setAdapterAndListenersForGrid() {
        imageAdapter = new ImageAdapter();
        gridView.setAdapter(imageAdapter);
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                actionMode = startActionMode(callback);
                for (int i = 0; i < photoDataList.size(); i++) {
                    photoDataList.get(i).setVisibleCheckBox(true);
                }
                photoDataList.get(position).setSelected(true);
                imageAdapter.notifyDataSetChanged();
                gridView.setAdapter(imageAdapter);
                return true;
            }
        });

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(getApplicationContext(),
                        FullImageActivity.class);
                i.putExtra(POSITION, position);
                startActivity(i);
            }
        });
    }

    private ActionMode.Callback callback = new ActionMode.Callback() {

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_delete_grid_item, menu);
            actionModeWorks = true;
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.delete:
                    SparseIntArray selected = imageAdapter.getSelectedIds();
                    int key;
                    ArrayList<PhotoData> selectedItems = new ArrayList<>();
                    for (int i = 0; i < selected.size(); i++) {
                        key = selected.keyAt(i);
                        dbHelper.removeFromDB(selected.get(key), database);
                        selectedItems.add(photoDataList.get(key));
                    }
                    photoDataList.removeAll(selectedItems);
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        public void onDestroyActionMode(ActionMode mode) {
            for (int i = 0; i < photoDataList.size(); i++) {
                photoDataList.get(i).setVisibleCheckBox(false);
            }
            imageAdapter.removeSelection();
            actionModeWorks = false;
            imageAdapter.notifyDataSetChanged();
            gridView.setAdapter(imageAdapter);
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentQuery != null) {
            outState.putString(CURRENT_QUERY, currentQuery);
            outState.putBoolean(ACTION_MODE_WORKS, actionModeWorks);
            outState.putParcelableArrayList(PHOTO_DATA_LIST, photoDataList);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (database != null) {
            database.close();
        }
    }
}
