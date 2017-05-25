package zhdan.photo_detection;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.opencv.core.Rect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

class PhotoLab {

    private Context context;

    PhotoLab(Context context) {
        this.context = context;
    }

    File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        MainActivity.currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    String getPhotoPath(Intent data) {
        Uri photoUri = data.getData();
        String[] currentPhotoPathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(photoUri, currentPhotoPathColumn, null, null, null);
        if(cursor.moveToFirst()){
            int columnIndex = cursor.getColumnIndex(currentPhotoPathColumn[0]);
            String photoPath = cursor.getString(columnIndex);
            cursor.close();
            return photoPath;
        }
        return null;
    }

    void saveBitmap(Bitmap bitmap) {
        FileOutputStream outputStream = null;
        File image = null;
        try {
            image = createImageFile();
            outputStream = new FileOutputStream(image);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            bitmap.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                    MediaStore.Images.Media.insertImage(context.getContentResolver(), MainActivity.currentPhotoPath, image.getName(), image.getName());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    Bitmap rotateBitmap(Bitmap bitmap, String currentPhotoPath) {
        int orientation = 0;
        try {
            ExifInterface imgParams = new
                    ExifInterface(currentPhotoPath);
            orientation =
                    imgParams.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_UNDEFINED);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    Bitmap drawBitmap(Rect[] facesArray, ImageView imageView, List<TextView> textViewList,
                      RelativeLayout imageAndTagsRelativeLayout) {
        final Bitmap backgroundBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        Bitmap copyOfBackgroundBitmap = Bitmap.createBitmap(backgroundBitmap.getWidth(),
                backgroundBitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(copyOfBackgroundBitmap);
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        LinearLayout textViewLinearLayout = new LinearLayout(context);
        int tagWidth;

        for (int i = 0; i < facesArray.length; i++) {
            imageAndTagsRelativeLayout.removeView(textViewList.get(i));
            TextView tagTextView = textViewList.get(i);
            tagTextView.setX(0);
            tagTextView.setY(0);
            setTextSize(tagTextView, imageView, backgroundBitmap);
            tagWidth = ((int) (facesArray[i].br().x - facesArray[i].tl().x));
            tagTextView.setWidth(tagWidth);
            textViewLinearLayout.addView(tagTextView);
            textViewLinearLayout.measure(canvas.getWidth(), canvas.getHeight());
            textViewLinearLayout.layout(0, 0, canvas.getWidth(), canvas.getHeight());
            placeTextView(facesArray[i], canvas, textViewLinearLayout);
        }
        return copyOfBackgroundBitmap;
    }

    private void placeTextView(Rect rect, Canvas canvas, LinearLayout containerForTextViews) {
        canvas.translate(((float) rect.tl().x), ((float) rect.br().y));
        containerForTextViews.draw(canvas);
        containerForTextViews.removeAllViews();
        canvas.setMatrix(null);
    }

    private void setTextSize(TextView tag, ImageView imageView, Bitmap backgroundBitmap) {
        double ratioWidth = imageView.getMeasuredWidth() / backgroundBitmap.getWidth();
        if (ratioWidth < 0.35) {
            tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        } else if (ratioWidth < 0.5) {
            tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        } else if (ratioWidth < 0.7) {
            tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        }else{
            tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        }
    }
}
