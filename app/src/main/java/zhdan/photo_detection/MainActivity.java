package zhdan.photo_detection;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_CODE_GALLERY = 0;
    private final int REQUEST_CODE_CAMERA = 1;
    private final int REQUEST_CODE_NAME = 2;
    private final float RELATIVE_FACE_SIZE = 0.2f;
    private final String PHOTO_MIME_TYPE = "image/png";
    private final String CURRENT_PHOTO_PATH = "currentPhotoPath";
    private final String SAVE_USED = "saveUsed";
    private final String DETECTION_USED = "detectionUsed";

    public static final String FACE_TAG = "faceTag";
    public static final String INDEX_TAG = "indexTag";
    public static final String FILE_PATH = "filePath";
    public static final String TAG = "FaceDetection";

    //Activity variables
    private Context context = this;
    private ImageView imageView;
    RelativeLayout imageAndTagsRelativeLayout;

    //OpenCV variables
    private OpenCVHelper openCVHelper;
    private Rect[] facesArray;

    private DBHelper dbHelper;
    private PhotoLab photoLab;
    static String currentPhotoPath;
    private ArrayList<TextView> textViewList;
    private boolean detectionUsed;
    private boolean saveUsed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        imageView = ((ImageView) findViewById(R.id.image_view));
        imageAndTagsRelativeLayout = (RelativeLayout) findViewById(R.id.relative_layout_image);
        textViewList = new ArrayList<>();
        dbHelper = new DBHelper(this);
        openCVHelper = new OpenCVHelper(getApplicationContext());
        photoLab = new PhotoLab(this);
        if (savedInstanceState != null && savedInstanceState.containsKey(CURRENT_PHOTO_PATH)) {
            currentPhotoPath = savedInstanceState.getString(CURRENT_PHOTO_PATH);
            Bitmap savedBitmap = BitmapFactory.decodeFile(currentPhotoPath);
            Bitmap rotatedBitmap = photoLab.rotateBitmap(savedBitmap, currentPhotoPath);
            imageView.setImageBitmap(rotatedBitmap);
            if (savedInstanceState.containsKey(SAVE_USED)) {
                saveUsed = savedInstanceState.getBoolean(SAVE_USED);
                detectionUsed = savedInstanceState.getBoolean(DETECTION_USED);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, openCVHelper.baseLoaderCallback);
        } else {
            openCVHelper.baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void btnClick(View view) {
        switch (view.getId()) {
            case R.id.btnNewImage:
                getFile(view);
                break;
            case R.id.btnDetect:
                if (imageView.getDrawable() == null) {
                    Toast.makeText(this, R.string.toast_on_detect_no_bitmap, Toast.LENGTH_SHORT).show();
                    break;
                }
                if (detectionUsed) {
                    Toast.makeText(this, R.string.toast_on_detect_image_already_saved, Toast.LENGTH_SHORT).show();
                    break;
                }
                Bitmap bitmapBeforeDetection = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                Bitmap bitmapAfterDetection = faceDetection(bitmapBeforeDetection);
                bitmapBeforeDetection.recycle();
                imageView.setImageBitmap(bitmapAfterDetection);
                break;
        }
    }

    private void getFile(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.inflate(R.menu.popup);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.gallery:
                        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                        photoPickerIntent.setType(getString(R.string.photo_picker_intent_type));
                        startActivityForResult(photoPickerIntent, REQUEST_CODE_GALLERY);
                        break;
                    case R.id.camera:
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                            File photoFile = null;
                            try {
                                photoFile = photoLab.createImageFile();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            if (photoFile != null) {
                                Uri photoUri = FileProvider.getUriForFile(getApplicationContext(),
                                        getString(R.string.authority),
                                        photoFile);
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                                startActivityForResult(takePictureIntent, REQUEST_CODE_CAMERA);
                            }
                        }
                        break;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private Bitmap faceDetection(Bitmap bitmap) {
        int absoluteFaceSize = 0;
        Mat imageMat = new Mat(bitmap.getHeight(),
                bitmap.getWidth(), CvType.CV_8UC4);
        Mat grayMat = new Mat();
        Utils.bitmapToMat(bitmap.copy(Bitmap.Config.RGB_565, true), imageMat);
        Imgproc.cvtColor(imageMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        int height = grayMat.rows();
        if (Math.round(height * RELATIVE_FACE_SIZE) > 0) {
            absoluteFaceSize = Math.round(height * RELATIVE_FACE_SIZE);
        }
        MatOfRect faces = new MatOfRect();
        openCVHelper.getCascadeClassifier().detectMultiScale(grayMat, faces, 1.3, 2, 2,
                new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        facesArray = faces.toArray();

        double bitmapWidth = bitmap.getWidth();
        double bitmapHeight = bitmap.getHeight();
        bitmap.recycle();
        double ratioWidth = imageView.getMeasuredWidth() / bitmapWidth;
        double ratioHeight = imageView.getMeasuredHeight() / bitmapHeight;
        int lpWidth;
        textViewList.clear();

        for (int i = 0; i < facesArray.length; i++) {
            Imgproc.rectangle(imageMat, facesArray[i].br(), facesArray[i].tl(), new Scalar(0, 255, 0, 255), 2);
            lpWidth = (((int) ((facesArray[i].br().x - facesArray[i].tl().x) * ratioWidth)));
            final TextView textViewTag = new TextView(this);
            textViewTag.setText(String.valueOf(i));
            textViewTag.setTextColor(ContextCompat.getColor(context, R.color.colorText));
            textViewTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            textViewList.add(textViewTag);
            textViewTag.setX((float) (facesArray[i].tl().x * ratioWidth));
            textViewTag.setY((float) (facesArray[i].br().y * ratioHeight));
            textViewTag.setWidth(lpWidth);
            final int index = i;
            textViewTag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), ChangeNameActivity.class);
                    intent.putExtra(FACE_TAG, textViewTag.getText().toString());
                    intent.putExtra(INDEX_TAG, index);
                    startActivityForResult(intent, REQUEST_CODE_NAME);
                }
            });
            imageAndTagsRelativeLayout.addView(textViewList.get(i));
        }

        Bitmap imageBitmap = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(imageMat, imageBitmap);
        detectionUsed = true;
        return imageBitmap;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_share:
                if (!saveUsed) {
                    Toast.makeText(this, R.string.toast_error_to_share, Toast.LENGTH_SHORT).show();
                    return true;
                }
                sharePhoto();
                return true;

            case R.id.item_save:
                if (imageAndTagsRelativeLayout.getChildCount() == 0 || (imageView.getDrawable() == null)) {
                    Toast.makeText(this, R.string.toast_on_save_no_data, Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (saveUsed) {
                    Toast.makeText(this, R.string.toast_on_save_used_before, Toast.LENGTH_SHORT).show();
                    return true;
                }
                Bitmap canvasBitmap = photoLab.drawBitmap(facesArray, imageView, textViewList, imageAndTagsRelativeLayout);
                photoLab.saveBitmap(canvasBitmap);
                List<String> tagNames = new ArrayList<>(textViewList.size());
                for (TextView textView : textViewList) {
                    tagNames.add(textView.getText().toString());
                }
                dbHelper.insertTagsIntoDB(tagNames, currentPhotoPath);
                saveUsed = true;
                Intent i = new Intent(this, FullImageActivity.class);
                i.putExtra(FILE_PATH, currentPhotoPath);
                startActivity(i);
                return true;

            case R.id.item_search:
                Intent intent = new Intent(this, GridLayoutDemoActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sharePhoto() {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(PHOTO_MIME_TYPE);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(currentPhotoPath)));
        intent.putExtra(Intent.EXTRA_SUBJECT,
                getString(R.string.photo_send_extra_subject));
        intent.putExtra(Intent.EXTRA_TEXT,
                getString(R.string.photo_send_extra_text));
        startActivity(Intent.createChooser(intent,
                getString(R.string.photo_send_chooser_title)));
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_NAME:
                    String tag = data.getStringExtra(FACE_TAG);
                    textViewList.get(data.getIntExtra(INDEX_TAG, -1)).setText(tag);
                    break;

                case REQUEST_CODE_GALLERY:
                    detectionUsed = false;
                    saveUsed = false;
                    if (!textViewList.isEmpty()) {
                        for (TextView textView : textViewList) {
                            textView.setVisibility(View.GONE);
                        }
                    }
                    currentPhotoPath = photoLab.getPhotoPath(data);

                    Bitmap tempGalleryBitmap = BitmapFactory.decodeFile(currentPhotoPath);
                    Bitmap originalGalleryBitmap = photoLab.rotateBitmap(tempGalleryBitmap, currentPhotoPath);
                    imageView.setImageBitmap(originalGalleryBitmap);
                    break;

                case REQUEST_CODE_CAMERA:
                    detectionUsed = false;
                    saveUsed = false;
                    if (!textViewList.isEmpty()) {
                        for (TextView textView : textViewList) {
                            textView.setVisibility(View.GONE);
                        }
                    }
                    Bitmap tempCameraBitmap = BitmapFactory.decodeFile(currentPhotoPath);
                    Bitmap scaledCameraBitmap = Bitmap.createScaledBitmap(tempCameraBitmap, 800, 600, true);
                    tempCameraBitmap.recycle();
                    Bitmap originalCameraBitmap = photoLab.rotateBitmap(scaledCameraBitmap, currentPhotoPath);
                    imageView.setImageBitmap(originalCameraBitmap);
                    break;
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        if (imageView.getDrawable() != null) {
            savedInstanceState.putString(CURRENT_PHOTO_PATH, currentPhotoPath);
            if (saveUsed) {
                savedInstanceState.putBoolean(SAVE_USED, saveUsed);
                savedInstanceState.putBoolean(DETECTION_USED, detectionUsed);
            }
        }
        super.onSaveInstanceState(savedInstanceState);
    }
}