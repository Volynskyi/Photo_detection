package zhdan.photo_detection;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;

import java.util.ArrayList;


class ImageAdapter extends BaseAdapter {

    private SparseIntArray checkedItems;
    private DBHelper dbHelper;
    private ViewHolder viewHolder;

    ArrayList<PhotoData> photoDataList;

    ImageAdapter(){
    }

    ImageAdapter(Context c, ArrayList<PhotoData> photoDataList) {
        Log.d(MainActivity.TAG, "New object");
        checkedItems = new SparseIntArray();
        dbHelper = new DBHelper(c);
        this.photoDataList = photoDataList;
    }

    @Override
    public int getCount() {

        return photoDataList.size();
    }

    @Override
    public PhotoData getItem(int position) {
        return photoDataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(parent.getContext(), R.layout.grid_item_layout, null);
            viewHolder = new ViewHolder(convertView);
            Log.d(MainActivity.TAG, "photoDataList.get(position).isVisible() = " + photoDataList.get(position).isVisible() + position);
            if (photoDataList.get(position).isVisible()) {
                viewHolder.getCheckBox().setVisibility(View.VISIBLE);
            } else {
                viewHolder.getCheckBox().setVisibility(View.GONE);
            }
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final PhotoData photoData = getItem(position);

        Bitmap bitmap = decodeSampledBitmapFromPath(photoData.getPhotoPath(), 100, 100);
        Log.d(MainActivity.TAG, "" + bitmap.getByteCount());

        viewHolder.getImageView().setImageBitmap(bitmap);
        viewHolder.id = photoData.getId();
        viewHolder.visible = photoData.isVisible();
        viewHolder.selected = photoData.isSelected();

        if (!viewHolder.visible)
            viewHolder.getCheckBox().setVisibility(View.GONE);
        else {
            viewHolder.getCheckBox().setVisibility(View.VISIBLE);
            if (viewHolder.selected) {
                viewHolder.getCheckBox().setChecked(true);
                Log.d(MainActivity.TAG, "OnLongClick in position = " + position);
                checkedItems.put(photoData.getId(), position);
            }

            viewHolder.getCheckBox().setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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


    private static Bitmap decodeSampledBitmapFromPath(String path, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and
            // width
            final int heightRatio = Math.round((float) height
                    / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will
            // guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
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
