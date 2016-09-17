package pl.poznan.put.kubiaklajlo.transceiver;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private Intent intent;
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
                startService(intent);
            }
        };

        View.OnClickListener Stop = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(intent);
            }
        };

        StartButton.setOnClickListener(Start);
        StopButton.setOnClickListener(Stop);
    }
}
