package com.idbarcodesolutions.mainactivity.fragments;


import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.idbarcodesolutions.mainactivity.R;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerConfig;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.StatusData;

import java.util.ArrayList;

public class ScanFragment extends Fragment implements EMDKManager.EMDKListener, Scanner.DataListener, Scanner.StatusListener {
    // Store EMDKManager object
    private EMDKManager emdkManager = null;

    // Store Barcode Manager object
    private BarcodeManager barcodeManager = null;

    // Scanner device to scan
    private Scanner scanner = null;

    // UI elements
    private TextView statusTextView = null;
    private EditText dataView = null;

    private boolean startRead;

    private int dataLength = 0;

    private Context context;


    public ScanFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_scan, container, false);
        // Reference to UI elements
        bindUI(view);
        //this.startRead = false;

        // Se crea el objeto EMDKManager
        EMDKResults results = EMDKManager.getEMDKManager(
                context,
                this
        );

        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            statusTextView.setText("EMDKManager Request Failed");
        }
        return view;
    }

    private void bindUI(View view) {
        statusTextView = view.findViewById(R.id.textViewStatusFragment);
        dataView = view.findViewById(R.id.editTextDataFragment);
    }

    public void initializeScanner() throws ScannerException {
        if (scanner == null) {
            // Get the Barcode Manager object


            barcodeManager = (BarcodeManager) this.emdkManager
                    .getInstance(EMDKManager.FEATURE_TYPE.BARCODE);
            // Get default scanner defined on the device
            scanner = barcodeManager.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT);

            // Add data and status listeners
            scanner.addDataListener(this);
            scanner.addStatusListener(this);
            // Hard trigger. When this mode is set, the user has to manually
            // press the trigger on the device after issuing the read call.
            scanner.triggerType = Scanner.TriggerType.HARD;
            // Enable the scanner
            scanner.enable();
            scanner.read();
            //set startRead flag to true. this flag will be used in the OnStatus callback to insure
            //the scanner is at an IDLE state and a read is not pending before calling scanner.read()
            // this.startRead = true;
        }
    }

    private void deInitializeScanner() throws ScannerException {
        if (scanner != null) {
            try {
                if (scanner.isReadPending()) {
                    scanner.cancelRead();
                }
                scanner.disable();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                scanner.removeDataListener(this);
                scanner.removeStatusListener(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                scanner.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            scanner = null;
        }
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {
        this.emdkManager = emdkManager;

        try {
            // Call this method to enable Scanner and its listeners
            initializeScanner();
        } catch (ScannerException e) {
            e.printStackTrace();
        }

        try {
            ScannerConfig scannerConfig = scanner.getConfig();
            scannerConfig.scanParams.decodeLEDTime = 800;
            scanner.setConfig(scannerConfig);
        } catch (ScannerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClosed() {
        if (this.emdkManager != null) {
            this.emdkManager.release();
            this.emdkManager = null;
        }
    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {
        new AsyncDataUpdate().execute(scanDataCollection);
    }

    @Override
    public void onStatus(StatusData statusData) {
        new AsyncStatusUpdate().execute(statusData);
    }

    private class AsyncDataUpdate extends AsyncTask<ScanDataCollection, Void, String> {


        @Override
        protected String doInBackground(ScanDataCollection... scanDataCollections) {

            // Status string that contains both barcode data and type of barcode
            // that is being scanned
            String statusStr = "";

            try {
                // Starts an asynchronous Scan. The method will not turn ON the
                // scanner. It will, however, put the scanner in a state in
                // which
                // the scanner can be turned ON either by pressing a hardware
                // trigger or can be turned ON automatically.
                scanner.read();

                ScanDataCollection scanDataCollection = scanDataCollections[0];

                // The ScanDataCollection object gives scanning result and the
                // collection of ScanData. So check the data and its status
                if (scanDataCollection != null && scanDataCollection.getResult() == ScannerResults.SUCCESS) {

                    ArrayList<ScanDataCollection.ScanData> scanData = scanDataCollection
                            .getScanData();

                    // Iterate through scanned data and prepare the statusStr
                    for (ScanDataCollection.ScanData data : scanData) {
                        // Get the scanned data
                        // Get the type of label being scanned
                        //ScanDataCollection.LabelType labelType = data.getLabelType();
                        // Concatenate barcode data and label type
                        statusStr = data.getData();
                    }
                }

            } catch (ScannerException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return result to populate on UI thread
            return statusStr;
        }


        @Override
        protected void onPostExecute(String result) {
            // Update the dataView EditText on UI thread with barcode data and
            // its label type
            if (!result.isEmpty()) {
                if (dataLength++ > 50) {
                    // Clear the cache after 50 scans
                    dataView.getText().clear();
                    dataLength = 0;
                }
                dataView.setText(result);
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    private class AsyncStatusUpdate extends AsyncTask<StatusData, Void, String> {

        @Override
        protected String doInBackground(StatusData... statusData) {
            String statusStr = "";
            // Get the current state of scanner in background
            StatusData statusData1 = statusData[0];
            StatusData.ScannerStates state = statusData1.getState();
            // Different states of scanner
            switch (state) {
                case IDLE:
                    try {
                        scanner.read();
                    } catch (ScannerException e) {
                        e.printStackTrace();
                    }
                    statusStr = "Scanner enabled and idle";
                    break;
                case WAITING:
                    try {
                        scanner.read();
                    } catch (ScannerException e) {
                        e.printStackTrace();
                    }
                    statusStr = "Waiting for trigger press";
                    break;
                case ERROR:
                    statusStr = "There is an error!!!!";
                    break;
                case DISABLED:
                    statusStr = "Scanner is disabled";
                    break;
                case SCANNING:
                    statusStr = "Scanning...";
                    break;
            }
            return statusStr;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            statusTextView.setText(result);
        }


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (emdkManager != null) {
            // Clean up the objects created by EMDK manager
            emdkManager.release();
            emdkManager = null;
        }
        try {
            deInitializeScanner();
        } catch (ScannerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            if (scanner != null) {
                // releases the scanner hardware resources for other application
                // to use. You must call this as soon as you're done with the
                // scanning.
                scanner.removeDataListener(this);
                scanner.removeStatusListener(this);
                scanner.disable();
                scanner = null;
            }
        } catch (ScannerException e) {
            e.printStackTrace();
        }
    }
}