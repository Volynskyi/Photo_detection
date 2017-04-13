package zhdan.photo_detection;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import java.util.ArrayList;


class ImageAdapter extends BaseAdapter {

    private Context context;
    private SparseIntArray checkedItems;
    private DBHelper dbHelper;

    static ArrayList<PhotoData> photoDataList;

    ImageAdapter(Context c, ArrayList<PhotoData> photoDataList) {
        Log.d(MainActivity.TAG, "New object");
        checkedItems = new SparseIntArray();
        dbHelper = new DBHelper(c);
        context = c;
        this.photoDataList = photoDataList;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return photoDataList.size(); // длина массива
    }

    @Override
    public PhotoData getItem(int position) {
        // TODO Auto-generated method stub
        return photoDataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    private static class ViewHolder {
        ImageView imgView;
        CheckBox checkBox;
        int id;
        boolean visible;
        boolean selected;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(context);
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.grid_item_layout, null);
            viewHolder.imgView = (ImageView) convertView.findViewById(R.id.itemImageView);
            viewHolder.checkBox = (CheckBox) convertView.findViewById(R.id.itemCheckBox);
            Log.d(MainActivity.TAG, "photoDataList.get(position).isVisible() = " + photoDataList.get(position).isVisible() + position);
            if (photoDataList.get(position).isVisible()) {
                viewHolder.checkBox.setVisibility(View.VISIBLE);
            } else {
                viewHolder.checkBox.setVisibility(View.GONE);
            }
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final PhotoData photoData = getItem(position);
        viewHolder.imgView.setImageBitmap(BitmapFactory.decodeFile(photoData.getPhotoPath()));
        viewHolder.id = photoData.getId();
        viewHolder.visible = photoData.isVisible();
        viewHolder.selected = photoData.isSelected();

        if (!viewHolder.visible)
            viewHolder.checkBox.setVisibility(View.GONE);
        else {
            viewHolder.checkBox.setVisibility(View.VISIBLE);
            if (viewHolder.selected) {
                viewHolder.checkBox.setChecked(true);
                Log.d(MainActivity.TAG, "OnLongClick in position = " + position);
                checkedItems.put(photoData.getId(), position);
            }

            viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    if (buttonView.isChecked()) {
                        Log.d(MainActivity.TAG, "Checked in position = " + position);
                        checkedItems.put(photoData.getId(), position);
                    } else {
                        Log.d(MainActivity.TAG, "Unchecked in position = " + position);
                        checkedItems.delete(photoData.getId());
                    }
                }
            });
        }
        return convertView;
    }

    SparseIntArray getSelectedIds() {
        return checkedItems;
    }

    void removeFromDB(int id) {

        SQLiteDatabase database = dbHelper.getWritableDatabase();
        int delCount = database.delete(DBHelper.TABLE_NAME, DBHelper.KEY_ID + "=" + id, null);
        Log.d(MainActivity.TAG, "deleted row counts = " + delCount);
        notifyDataSetChanged();
    }

    void removeSelection() {
        checkedItems = new SparseIntArray();
        notifyDataSetChanged();
    }
}
