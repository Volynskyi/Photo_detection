package zhdan.photo_detection;

import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;

class ViewHolder {

    private ImageView imgView;
    private CheckBox checkBox;
    private View view;

    ViewHolder(View view) {
        this.view = view;
    }

    ImageView getImageView(){
        if (imgView == null){
            imgView = (ImageView) view.findViewById(R.id.itemImageView);
        }
        return imgView;
    }

    CheckBox getCheckBox(){
        if (checkBox == null){
            checkBox = (CheckBox) view.findViewById(R.id.itemCheckBox);
        }
        return checkBox;
    }
}
