package pl.poznan.put.kubiaklajlo.transceiver;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TransceiverService extends Service {

    SharedPreferences.Editor editor;

    // UART
    String TAG = "PL2303HXD_uart_service";
    private static final String ACTION_USB_PERMISSION = "pl.poznan.put.transceiver.transceiver.USB_PERMISSION";

    private String []initializeSequention =  {"15", "\r", "\n", "8000", "\r", "\n", "\r", "53", "\r", "\n"};
    UsbSerialDevice uart;
    boolean uartInitialized = false;
    //

    // Bluetooth
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private BluetoothDevice btDevice = null;

    // Well known SPP UUID
    private static final UUID SPP_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Insert your server's MAC address
    private static String address = "BC:77:37:2F:54:08";
    //

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        editor = getSharedPreferences("txt", Context.MODE_PRIVATE).edit();

        if(initializationBluetooth() == 0) {
            if (initializationUART() == 0) {
                writeDataToSerial();
            } else {
                return;
            }
        }
            else {
            return;
        }


    }

    private int initializationBluetooth()
    {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter==null) {
            Log.d(TAG, "Bluetooth Not supported. Aborting.");
            return 1;
        }
        else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "Bluetooth is enabled...");
            }
            else {
                //Prompt user to turn on Bluetooth
                /*Intent BtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(BtIntent);*/
                btAdapter.enable();
                while (!btAdapter.isEnabled()); // waiting for bluetooth
            }
        }
        btDevice = btAdapter.getRemoteDevice(address);

        try {
            btSocket = btDevice.createRfcommSocketToServiceRecord(SPP_UUID);
            btAdapter.cancelDiscovery();
            btSocket.connect();
            outStream = btSocket.getOutputStream();
            editor.putString("text", "Połączono z serwerem. Zaraz nastąpi transmisja!" + '\r');
            editor.putBoolean("refreshed", true);
            editor.commit();
            Log.d(TAG, "Connection established and data link opened...");
        }
        catch (IOException e) {
            editor.putString("text", "Nie mogę nawiązać połączenia Bluetooth. MSG: " + e.toString() + '\r');
            editor.putBoolean("refreshed", true);
            editor.commit();
            try {
                btSocket.close();
            } catch (IOException e2) {
                Log.d(TAG, e2.toString());
            }
            return 2;
        }

        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            Log.d(TAG, e.toString());
        }

        return 0;
    }

    private int initializationUART()
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
                    PendingIntent UARTPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, UARTPendingIntent);
                    mConnection = usbManager.openDevice(device);
                    keep = false;
                }
                else
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
            editor.putBoolean("refreshed", true);
            editor.commit();
            return 1;
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
        return 0;
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
            /*String data;
            try {
                data = new String(arg0, "UTF-8");
            }
            catch (java.io.UnsupportedEncodingException e)
            {
                data = e.toString();
            }*/
            try {
                outStream.write(arg0);
            }
            catch (IOException e)
            {
                editor.putString("text", "Problem z nadawaniem." + e.toString() + '\r');
                editor.putBoolean("refreshed", true);
                editor.commit();
            }
            /*editor.putString("text", data + '\r');
            editor.putBoolean("refreshed", true);
            editor.commit();*/
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
        try {
            if (outStream != null) outStream.flush();
            if (btSocket != null) btSocket.close();
            if (btAdapter != null) btAdapter.disable();
            editor.putString("text", "Zakończono transmisję" + '\r');
            editor.putBoolean("refreshed", true);
            editor.apply();
        }
        catch (IOException e)
        {
            Log.d(TAG, "Something went wrong with closing connection: " + e.toString());
        }
        Log.d(TAG, "Service stopped!");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
