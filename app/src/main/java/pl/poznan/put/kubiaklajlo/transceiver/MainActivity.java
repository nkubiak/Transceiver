package pl.poznan.put.kubiaklajlo.transceiver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private Intent intent;
    boolean working;

    String TAG = "PL2303HXD_uart_activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button StartButton;
        Button StopButton;

        intent = new Intent(getApplicationContext(), TransceiverService.class);

        StartButton = (Button) findViewById(R.id.buttonStart);
        StopButton = (Button) findViewById(R.id.buttonStop);


        View.OnClickListener Start = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!working) {
                    startService(intent);
                    working = true;
                }
            }
        };

        View.OnClickListener Stop = new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                working = false;
                try
                {
                    stopService(intent);
                }
                catch (java.lang.NullPointerException e)
                {
                    Log.d(TAG, e.toString());
                }
            }
        };

        StartButton.setOnClickListener(Start);
        StopButton.setOnClickListener(Stop);
    }

}
