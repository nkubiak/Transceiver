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
    Thread updateEditTextThread;
    boolean working;

    String TAG = "PL2303HXD_uart_activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button StartButton;
        Button StopButton;
        final EditText EditText;
        final Activity myActivity = this;

        intent = new Intent(getApplicationContext(), TransceiverService.class);

        StartButton = (Button) findViewById(R.id.buttonStart);
        StopButton = (Button) findViewById(R.id.buttonStop);
        EditText = (EditText) findViewById(R.id.editText);

       final Runnable runnableUpdateEditText = new Runnable() {
            @Override
            public void run() {
                final SharedPreferences preferences = getSharedPreferences("txt", Context.MODE_PRIVATE);
                while (working) {
                    final String text = preferences.getString("text", "");
                   try {
                        Thread.sleep(15);
                    }
                    catch (InterruptedException e) {
                        Log.d(TAG, e.toString());
                    }
                    boolean tmp = false;
                    boolean refreshed = preferences.getBoolean("refreshed", tmp);
                    if(refreshed) {
                        myActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                preferences.edit().putBoolean("refreshed", false).commit();
                                EditText.setText(text + EditText.getText());
                            }
                        });
                    }
                }
            }
        };


        View.OnClickListener Start = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!working) {
                    startService(intent);
                    updateEditTextThread = new Thread(runnableUpdateEditText);
                    updateEditTextThread.start();
                    working = true;
                }
            }
        };

        View.OnClickListener Stop = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(intent);
                working = false;
                try {
                    updateEditTextThread.join();
                }
                catch (InterruptedException e)
                {
                    Log.d(TAG, e.toString());
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
