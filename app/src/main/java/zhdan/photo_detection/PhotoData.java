package zhdan.photo_detection;

import android.os.Parcel;
import android.os.Parcelable;

class PhotoData implements Parcelable {
    private String photoPath;
    private int id;
    private boolean visibleCheckBox;
    private boolean selected;

    PhotoData(String photoPath, int id, boolean selected, boolean visibleCheckBox) {
        this.photoPath = photoPath;
        this.selected = selected;
        this.visibleCheckBox = visibleCheckBox;
        this.id = id;
    }

    private PhotoData(Parcel in) {
        photoPath = in.readString();
        id = in.readInt();
        visibleCheckBox = in.readByte() != 0;
        selected = in.readByte() != 0;
    }

    public static final Creator<PhotoData> CREATOR = new Creator<PhotoData>() {
        @Override
        public PhotoData createFromParcel(Parcel in) {
            return new PhotoData(in);
        }

        @Override
        public PhotoData[] newArray(int size) {
            return new PhotoData[size];
        }
    };

    boolean isSelected() {
        return selected;
    }

    void setSelected(boolean selected) {
        this.selected = selected;
    }

    boolean isVisibleCheckBox() {
        return visibleCheckBox;
    }

    void setVisibleCheckBox(boolean visibleCheckBox) {
        this.visibleCheckBox = visibleCheckBox;
    }

    String getPhotoPath() {
        return photoPath;
    }

    int getId() {
        return id;
    }

    void setId(int id) {
        this.id = id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(photoPath);
        dest.writeInt(id);
        dest.writeByte((byte) (visibleCheckBox ? 1 : 0));
        dest.writeByte((byte) (selected ? 1 : 0));
    }
}
