package se.miun.com.indoormonitoring.ui.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import se.miun.com.indoormonitoring.R;
import se.miun.com.indoormonitoring.model.SensorObject;
import se.miun.com.indoormonitoring.ui.adapters.SensorDataAdapter;
import se.miun.com.indoormonitoring.util.BluetoothConnector;
import se.miun.com.indoormonitoring.util.RBLService;

public class MainActivity extends BaseActivity {

    private static final int REQUEST_ENABLE_BT = 1337;
    private static final int MY_PERMISSIONS_REQUEST_BLUETOOTH = 999;
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 888;
    private static final int MY_PERMISSIONS_REQUEST_COARSE_LOCATION = 777;
    private BluetoothAdapter mBluetoothAdapter;

    private boolean mScanning;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private Boolean mIsCorrectJson = false;
    private Boolean mIsJsonDone = false;
    private StringBuilder mJsonString = new StringBuilder();

    private SensorDataAdapter mAdapter;
    private RecyclerView mRecycler;
    private LinearLayoutManager mLayoutManager;
    private List<String> jsonValues = new ArrayList<>();
    private BluetoothGatt mBluetoothGatt;
    private BluetoothDevice mDevice;
    private ConnectThread mConnectThread;
    private RBLService mBluetoothLeService;

    public static List<BluetoothDevice> mDevices = new ArrayList<>();
    private Map<UUID, BluetoothGattCharacteristic> map = new HashMap<>();
    private Boolean mIsDevice = false;


