package zhdan.photo_detection;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.Toast;

public class FullImageActivity extends AppCompatActivity {

    Bitmap bitmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);
        ImageView imageView = (ImageView) findViewById(R.id.full_image_view);

        Intent intent = getIntent();
        if (intent.hasExtra(GridLayoutDemoActivity.POSITION)) {
            int position = intent.getExtras().getInt(GridLayoutDemoActivity.POSITION);
            ImageAdapter imageAdapter = new ImageAdapter();
            bitmap = BitmapFactory.decodeFile(imageAdapter.photoDataList.get(position).getPhotoPath());
            imageView.setImageBitmap(bitmap);

        } else if (intent.hasExtra(MainActivity.FILE_PATH)) {
            Toast.makeText(this, "Image have been saved", Toast.LENGTH_SHORT).show();
            String filepath = intent.getExtras().getString(MainActivity.FILE_PATH);
            bitmap = BitmapFactory.decodeFile(filepath);
            imageView.setImageBitmap(bitmap);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
    }
}
