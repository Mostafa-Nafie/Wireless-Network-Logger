package com.example.wirelessnetworklogger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter bluetoothAdapter;
    ArrayAdapter<String> bluetoothArrayAdapter;
    ArrayList<String> bluetoothArrayList = new ArrayList<String>();
    ListView bluetoothListView;
    Button scanBtn;

    WifiManager wifiManager;
    ArrayAdapter<String> wifiArrayAdapter;
    ArrayList<String> wifiArrayList = new ArrayList<String>();
    ListView wifiListView;
    List<ScanResult> results;


    boolean finishedWifiScan = false;
    final int REQUEST_PERMISSION = 50;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanBtn = (Button) findViewById(R.id.scanBtn);
        bluetoothListView = (ListView) findViewById(R.id.bluetoothListView);
        wifiListView = (ListView) findViewById(R.id.wifiListView);

        bluetoothArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, bluetoothArrayList);
        bluetoothListView.setAdapter(bluetoothArrayAdapter);

        wifiArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, wifiArrayList);
        wifiListView.setAdapter(wifiArrayAdapter);


        //check permission and request it
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                + ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION);
        }


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null)
        {
            Toast.makeText(this, "Bluetooth not available !", Toast.LENGTH_LONG).show();
            finish();
        }

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        scanBtn.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        startScanning();
                    }
                }
        );
    }


    //if user doesn't give permission, exit the program
    public void onRequestPermissionsResult(int requestCode, String permission[], int[]grantResults){
        switch (requestCode){
            case REQUEST_PERMISSION:{
                if(grantResults.length > 0 && grantResults[0]+grantResults[1]+grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        //do nothing
                    }
                }else{
                    Toast.makeText(this, "Please give the app the required permissions!!!", Toast.LENGTH_LONG);
                    finish();
                    System.exit(0);
                }
                return;
            }
        }
    }

    private void writeToFile(String data, String fileName){
        try {
            File file = new File(this.getExternalFilesDir(null), fileName+".txt");
            FileOutputStream fileOutput = new FileOutputStream(file);
            OutputStreamWriter os = new OutputStreamWriter(fileOutput);
            os.write(data);
            os.flush();
            os.close();
            Toast.makeText(this, "Data is saved at: " + fileName + ".txt", Toast.LENGTH_LONG).show();
        }
        catch (IOException e){
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private void startScanning()
    {
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, intentFilter);

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastReceiver, intentFilter);


        intentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(broadcastReceiver, intentFilter);


        finishedWifiScan = false;
        scanBtn.setEnabled(false);
        scanBtn.setText("Scanning...");

        bluetoothArrayAdapter.clear();
        bluetoothAdapter.startDiscovery();

        wifiArrayList.clear();
        wifiManager.startScan();

    }

    private void stopScanning()
    {
        unregisterReceiver(broadcastReceiver);
        scanBtn.setEnabled(true);
        scanBtn.setText("Scan");
    }


    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action){

                //If wifi finished scanning, put the results in the list
                case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                {
                    results = wifiManager.getScanResults();
                    if (results.size() == 0){
                        wifiArrayList.add("No APs were found !");
                        wifiArrayAdapter.notifyDataSetChanged();
                    }
                    for(ScanResult scanResult: results){
                        String wifiSSID = scanResult.SSID;
                        String wifiRSSI = String.valueOf(scanResult.level);
                        String element = "AP name: " + wifiSSID +"\nStrength: " + wifiRSSI;
                        wifiArrayList.add(element);
                        wifiArrayAdapter.notifyDataSetChanged();
                    }
                    finishedWifiScan = true;
                    break;
                }

                //If a new bluetooth device is found add it to the list
                case BluetoothDevice.ACTION_FOUND:
                {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String macAddress = device.getAddress();
                    String element = "Device: " + deviceName + "\nMAC Address: " + macAddress;
                    bluetoothArrayList.add(element);
                    bluetoothArrayAdapter.notifyDataSetChanged();
                    break;
                }

                //If the bluetooth scanning is done, check if there is not any found devices
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                {
                    if (bluetoothArrayAdapter.getCount() == 0) {
                        bluetoothArrayList.add("No Devices were discovered !");
                        bluetoothArrayAdapter.notifyDataSetChanged();
                    }

                    //if the wifi scanning is done as well, then stop the scanning and save the file
                    if(finishedWifiScan)
                    {
                        stopScanning();
                        SimpleDateFormat df = new SimpleDateFormat("yyMMdd-HHmmss");
                        writeToFile("APs found:\n"+wifiArrayList.toString()+"\nBluetooth Devices found:\n"+bluetoothArrayList.toString(),
                                df.format(new Date())+"_Wireless log file");
                    }
                    break;
                }
            }

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}