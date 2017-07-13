package com.studi.timesyncwifi.Activity;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.studi.timesyncwifi.R;
import com.studi.timesyncwifi.Dialogs.SelectPositionDialog;
import com.studi.timesyncwifi.Utility.OpenCVNative;

public class TimeSyncActivity extends AppCompatActivity {

    /*
        The official app
     */

    private static final String TAG = "TimeSync";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.launch_layout);
        Log.d(TAG, OpenCVNative.function());

        Button hog2Button = (Button) findViewById(R.id.hog2Button);
        Button hog1Button = (Button) findViewById(R.id.hog1Button);
        Button backButton = (Button) findViewById(R.id.backButton);

        hog1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Integer.parseInt(Build.VERSION.RELEASE.split("\\.")[0])>=5) {
                    Intent intent = new Intent(getApplicationContext(), ServerActivity.class);
                    startActivity(intent);
                    //if (!(this instanceof TimeSyncActivity)) {
                    //    finish();
                    //}
                } else {
                    Toast.makeText(getApplicationContext(), "To old Android Version to be Server", Toast.LENGTH_LONG).show();
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ClientActivity.class);
                startActivity(intent);
                //if (!(getCallingActivity() instanceof TimeSyncActivity)) {
                //    finish();
                //}
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //selectPlace();
    }

    /*
    private void selectPlace() {
        DialogFragment dialog = new SelectPositionDialog();
        dialog.show(getSupportFragmentManager(), "SelectPositionDialog");
    }
    */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_settings_id: {
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

}
