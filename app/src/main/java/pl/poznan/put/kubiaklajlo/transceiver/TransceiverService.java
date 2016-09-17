package pl.poznan.put.kubiaklajlo.transceiver;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import tw.com.prolific.driver.pl2303.PL2303Driver;

public class TransceiverService extends Service {

    private PL2303Driver uart;

    String TAG = "PL2303HXD_uart";

    private String initializeSequention = "15" + '\r' + "8000" + '\r' + '\r' + "53" + '\r';

    private PL2303Driver.BaudRate mBaudrate = PL2303Driver.BaudRate.B9600;
    private PL2303Driver.DataBits mDataBits = PL2303Driver.DataBits.D8;
    private PL2303Driver.Parity mParity = PL2303Driver.Parity.NONE;
    private PL2303Driver.StopBits mStopBits = PL2303Driver.StopBits.S1;
    private PL2303Driver.FlowControl mFlowControl = PL2303Driver.FlowControl.OFF;
    private static final String ACTION_USB_PERMISSION = "pl.poznan.put.transceiver.transceiver.USB_PERMISSION";

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        uart = new PL2303Driver((UsbManager)getSystemService(Context.USB_SERVICE),
                getApplicationContext(), ACTION_USB_PERMISSION);

        initialization();
        writeDataToSerial();

    }

    private void initialization()
    {
        if (!uart.InitByBaudRate(mBaudrate,700)) { // INITIALIZATION HERE!!
            if(!uart.PL2303Device_IsHasPermission()) {
                Toast.makeText(this, "cannot open, maybe no permission", Toast.LENGTH_SHORT).show();
            }

            if(uart.PL2303Device_IsHasPermission() && (!uart.PL2303Device_IsSupportChip())) {
                Toast.makeText(this, "cannot open, maybe this chip has no support, please use PL2303HXD / RA / EA chip.", Toast.LENGTH_SHORT).show();
            }
        } else {

            Toast.makeText(this, "connected : " , Toast.LENGTH_SHORT).show();
        }
    }

    private void writeDataToSerial() {

        Log.d(TAG, "Enter writeDataToSerial");

        if(null==uart)
            return;

        if(!uart.isConnected())
            return;

        Log.d(TAG, "PL2303Driver Write 2(" + initializeSequention.length() + ") : " + initializeSequention);

        int res = uart.write(initializeSequention.getBytes(), initializeSequention.length());
        if( res<0 ) {
            Log.d(TAG, "setup2: fail to controlTransfer: "+ res);
            return;
        }

        Toast.makeText(this, "Write length: " + initializeSequention.length() + " bytes", Toast.LENGTH_SHORT).show();

        Log.d(TAG, "Leave writeDataToSerial");
    }

    public TransceiverService() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        uart.end();
        Log.d(TAG, "Service stopped!");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
