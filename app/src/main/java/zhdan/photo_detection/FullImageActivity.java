package zhdan.photo_detection;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.Toast;

public class FullImageActivity extends AppCompatActivity {

    private final String BITMAP = "bitmap";
    private Bitmap bitmap;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);
        imageView = (ImageView) findViewById(R.id.full_image_view);
        if (savedInstanceState != null) {
            bitmap = savedInstanceState.getParcelable(BITMAP);
        } else {
            Intent intent = getIntent();
            if (intent.hasExtra(GridLayoutDemoActivity.POSITION)) {
                int position = intent.getExtras().getInt(GridLayoutDemoActivity.POSITION);
                bitmap = BitmapFactory.decodeFile(GridLayoutDemoActivity.photoDataList.get(position).getPhotoPath());
            } else {
                Toast.makeText(this, "Image have been saved", Toast.LENGTH_SHORT).show();
                String filePath = intent.getExtras().getString(MainActivity.FILE_PATH);
                bitmap = BitmapFactory.decodeFile(filePath);
            }
        }
        imageView.setImageBitmap(bitmap);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(BITMAP, ((BitmapDrawable) imageView.getDrawable()).getBitmap());
    }
}
