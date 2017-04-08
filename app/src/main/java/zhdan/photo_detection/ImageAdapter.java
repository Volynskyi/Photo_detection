package zhdan.photo_detection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by vova on 17.03.2017.
 */

public class ImageAdapter extends BaseAdapter {

    private Context mContext;

    // Keep path in array
    static ArrayList<String> photosPath;

    public ImageAdapter(Context c) {
        this.mContext = c;
    }

    // Constructor
    public ImageAdapter(Context c, ArrayList<String> photosPath) {
        mContext = c;
        this.photosPath = photosPath;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return photosPath.size(); // длина массива
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return photosPath.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    public static class ViewHolder
    {
        public ImageView imgView;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        
        LayoutInflater inflater = LayoutInflater.from(mContext);

        if(convertView==null)
        {
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.grid_item_layout, null);

            viewHolder.imgView = (ImageView) convertView.findViewById(R.id.imageViewForGrid);

            convertView.setTag(viewHolder);
        }
        else
        {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Bitmap bitmap = BitmapFactory.decodeFile(photosPath.get(position));
        viewHolder.imgView.setImageBitmap(bitmap);

        return convertView;
    }
}
