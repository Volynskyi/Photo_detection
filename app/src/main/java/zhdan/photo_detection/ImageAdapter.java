package zhdan.photo_detection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;


class ImageAdapter extends BaseAdapter {

    private SparseIntArray checkedItems;

    ImageAdapter() {
        checkedItems = new SparseIntArray();
    }

    @Override
    public int getCount() {
        return GridLayoutDemoActivity.photoDataList.size();
    }

    @Override
    public PhotoData getItem(int position) {
        return GridLayoutDemoActivity.photoDataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = View.inflate(parent.getContext(), R.layout.grid_item_layout, null);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final PhotoData photoData = getItem(position);
        viewHolder.getImageView().setImageBitmap(decodeSampledBitmapFromPath(photoData.getPhotoPath(), 100, 100));

        if (!photoData.isVisibleCheckBox())
            viewHolder.getCheckBox().setVisibility(View.GONE);
        else {
            viewHolder.getCheckBox().setVisibility(View.VISIBLE);
            if (photoData.isSelected()) {
                viewHolder.getCheckBox().setChecked(true);
                checkedItems.put(position, photoData.getId());
            }
            viewHolder.getCheckBox().setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (buttonView.isChecked()) {
                        checkedItems.put(position, photoData.getId());
                        photoData.setSelected(true);
                    } else {
                        checkedItems.delete(position);
                        photoData.setSelected(false);
                    }
                }
            });
        }
        return convertView;
    }

    private static Bitmap decodeSampledBitmapFromPath(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options,
                                             int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height
                    / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    SparseIntArray getSelectedIds() {
        return checkedItems;
    }

    void removeSelection() {
        checkedItems.clear();
    }
}
