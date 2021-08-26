package com.example.testing_bluetooth;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.apache.commons.lang3.StringUtils;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends AppCompatActivity {

    ConnectThread c;
    ConnectedThread ct;
    static String RecieveBuffer=null;
    private static final String TAG = "MY_BT";
    private static BluetoothResponseHandler mHandler;
    UUID BHANU_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int PERMISSION_CODE = 1;
    //Activity activity = MainActivity.this;
    private BluetoothAdapter bluetoothAdapter = null;
    TextView textview;
    TextView receive;
    ListView Paired;
    EditText stext;
    ListView blist;
    ListView slist;
    ArrayList<BluetoothDevice> devices = new ArrayList<>();
    ArrayList<BluetoothDevice> devices1 = new ArrayList<>();
    ArrayAdapter<BluetoothDevice> arrayAdapter;
    ArrayAdapter<String> arrayAdapter1;
    ArrayAdapter<String> arrayAdapter2;
    public static final int MESSAGE_READ = 1;

    FileWriter fw;
    File file;


    private LineGraphSeries<DataPoint> series;
    private int lastX = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,}, 1);
            }
        }

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mHandler == null) mHandler = new BluetoothResponseHandler(MainActivity.this);
        else mHandler.setTarget(MainActivity.this);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        MainActivity.this.registerReceiver(receiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        MainActivity.this.registerReceiver(receiver, filter);

        filter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        MainActivity.this.registerReceiver(receiver, filter);



        Button button = findViewById(R.id.button);
        Button send = findViewById(R.id.send);
        Button pair = findViewById(R.id.pair);
        textview = findViewById(R.id.textView);
        stext = findViewById(R.id.stext);
        Paired = findViewById(R.id.paired);

        pairedList();

        // we get graph view instance
        GraphView graph = (GraphView) findViewById(R.id.graph);
        // data
        series = new LineGraphSeries<DataPoint>();
        graph.addSeries(series);
        // customize a little bit viewport
        Viewport viewport = graph.getViewport();
//        viewport.setYAxisBoundsManual(true);
//        viewport.setMinY(-1000);
//        viewport.setMaxY(1000);
        viewport.setMinX(0);
        viewport.setMaxX(200);
        viewport.setScrollable(true);
        viewport.setScalable(true);
       // viewport.computeScroll();
        //viewport.scrollToEnd();

        File folder = new File(getExternalFilesDir(null) + "/dataFolder");

        boolean var = false;
        if (!folder.exists()){
            Log.d("App", "failed to create directory");
            var = folder.mkdir();
        }

        final String filename = folder.toString() + "/" + "PPG.txt";

        file = new File(filename);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        button.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            LayoutInflater inflater = LayoutInflater.from(this);
            View Viewpage = inflater.inflate(R.layout.listlayout, findViewById(R.id.btlist));
            blist = Viewpage.findViewById(R.id.list);
            builder.setView(Viewpage);
            arrayAdapter1 = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            blist.setAdapter(arrayAdapter1);
            //blist.setClickable(true);

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for(BluetoothDevice device : pairedDevices) {
                arrayAdapter1.add(device.getName() + "\n" + device.getAddress());

            }

            AlertDialog dialog = builder.create();
            arrayAdapter1.notifyDataSetChanged();
            dialog.show();

            blist.setOnItemClickListener((parent, view, position, id) -> {
                String got = (String) parent.getItemAtPosition(position);
                String a = got.substring(got.indexOf("\n"));
                String b = a.trim();
                if ( c != null )
                {
                    c.cancel();
                    c = null;
                }
                Toast.makeText(MainActivity.this, b,
                        Toast.LENGTH_SHORT).show();
                c = new ConnectThread(bluetoothAdapter.getRemoteDevice(b), false);
                c.start();
                dialog.cancel();
            });
        });

        send.setOnClickListener(v -> {
            ConnectedThread r;
            // Synchronize a copy of the ConnectedThread
            String str = stext.getText().toString();
            r = ct;
            r.write(str.getBytes());
        });

        pair.setOnClickListener(v -> {

            Toast.makeText(MainActivity.this, "  Searching",
                    Toast.LENGTH_SHORT).show();
            if (bluetoothAdapter.isDiscovering())
            {
                bluetoothAdapter.cancelDiscovery();
            }

            boolean s = bluetoothAdapter.startDiscovery();
            if (s)
            {
                Toast.makeText(MainActivity.this, "  Started",
                        Toast.LENGTH_SHORT).show();
            }
            else
            {

                Toast.makeText(MainActivity.this, " Not Started",
                        Toast.LENGTH_SHORT).show();
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            LayoutInflater inflater = LayoutInflater.from(this);
            View Viewpage = inflater.inflate(R.layout.listlayout, findViewById(R.id.btlist));
            slist = Viewpage.findViewById(R.id.list);
            builder.setView(Viewpage);
            arrayAdapter2 = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,0);
            slist.setAdapter(arrayAdapter2);
            AlertDialog dialog = builder.create();
            arrayAdapter2.notifyDataSetChanged();
            dialog.show();
            slist.setOnItemClickListener((parent, view, position, id) -> {
                String got = (String) parent.getItemAtPosition(position);
                String a = got.substring(got.indexOf("\n"));
                String b = a.trim();
                Toast.makeText(MainActivity.this, b,
                        Toast.LENGTH_SHORT).show();
                bluetoothAdapter.cancelDiscovery();
                dialog.cancel();
            });
        });


        Paired.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position);
            Toast.makeText(MainActivity.this, device.getName(),
                    Toast.LENGTH_SHORT).show();

            if ( c != null )
            {
                c.cancel();
                c = null;
            }

            c = new ConnectThread(bluetoothAdapter.getRemoteDevice(device.getAddress()), false);
            c.start();
        });


    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    public void onStart() {
        super.onStart();
        if (!bluetoothAdapter.isEnabled())
        {
            Activity activity = MainActivity.this;
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    public void pairedList() {
        //arrayAdapter.clear();
        //devices.addAll(bluetoothAdapter.getBondedDevices());
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        devices.addAll(pairedDevices);
        arrayAdapter = new ArrayAdapter<>(this, R.layout.layout, R.id.textView2, devices);
        Paired.setAdapter(arrayAdapter);
        arrayAdapter.notifyDataSetChanged();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK)
                {
                    Toast.makeText(MainActivity.this, "Enabled",
                            Toast.LENGTH_SHORT).show();

                } else
                    {
                        Toast.makeText(MainActivity.this, "Not Enabled",
                                Toast.LENGTH_SHORT).show();
                }
        }
    }


    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Toast.makeText(MainActivity.this, "Found",
                        Toast.LENGTH_SHORT).show();
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                textview.append("\n  Device: " + device.getName());
                arrayAdapter2.add(device.getName() + "\n" + device.getAddress());

            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
                    .equals(action)) {
                Toast.makeText(MainActivity.this, "  Discovery over",
                        Toast.LENGTH_SHORT).show();
            }

        }
    };

    public void toast(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }


    public class ConnectThread extends Thread {
        BluetoothDevice cDevice;
        BluetoothSocket socket;

        ConnectThread(BluetoothDevice device, boolean insecureConnection) {
            cDevice = device;
            socket = BluetoothUtils.createRfcommSocket(cDevice);
            //socket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID)
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();

            try {
                socket.connect();
            } catch (final IOException e) {
                e.getMessage();
            }

            c = null;
            if ( ct != null )
            {
                ct.cancel();

            }
            ct = new ConnectedThread(socket);
            ct.start();

        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            StringBuilder readMessage = new StringBuilder();
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    String read = new String(mmBuffer, 0, numBytes);
                    readMessage.append(read);

                    // маркер конца команды - вернуть ответ в главный поток
                    mHandler.obtainMessage(MainActivity.MESSAGE_READ, numBytes, -1, readMessage.toString()).sendToTarget();
                    readMessage.setLength(0);
                    
                    // Send the obtained bytes to the UI activity.
               /* Message readMsg = mHandler.obtainMessage(
                        MessageConstants.MESSAGE_READ, numBytes, -1,
                        mmBuffer);
                readMsg.sendToTarget();*/
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                mmOutStream.flush();
                // Share the sent message with the UI activity.
           /* Message writtenMsg = mHandler.obtainMessage(
                    MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
            writtenMsg.sendToTarget();*/
                //toast("sent the data");
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
           /* Message writeErrorMsg =
                    mHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString("toast",
                    "Couldn't send data to the other device");
            writeErrorMsg.setData(bundle);
            mHandler.sendMessage(writeErrorMsg);*/
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
               // if (D) Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class BluetoothResponseHandler extends Handler {
        private WeakReference<MainActivity> mActivity;

        public BluetoothResponseHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        public void setTarget(MainActivity target) {
            mActivity.clear();
            mActivity = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what)
                {
                    case MESSAGE_READ:
                        String readMessage = (String) msg.obj;
                        if (readMessage != null) {
                            activity.textview.setText(readMessage);
                            try {
                                writeDataToTextFile(readMessage);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                }
            }
        }

    }
    private void writeDataToTextFile(String readMessage) throws IOException {

        //series.appendData(new DataPoint(lastX++, Double.parseDouble(readMessage)), true,10000);
        Log.d("Data", readMessage);
        int count = StringUtils.countMatches(readMessage, ".");
        if (count <= 1){
            float d = Float.parseFloat(readMessage);
            series.appendData(new DataPoint(lastX++, d), true, 1000);
            Log.d("Data", String.valueOf(d));
            fw = new FileWriter(file, true);
            fw.write(readMessage + "\n");
            fw.close();
    }
//        if(d<16520000) {
//            series.appendData(new DataPoint(lastX++, (float)d), true, 5000);
//            Log.d("point", String.valueOf(d));
//            fw = new FileWriter(file, true);
//            fw.write(readMessage + "\n");
//            fw.close();
//        }
    }



}