    @Override
    protected void onCreateWithLayout(@Nullable Bundle savedInstanceState, @LayoutRes int layout) {
        super.onCreateWithLayout(savedInstanceState, R.layout.activity_generic_toolbar);


        setToolbarTitle("Home");


        //Check every single permission
        int permissionBluetooth = ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH);
        int permissionCoarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int permissionFineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);


        if (permissionBluetooth != PackageManager.PERMISSION_GRANTED)
            askPermission(MY_PERMISSIONS_REQUEST_BLUETOOTH);

        if (permissionCoarseLocation != PackageManager.PERMISSION_GRANTED)
            askPermission(MY_PERMISSIONS_REQUEST_COARSE_LOCATION);

        if (permissionFineLocation != PackageManager.PERMISSION_GRANTED)
            askPermission(MY_PERMISSIONS_REQUEST_FINE_LOCATION);


        //VERSION 2 START {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "BLE NOT SUPPORTED!", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


        if (mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "BT ENABLED!", Toast.LENGTH_SHORT).show();
            scanLeDevice();
        }
        //TODO add logic for if user does not turn Bluetooth on

        Intent gattServiceIntent = new Intent(this, RBLService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void scanLeDevice() {
        new Thread() {

            @Override
            public void run() {
                mBluetoothAdapter.startLeScan(mLeScanCallback);


                try {
                    Thread.sleep(SCAN_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }.start();
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d("FFF", "Disconnected");
            } else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                getGattService(mBluetoothLeService.getSupportedGattService());
            } else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getByteArrayExtra(RBLService.EXTRA_DATA));
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);

        return intentFilter;
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mBluetoothLeService.disconnect();
        mBluetoothLeService.close();

        System.exit(0);
    }

    private void getGattService(BluetoothGattService gattService) {
        if (gattService == null)
            return;

        BluetoothGattCharacteristic characteristic = gattService
                .getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);
        map.put(characteristic.getUuid(), characteristic);

        BluetoothGattCharacteristic characteristicRx = gattService
                .getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
        mBluetoothLeService.setCharacteristicNotification(characteristicRx,
                true);
    }

    private void displayData(byte[] byteArray) {
        if (byteArray != null) {
            String data = new String(byteArray);
            Log.d("FFF", "Data: " + data);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String arduinoAddress = "C4:A1:CD:BE:0F:5E";
                            //If device address is equal to address string, pair device
                            Log.d("FFF", "Address: " + device.getAddress());

                            if (arduinoAddress.equals(device.getAddress()) && !mIsDevice) {
                                mDevice = device;
                                mIsDevice = true;



//                                mBluetoothGatt = mDevice.connectGatt(getBaseContext(), false, mGattCallback);
//
//
//                                BluetoothGattService bluetoothGattService = mBluetoothGatt.getService(UUID.fromString("00001110-0000-1000-8000-00805f9b34fb"));
//                                BluetoothGattCharacteristic bluetoothCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString("00000002-0000-1000-8000-00805f9b34fb"));


                            }
                        }
                    });
                }
            };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((RBLService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("FFF", "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up
            // initialization.
            mBluetoothLeService.connect(mDevice.getAddress());
            Log.d("FFF", "CONNECT BT");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void eddyStoneResult(ScanResult result) {
        String arduinoName = "test";
        String uuid = "0000FEAA-0000-1000-8000-00805F9B34FB";

        //TODO Compare Address with hardcoded address
        //TODO Pair with Arduino
        //TODO Read out characteristics
        Log.d("FFF", "Devices found: " + result.getDevice().getName());

        // We use the url in the eddystone to get the json string
        // We defined a start character to indicate the start of the json
        // And also an end character to end the json

        String jsonStartChar = "";
        String jsonEndChar = "";

        if (arduinoName.equals(result.getDevice().getName())) {
            byte[] rawData = result
                    .getScanRecord()
                    .getServiceData(ParcelUuid.fromString(uuid));

            String url = "";

            try {
                String tempUrl = new String(rawData, "ASCII");
                url = tempUrl.substring(2, tempUrl.length());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            if (url.startsWith(jsonStartChar)) {
                //todo add to list
            }

            if (url.startsWith(jsonStartChar)) {
                mIsCorrectJson = true;
                mJsonString.append(url);
            } else if (url.endsWith(jsonEndChar)) {
                mIsCorrectJson = false;
                mJsonString.append(url);

                mIsJsonDone = true;
            } else if (mIsCorrectJson) {
                mJsonString.append(url);
            }
            if (mIsJsonDone) {
                Log.d("FFF", "The final JsonString: " + mJsonString.toString());
//                        Gson gson = new Gson();
//                        SensorObject sensorObject = gson.fromJson(mJsonString.toString(), SensorObject.class);
//                        fillRecyclerView(sensorObject);


            }
        }
    }

    public void pairResult(ScanResult result) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        mConnectThread = new ConnectThread(mDevice);
        mConnectThread.start();
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void scanLeDeviceLollipop() {
        List<ScanFilter> scanFilters = new ArrayList<>();
        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                Log.d("FFF", "status: " + status + " | newState " + newState);
                Log.d("FFF", "Gatt: " + gatt.getDevice().getName());


            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                byte[] data = characteristic.getValue();

                try {
                    String test = new String(data, "UTF-8");
                    Log.d("FFF", "Data: " + test);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }


            }


        };

        ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                // Eddystone scan
//                eddyStoneResult(result);

                // Pair scan

                if (mDevice == null) {

                    String arduinoAddress = "C4:A1:CD:BE:0F:5E";


                    //If device address is equal to address string, pair device
                    Log.d("FFF", "Address: " + result.getDevice().getAddress());
                    if (arduinoAddress.equals(result.getDevice().getAddress())) {
                        mDevice = result.getDevice();

                        mBluetoothGatt = mDevice.connectGatt(getBaseContext(), false, mGattCallback);
                        BluetoothGattCharacteristic car = new BluetoothGattCharacteristic(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"), 0, 0);
//                        mBluetoothGatt.readCharacteristic(car);
                        Log.d("FFF", "Boolean: " + mBluetoothGatt.readCharacteristic(car));
                        Log.d("FFF", "Result: " + result.toString());
                        byte[] data = result.getScanRecord().getBytes();

                        String test = new String(data);
                        Log.d("FFF", "Test: " + test);

                    }
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        };

        bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback);

    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mSocket;
        private final BluetoothDevice mDevice;
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        private BluetoothSocket fallbackSocket;

        public ConnectThread(BluetoothDevice device) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            BluetoothSocket tmp = null;
            mDevice = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);


                Class<?> clazz = tmp.getRemoteDevice().getClass();
                Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};

                Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                Object[] params = new Object[]{Integer.valueOf(1)};

                fallbackSocket = (BluetoothSocket) m.invoke(tmp.getRemoteDevice(), params);

            } catch (IOException e) {
                Log.d("EEE", e.getMessage());
            }

            mSocket = tmp;
        }

        public void run() {
            mBluetoothAdapter.cancelDiscovery();
            BluetoothConnector bluetoothConnector = new BluetoothConnector(mDevice, false, mBluetoothAdapter, null);
            try {
                bluetoothConnector.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private void setJsonValues(String values) {
        String tempValue = values;
        if (values.startsWith("§")) {

        }

    }


    private String createJson() {
        return "{ \"version\": \"" + jsonValues.get(0) + "\", " +
                "\"sensors\": { \"temperature\": \"" + jsonValues.get(1) + "\"," +
                "\"humidity\": \"" + jsonValues.get(2) + "\", " +
                "\"pressure\": \"" + jsonValues.get(3) + "\", " +
                "\"concentrationGases\": \"" + jsonValues.get(4) + "\", " +
                "\"combustibleGases\": \"" + jsonValues.get(5) + "\", " +
                "\"airQuality\": \"" + jsonValues.get(6) + "\" }}";
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void scanLeDeviceKitKat(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private void askPermission(int permissionRequest) {

        String permission = "";
        switch (permissionRequest) {
            case MY_PERMISSIONS_REQUEST_BLUETOOTH:
                permission = Manifest.permission.BLUETOOTH;
                break;
            case MY_PERMISSIONS_REQUEST_COARSE_LOCATION:
                permission = Manifest.permission.ACCESS_COARSE_LOCATION;
                break;
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION:
                permission = Manifest.permission.ACCESS_FINE_LOCATION;
                break;
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                permission)) {

        } else {

            // No explanation needed, we can request the permission.

            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    permissionRequest);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        }
    }

    private void fillRecyclerView(SensorObject object) {
        if (findViewById(R.id.recycler_view) != null) {

            mAdapter = new SensorDataAdapter(this, object);
            mRecycler = (RecyclerView) findViewById(R.id.recycler_view);
            mLayoutManager = new LinearLayoutManager(this);

            mRecycler.setLayoutManager(mLayoutManager);
            mRecycler.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();


        }
    }
}
