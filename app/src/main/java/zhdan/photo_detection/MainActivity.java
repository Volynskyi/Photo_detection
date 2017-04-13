package zhdan.photo_detection;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
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
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_CODE_GALLERY = 0;
    private final int REQUEST_CODE_CAMERA = 1;
    private final int REQUEST_CODE_NAME = 2;
    private final float RELATIVE_FACE_SIZE = 0.2f;

    public static final String TAG = "FaceDetectionActivity";
    public static final String FACE_TAG = "faceTag";
    public static final String INDEX_TAG = "indexTag";
    public static final String FILE_PATH = "filePath";

    private final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);

    //Activity variables
    private Context context = this;
    private ImageView imageView;
    private RelativeLayout imageAndTagsRelativeLayout;

    // OpenCV variables
    private CascadeClassifier cascadeClassifier;
    private Mat imageMat;
    private Rect[] facesArray;
    private int absoluteFaceSize;

    //DB variables
    private DBHelper dbHelper;

    private String currentPhotoPath;
    private Uri photoURI;
    private List<TextView> textViewList;
    private boolean detectionUsed;
    private double ratioWidth;

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    try {
                        loadCascadeFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.i(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }
                    imageMat = new Mat();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }

        private void loadCascadeFile() throws IOException {
            InputStream is = context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            cascadeClassifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
            if (cascadeClassifier.empty()) {
                Log.e("TAG", "Failed to load cascade classifier");
                cascadeClassifier = null;
            } else
                Log.i("TAG", "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());

            cascadeFile.delete();
            cascadeDir.delete();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = ((ImageView) findViewById(R.id.image_view1));
        imageAndTagsRelativeLayout = (RelativeLayout) findViewById(R.id.relative_layout2);
        textViewList = new ArrayList<>();
        dbHelper = new DBHelper(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, baseLoaderCallback);
        } else {
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void btnClick(View view) {
        switch (view.getId()) {
            case R.id.btnNewImage:
                getFile(view);
                break;
            case R.id.btnDetect:
                if(imageView.getDrawable() == null){
                    Toast.makeText(this, "First choose an image", Toast.LENGTH_SHORT).show();
                    break;
                }

                Bitmap bitmapBeforeDetection = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                Bitmap bitmapAfterDetection  = faceDetection(bitmapBeforeDetection);
                bitmapBeforeDetection.recycle();
                if(bitmapAfterDetection == null){
                    Toast.makeText(this, "Image have been used already for detection", Toast.LENGTH_SHORT).show();
                    break;
                }
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
                        photoPickerIntent.setType("image/*");
                        startActivityForResult(photoPickerIntent, REQUEST_CODE_GALLERY);
                        break;
                    case R.id.camera:
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        // Ensure that there's a camera activity to handle the intent
                        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                            // Create the File where the photo should go
                            File photoFile = null;
                            try {
                                photoFile = createImageFile();
                            } catch (IOException ex) {
                                Log.e(TAG, "Failed to create image file");
                                ex.printStackTrace();
                            }
                            // Continue only if the File was successfully created
                            if (photoFile != null) {
                                photoURI = FileProvider.getUriForFile(getApplicationContext(),
                                        "com.example.android.fileprovider",
                                        photoFile);
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                                startActivityForResult(takePictureIntent, REQUEST_CODE_CAMERA);
                            }
                        }
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private Bitmap faceDetection(Bitmap bitmap) {
        if(detectionUsed){
            return null;
        }
        imageMat = new Mat(bitmap.getHeight(),
                bitmap.getWidth(), CvType.CV_8UC4);
        Mat grayMat = new Mat();
        Utils.bitmapToMat(bitmap.copy(Bitmap.Config.ARGB_8888, true), imageMat);
        Imgproc.cvtColor(imageMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        int height = grayMat.rows();
        if (Math.round(height * RELATIVE_FACE_SIZE) > 0) {
            absoluteFaceSize = Math.round(height * RELATIVE_FACE_SIZE);
        }

        MatOfRect faces = new MatOfRect();
        cascadeClassifier.detectMultiScale(grayMat, faces, 1.3, 2, 2,
                new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        facesArray = faces.toArray();

        double bitmapWidth = bitmap.getWidth();
        double bitmapHeight = bitmap.getHeight();

        bitmap.recycle();

        ratioWidth = imageView.getMeasuredWidth() / bitmapWidth;
        double ratioHeight = imageView.getMeasuredHeight() / bitmapHeight;

        int lpWidth;
        textViewList.clear();

        for (int i = 0; i < facesArray.length; i++) {
            Imgproc.rectangle(imageMat, facesArray[i].br(), facesArray[i].tl(), FACE_RECT_COLOR, 2);
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
                    intent.putExtra(INDEX_TAG, index); //ідентифікуєм вибраний елемент
                    startActivityForResult(intent, REQUEST_CODE_NAME);
                }
            });
            imageAndTagsRelativeLayout.addView(textViewList.get(i));
        }

        Bitmap imageBitmap = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imageMat, imageBitmap);

        detectionUsed = true;
        return imageBitmap;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_save:
                if(imageAndTagsRelativeLayout.getChildCount() == 0 || (imageView.getDrawable() == null)){
                    Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show();
                    break;
                }
                drawBitmap(facesArray);

                List<String> tagNames = new ArrayList<>(textViewList.size());
                for (TextView textView : textViewList) {
                    tagNames.add(textView.getText().toString());
                }

                insertTagsIntoDB(tagNames);
                break;
            case R.id.item_search:
                Intent intent = new Intent(this, GridLayoutDemoActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void insertTagsIntoDB(List<String> tags) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        for (String tag : tags) {
            contentValues.put(DBHelper.KEY_NAME, tag);
            contentValues.put(DBHelper.KEY_PATH, currentPhotoPath);
            database.insert(DBHelper.TABLE_NAME, null, contentValues);
        }

        Cursor cursor;
        cursor = database.query(DBHelper.TABLE_NAME, null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex(DBHelper.KEY_ID);
            int nameIndex = cursor.getColumnIndex(DBHelper.KEY_NAME);
            int emailIndex = cursor.getColumnIndex(DBHelper.KEY_PATH);
            do {
                Log.d("mLog", "ID = " + cursor.getInt(idIndex) +
                        ", name = " + cursor.getString(nameIndex) +
                        ", email = " + cursor.getString(emailIndex));
            } while (cursor.moveToNext());
        } else {
            Log.d("mLog", "0 rows");
        }
        cursor.close();
        database.close();
    }

    private void drawBitmap(Rect[] facesArray) {
        final Bitmap backgroundBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        Bitmap copyOfBackgroundBitmap = Bitmap.createBitmap(backgroundBitmap.getWidth(), backgroundBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(copyOfBackgroundBitmap);
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        LinearLayout textViewLinearLayout = new LinearLayout(this);
        int tagWidth;

//Draw the image bitmap into the canvas
        for (int i = 0; i < facesArray.length; i++) {
            imageAndTagsRelativeLayout.removeView(textViewList.get(i));
            TextView tagTextView = textViewList.get(i);
            tagTextView.setX(0);
            tagTextView.setY(0);
            setTextSize(tagTextView);

            tagWidth = ((int) (facesArray[i].br().x - facesArray[i].tl().x));
            tagTextView.setWidth(tagWidth);

            textViewLinearLayout.addView(tagTextView);
            textViewLinearLayout.measure(canvas.getWidth(), canvas.getHeight());
            textViewLinearLayout.layout(0, 0, canvas.getWidth(), canvas.getHeight());
            placeTextView(facesArray[i], canvas, textViewLinearLayout);
        }
        saveBitmap(copyOfBackgroundBitmap);
        Intent i = new Intent(this, FullImageActivity.class);
        i.putExtra(FILE_PATH, currentPhotoPath);
        startActivity(i);
    }

    private void placeTextView(Rect rect, Canvas canvas, LinearLayout containerForTextViews) {
        canvas.translate(((float) rect.tl().x), ((float) rect.br().y));
        containerForTextViews.draw(canvas);
        containerForTextViews.removeAllViews();
        canvas.setMatrix(null);
    }

    private void setTextSize(TextView tag) {
        if(ratioWidth < 0.35){
            tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        }else if(ratioWidth < 0.5){
            tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        }else if(ratioWidth < 0.7){
            tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        }
    }

    private void saveBitmap(Bitmap bitmap) {
        FileOutputStream outputStream = null;
        File image = null;
        try {
            image = createImageFile();
            outputStream = new FileOutputStream(image);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                    MediaStore.Images.Media.insertImage(getContentResolver(), currentPhotoPath, image.getName(), image.getName());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

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
                    if (!textViewList.isEmpty()) {
                        for (TextView textView : textViewList) {
                            textView.setVisibility(View.GONE);
                        }
                    }
                    currentPhotoPath = getPhotoPath(data);

                    Bitmap tempGalleryBitmap = BitmapFactory.decodeFile(currentPhotoPath);
                    Bitmap originalGalleryBitmap = rotateBitmap(tempGalleryBitmap, currentPhotoPath);
                    imageView.setImageBitmap(originalGalleryBitmap);
                    break;

                case REQUEST_CODE_CAMERA:
                    detectionUsed = false;
                    if (!textViewList.isEmpty()) {
                        for (TextView textView : textViewList) {
                            textView.setVisibility(View.GONE);
                        }
                    }
                    Bitmap tempCameraBitmap = BitmapFactory.decodeFile(currentPhotoPath);
                    Bitmap scaledCameraBitmap = Bitmap.createScaledBitmap(tempCameraBitmap, 800, 600, true);
                    Bitmap originalCameraBitmap = rotateBitmap(scaledCameraBitmap, currentPhotoPath);
                    imageView.setImageBitmap(originalCameraBitmap);
                    break;
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private String getPhotoPath(Intent data) {
        Uri photoUri = data.getData();
        String[] currentPhotoPathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(photoUri, currentPhotoPathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(currentPhotoPathColumn[0]);
        String photoPath = cursor.getString(columnIndex);
        cursor.close();
        return photoPath;
    }

    public Bitmap rotateBitmap(Bitmap bitmap, String currentPhotoPath) {
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
}
