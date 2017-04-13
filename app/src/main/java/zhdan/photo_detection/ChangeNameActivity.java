package zhdan.photo_detection;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class ChangeNameActivity extends Activity implements View.OnClickListener {
    EditText etName;
    Button btnName;
    String stateName;
    final static String TAG = "FaceDetectionActivity";
    int index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_name);

        etName = (EditText) findViewById(R.id.etName);
        btnName = (Button) findViewById(R.id.btnName);
        btnName.setOnClickListener(this);

        Intent intent = getIntent();
        stateName = intent.getStringExtra(MainActivity.FACE_TAG);
        index = intent.getIntExtra(MainActivity.INDEX_TAG, -1);
        Log.d(TAG, "" + index);

        etName.setText(stateName);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        intent.putExtra(MainActivity.FACE_TAG, etName.getText().toString());
        intent.putExtra(MainActivity.INDEX_TAG, index);
        setResult(RESULT_OK, intent);
        finish();
    }
}
