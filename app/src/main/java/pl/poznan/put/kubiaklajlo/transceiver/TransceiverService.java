package pl.poznan.put.kubiaklajlo.transceiver;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TransceiverService extends Service {

    SharedPreferences.Editor editor;

    String TAG = "PL2303HXD_uart_service";
    private static final String ACTION_USB_PERMISSION = "pl.poznan.put.transceiver.transceiver.USB_PERMISSION";

    private String []initializeSequention =  {"15", "\r", "\n", "8000", "\r", "\n", "\r", "53", "\r", "\n"};
    //private String initializeSequention =  "15" + '\r' + "8000" + '\r' + '\r' + "53" + '\r';
    UsbSerialDevice uart;
    boolean uartInitialized = false;

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        editor = getSharedPreferences("txt", Context.MODE_PRIVATE).edit();

        initialization();
        writeDataToSerial();

    }

    private void initialization()
    {
        UsbManager usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        UsbDevice device = null;
        UsbDeviceConnection mConnection = null;
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if(!usbDevices.isEmpty())
        {
            boolean keep = true;
            for(Map.Entry<String, UsbDevice> entry : usbDevices.entrySet())
            {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();
                if(deviceVID != 0x1d6b || (devicePID != 0x0001 || devicePID != 0x0002 || devicePID != 0x0003))
                {
                    // We are supposing here there is only one device connected and it is our serial device
                    PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, mPendingIntent);
                    mConnection = usbManager.openDevice(device);
                    keep = false;
                }else
                {
                    mConnection = null;
                    device = null;
                }

                if(!keep)
                    break;
            }
        }
        if(device == null || mConnection == null)
        {
            editor.putString("text", "Konwerter UART jest niepodłączony" + '\r');
            editor.commit();
            return;
        }

        uart = UsbSerialDevice.createUsbSerialDevice(device, mConnection);

        if(uart != null)
        {
            if(uart.open())
            {
                // Devices are opened with default values, Usually 9600,8,1,None,OFF
                // CDC driver default values 9600,8,1,None,OFF
                uart.setBaudRate(9600);
                uart.setDataBits(UsbSerialInterface.DATA_BITS_8);
                uart.setStopBits(UsbSerialInterface.STOP_BITS_1);
                uart.setParity(UsbSerialInterface.PARITY_NONE);
                uart.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                uart.read(mCallback);
                uartInitialized = true;
            }else
            {
               Log.d(TAG, "Serial port could not be opened, maybe an I/O error or it CDC driver was chosen it does not really fit");
            }
        }else
        {
            Log.d(TAG, "No driver for given device, even generic CDC driver could not be loaded");
        }
    }

    private void writeDataToSerial() {
        if (!uartInitialized) return;
        for (int i = 0; i < initializeSequention.length; i++ ) {
            byte[] bytes = initializeSequention[i].getBytes(StandardCharsets.US_ASCII);
            uart.write(bytes);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Log.d(TAG, e.toString());
            }
        }
    }


    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback()
    {
        @Override
        public void onReceivedData(byte[] arg0)
        {
            String data;
            try {
                data = new String(arg0, "UTF-8");
            }
            catch (java.io.UnsupportedEncodingException e)
            {
                data = e.toString();
            }
            editor.putString("text", data + '\r');
            editor.putBoolean("refreshed", true);
            editor.commit();
            //sleep thread!
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Log.d(TAG, e.toString());
            }
        }
    };

    public TransceiverService() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (uart != null) uart.close();
        Log.d(TAG, "Service stopped!");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
