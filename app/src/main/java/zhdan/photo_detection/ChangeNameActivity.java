package zhdan.photo_detection;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


/**
 * Created by vova on 07.03.2017.
 */
public class ChangeNameActivity extends Activity implements View.OnClickListener{
    EditText etName;
    Button btnName;
    String state_name;
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
        state_name = intent.getStringExtra(MainActivity.FACE_TAG);
        index = intent.getIntExtra(MainActivity.INDEX_TAG, -1);
        Log.d(TAG, "" + index);

        etName.setText(state_name);
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
