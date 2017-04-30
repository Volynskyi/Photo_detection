package zhdan.photo_detection;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class ChangeNameActivity extends Activity {
    private EditText etName;
    private String stateName;
    private int index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_name);
        etName = (EditText) findViewById(R.id.etName);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        Intent intent = getIntent();
        stateName = intent.getStringExtra(MainActivity.FACE_TAG);
        index = intent.getIntExtra(MainActivity.INDEX_TAG, -1);
        etName.setText(stateName);
    }

    public void btnClick(View view) {
        Intent intent = new Intent();
        intent.putExtra(MainActivity.FACE_TAG, etName.getText().toString());
        intent.putExtra(MainActivity.INDEX_TAG, index);
        setResult(RESULT_OK, intent);
        finish();
    }
}
