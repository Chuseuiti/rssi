package biominder.com.rssi;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import android.view.View.OnClickListener;


/*
* Data Collection Mobile Application
*
* Jesus Cardenes 30/11/2016
*
* The objective of this mobile application is to capture the RSSI data from all the available devices
* around the phone. It will gather the data every 5 min and it will record the RSSI data in one file
* per device
*
*
* */

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 1;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    Button buttonScan, buttonStop;
    ListView devicesListView;

    ArrayList<Tuple> listDevices=new ArrayList<Tuple>();

    //Class with the structure defining the relevant information of the ble scan devices
    class Tuple{
        private String name;
        private String MAC;
        private int RSSI;

        Tuple(String name_t,String MAC_t,int RSSI_t){
            name=name_t;
            MAC=MAC_t;
            RSSI=RSSI_t;
        }

        //It is needed to override the functionality of toString for this class because
        //the ArrayAdapter will directly try to convert to a String the Object to populate it on the list
        @Override
        public String toString(){

            return "Name: "+this.name+"\nMAC: "+this.MAC+"\nRSSI: "+String.valueOf(this.RSSI);

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        devicesListView = (ListView)this.findViewById(R.id.devicesListView);

        mHandler = new Handler();
        Toast.makeText(this, "Welcome",Toast.LENGTH_SHORT).show();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //I have to verify the SDK Level over API 18, it uses the new vesion of the BLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }


        //This button allows the system to rescan and in the future it could
        // be change for a timer and the system will automatically be able to look for the data

        addListenerOnButton();

    }

    public void addListenerOnButton() {

        buttonScan = (Button) findViewById(R.id.button1);

        buttonScan.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                //Here I have to rescan for BLE devices
                mBluetoothAdapter.startLeScan(mLeScanCallback);

            }

        });

        buttonStop = (Button) findViewById(R.id.button2);

        buttonStop.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                //Here I have to rescan for BLE devices
                mBluetoothAdapter.stopLeScan(mLeScanCallback);

            }

        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<ScanFilter>();

            }
            //scanLeDevice(true);

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        }
                    } else {
                       // mLEScanner.stopScan(mScanCallback);


                    }
                }
            }, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT < 21) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    mBluetoothAdapter.startLeScan(mLeScanCallback);
                }
            } else {
                //mLEScanner.startScan(filters, settings, mScanCallback);
            }
        } else {
            //Once the
            if (Build.VERSION.SDK_INT < 21) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            } else {
                //mLEScanner.stopScan(mScanCallback);
            }
        }
    }

/*
//This version is for devices over API 21, however it doesn't scan as it should, it is needed to do some debugging to eliminated the conflict with the previous
// android API, estimation of time
    @SuppressLint("NewApi")
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)

        public void onScanResult(int callbackType, ScanResult result) {

            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            //BluetoothDevice
            String Name_devive=result.getDevice().getName();
            String RSSI= String.valueOf(result.getRssi());
            Log.i("RSSI_Device_Surrounding", "Name Device:" + Name_devive +"and RSSI: "+ RSSI );

            //connectToDevice(btDevice);
        }


        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };*/

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            //Verify if the name of the BLE transmitter is defined in the BLE package
                            String ble_device_info="";

                            if (device.getName() != null) {

                                Log.i("onLeScan", "Device: " + device.getName().toString() + ", MAC" + device.toString() + " and RSSI: " + (rssi));

                                //Format the data of the scan device
                                Tuple ble_device=new Tuple(device.getName(),device.toString(),rssi);
                            } else {

                                Log.i("onLeScan", "MAC: " + device.toString() + " and RSSI: " + (rssi));

                                //Format the data of the scan device in the case the name of the device is unkown
                                Tuple ble_device=new Tuple("unknown_name",device.toString(),rssi);

                            }

                            //Write RSSI data into the multiple files with the name of the devices
                            final String FILES = "/BioMinder";

                            String path= Environment.getExternalStorageDirectory().getPath()+FILES; //Folder path

                            File folderFile = new File(path);
                            if (!folderFile.exists()) {
                                folderFile.mkdirs();
                            }

                            File myFile = new File(folderFile, device.toString()+".txt");
                            try {
                                if (!myFile.exists()) {
                                    myFile.createNewFile();
                                }
                                FileOutputStream fOut = new FileOutputStream(myFile,true);

                                OutputStreamWriter bw = new OutputStreamWriter(fOut);

                                String value=System.currentTimeMillis()+ ","+ Integer.toString(rssi)+ " \n";
                                bw.append(value);

                                bw.close();

                                fOut.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                            //List devices and their RSSI value in the UI to allow the visualization to the user
                            //verify is value is in list before adding it to the list

                            Tuple ble_device=new Tuple(device.getName(),device.toString(),rssi);

                            //This logic verifies is the value has been previously recorded
                            boolean contains=false;
                            int num=0;

                            //Verify is the device has been previously recorded
                            for (Tuple i:listDevices) {

                                if (i.MAC.equals(device.toString())) {
                                    contains = true;
                                    break;
                                }
                                num=num+1;
                            }
                            if(contains==false) {
                                //Records for the first time device
                                ble_device = new Tuple(device.getName(), device.toString(), rssi);
                                listDevices.add(ble_device);

                            }else{
                                //Update RSSI value of BLE device
                                listDevices.set(num,ble_device);

                            }


                            //List all the Ble devices up to that moment
                            final ArrayAdapter<Tuple> adapter = new ArrayAdapter<Tuple>(MainActivity.this,
                                    android.R.layout.simple_list_item_1, listDevices);
                            devicesListView.setAdapter(adapter);
                        }
                    });
                }
            };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);// will stop after first device detection
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            gatt.readCharacteristic(services.get(1).getCharacteristics().get
                    (0));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
            gatt.disconnect();
        }
    };
}
