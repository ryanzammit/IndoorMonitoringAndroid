package se.miun.com.indoormonitoring.ui.activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;

import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;

import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import se.miun.com.indoormonitoring.R;
import se.miun.com.indoormonitoring.util.RBLService;

public class MainActivity extends BaseActivity {

    private static final int REQUEST_ENABLE_BT = 1337;
    private static final int MY_PERMISSIONS_REQUEST_BLUETOOTH = 999;
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 888;
    private static final int MY_PERMISSIONS_REQUEST_COARSE_LOCATION = 777;
    private static final long SCAN_PERIOD = 10000;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;
    private RBLService mBluetoothLeService;
    private Boolean isStringStarted = false;

    private StringBuilder jsonStringValues = new StringBuilder();

    private Map<UUID, BluetoothGattCharacteristic> map = new HashMap<>();
    private Boolean mIsDevice = false;
    private String jsonStartChar = "[";
    private String jsonEndChar = "]";

    ImageView mTemperatureImage;
    TextView mTemperatureValue;

    ImageView   humidityImage;
    TextView humidityValue;

    ImageView pressureImage;
    TextView pressureValue;

    ImageView concentrationGasesImage;
    TextView concentrationGasesValue;

    ImageView combustibleGasesImage;
    TextView combustibleGasesValue;

    ImageView airQualityImage;
    TextView airQualityValue;


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


//        mTemperatureImage = (ImageView) findViewById(R.id.temperature_image);
        mTemperatureValue = (TextView) findViewById(R.id.temperature_value);

//        humidityImage = (ImageView) findViewById(R.id.humidity_image);
        humidityValue = (TextView) findViewById(R.id.humidity_value);

//        pressureImage = (ImageView) findViewById(R.id.pressure_image);
        pressureValue = (TextView) findViewById(R.id.pressure_value);

//        concentrationGasesImage = (ImageView) findViewById(R.id.concentration_gases_image);
        concentrationGasesValue = (TextView) findViewById(R.id.concentration_gases_value);

//        combustibleGasesImage = (ImageView) findViewById(R.id.combustible_gases_image);
        combustibleGasesValue = (TextView) findViewById(R.id.combustible_gases_value);

//        airQualityImage = (ImageView) findViewById(R.id.air_quality_image);
        airQualityValue = (TextView) findViewById(R.id.air_quality_value);

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
            String data = null;
//            try {
//                data = new String(byteArray, "UTF-8");
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
//            Log.d("FFF", "Data: " + data);
            data = "[44.34|19.13|101839.";
            String data2 = "14|0.166016|0.561523";
            String data3 = "|0.375977]";

            if (data.startsWith(jsonStartChar) && !isStringStarted) {
                Log.d("FFF", data);
                //append
                jsonStringValues.append(data);
                isStringStarted = true;
            }
            else if (isStringStarted) {

                if(data.contains(jsonEndChar)){
                    jsonStringValues.append(data);

                    String temperature = jsonStringValues.substring(jsonStringValues.indexOf(jsonStartChar) + 1, jsonStringValues.indexOf("|"));
                    jsonStringValues.replace(0, temperature.length() + 2, "");
//                Log.d("FFF", "Json values - temp: " + jsonStringValues.toString());

                    String humidity = jsonStringValues.substring(0, jsonStringValues.indexOf("|"));
                    jsonStringValues.replace(0, humidity.length() + 1, "");
//                Log.d("FFF", "Json values- humidity: " + jsonStringValues.toString());

                    String pressure = jsonStringValues.substring(0, jsonStringValues.indexOf("|"));
                    jsonStringValues.replace(0, pressure.length() + 1, "");

                    String concentrationGases = jsonStringValues.substring(0, jsonStringValues.indexOf("|"));
                    jsonStringValues.replace(0, concentrationGases.length() + 1, "");

                    String combustibleGases = jsonStringValues.substring(0, jsonStringValues.indexOf("|"));
                    jsonStringValues.replace(0, combustibleGases.length() + 1, "");

                    String airQuality = jsonStringValues.substring(0, jsonStringValues.indexOf(jsonEndChar));


                    Log.d("FFF", "Temp: " + temperature + " / " + humidity + " / " + pressure + " / " + concentrationGases + " / " + combustibleGases + " / " + airQuality);

                    List<String> arduinoValues = new ArrayList<>();
                    arduinoValues.add(0,temperature);
                    arduinoValues.add(1,humidity);
                    arduinoValues.add(2,pressure);
                    arduinoValues.add(3,concentrationGases);
                    arduinoValues.add(4,combustibleGases);
                    arduinoValues.add(5,airQuality);

                    updateViews(arduinoValues);

                    //RESET JSON STRING


                    isStringStarted = false;
                    jsonStringValues.setLength(0);
                }else{
                    jsonStringValues.append(data);
                }




            }

        }
    }

    private void updateViews(List<String> arduinoData) {
        findViewById(R.id.sensors_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.progressBar).setVisibility(View.GONE);
        mTemperatureValue.setText("Temperature: " + arduinoData.get(0));
        humidityValue.setText("Humidity: " + arduinoData.get(1));
        pressureValue.setText("Pressure: " + arduinoData.get(2));
        concentrationGasesValue.setText("Concentration Gases: " + arduinoData.get(3));
        combustibleGasesValue.setText("Combustible Gases: " + arduinoData.get(4));
        airQualityValue.setText("Air Quality: " + arduinoData.get(5));
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
//                            Log.d("FFF", "Address: " + device.getAddress());

                            if (arduinoAddress.equals(device.getAddress()) && !mIsDevice) {
                                mDevice = device;
                                mIsDevice = true;

                                Intent gattServiceIntent = new Intent(getBaseContext(), RBLService.class);
                                bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                                registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

                            }
                        }
                    });
                }
            };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((RBLService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("FFF", "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up
            // initialization.
            mBluetoothLeService.connect(mDevice.getAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


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

}
