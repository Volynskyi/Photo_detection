package zhdan.photo_detection;

class PhotoData {
    private String photoPath;
    private int id;
    private boolean visible;
    private boolean selected;

    PhotoData(String photoPath, int id, boolean selected, boolean visible) {
        this.photoPath = photoPath;
        this.selected = selected;
        this.visible = visible;
        this.id = id;
    }

    boolean isSelected() {
        return selected;
    }

    void setSelected(boolean selected) {
        this.selected = selected;
    }

    boolean isVisible() {
        return visible;
    }

    void setVisible(boolean visible) {
        this.visible = visible;
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
}
