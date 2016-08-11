package se.miun.com.indoormonitoring.ui.activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import se.miun.com.indoormonitoring.R;

public class MainActivity extends BaseActivity {

    private static final int REQUEST_ENABLE_BT = 1337;
    private static final int MY_PERMISSIONS_REQUEST_BLUETOOTH = 999;
    private BluetoothAdapter mBluetoothAdapter = null;


    @Override
    protected void onCreateWithLayout(@Nullable Bundle savedInstanceState, @LayoutRes int layout) {
        super.onCreateWithLayout(savedInstanceState, R.layout.activity_generic_toolbar);

        setToolbarTitle("Home");

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE NOT SUPPORTED!", Toast.LENGTH_SHORT).show();
            finish();
        }

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH);

        if(permissionCheck!= PackageManager.PERMISSION_GRANTED){
            askBluetoothPermission();
        }

        //TODO bluetooth compatibility

        //TODO make connection with Arduino

        if (mBluetoothAdapter == null)
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        //TODO add logic for if user does not turn Bluetooth on
        connectToArduino();

    }

    private void connectToArduino() {

        //TODO

    }

    private void askBluetoothPermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.BLUETOOTH)) {

        } else {

            // No explanation needed, we can request the permission.

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH},
                    MY_PERMISSIONS_REQUEST_BLUETOOTH);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        }
    }
}
