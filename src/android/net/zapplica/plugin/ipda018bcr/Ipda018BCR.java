/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

/**
 * read barcodes from Ipda018 series with build in barcode scanner
 */
package net.zapplica.plugin.ipda018bcr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.device.ScanDevice;
import android.util.Log;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author francis.appels@yahoo.com
 *
 */
public class Ipda018BCR extends CordovaPlugin {

    // Debugging
    private static final String TAG = "Ipda018BCR";
    private static final boolean D = false;

    // Intent request codes
    private static final String ACTION_FEEDBACK = "scan.rcv.message";

    // BCR states
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0; // we're doing nothing
    public static final int STATE_READING = 1; // reading BCR reader
    public static final int STATE_READ = 2; // read received BCR reader
    public static final int STATE_ERROR = 3; // Error
    public static final int STATE_DESTROYED = 4; // BCR reader destroyed
    public static final int STATE_READY = 5; // we're ready
    private int mState;

    // BCR actions
    private static final String ACTION_INIT = "init";
    private static final String ACTION_DESTROY = "destroy";
    private static final String ACTION_READ = "read";
    private static final String ACTION_GETSTATE = "getState";

    // Local BCR adapter
    private BCRBroadcastReceiver mCodeScanReceiver = null;
    private boolean bCodeScanReceiverRegistered = false;
    private ScanDevice sm;

    // Member fields
    private JSONObject szComData;

    /**
     * Create a BCR reader
     */
    public Ipda018BCR() {
        this.setState(STATE_NONE);
    }

    /**
     * Sets the context of the Command. This can then be used to do things like get
     * file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The associated CordovaWebView.
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    public void onDestroy() {
        this.setState(STATE_DESTROYED);
        if ((mCodeScanReceiver != null) && this.bCodeScanReceiverRegistered) {
            this.cordova.getActivity().unregisterReceiver(mCodeScanReceiver);
        }
        if (D) Log.d(TAG, "Destroyed");
        super.onDestroy();
    }

    /**
     * Execute supported functions
     */
    @Override
    public boolean execute(
        String action,
        JSONArray args,
        final CallbackContext callbackContext
    )
        throws JSONException {
        if (D) Log.d(TAG, "Action: " + action);

        if (ACTION_INIT.equals(action)) {
            sm = new ScanDevice();
            // init BCR receiver
            mCodeScanReceiver = new BCRBroadcastReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_FEEDBACK);

            cordova.getActivity().registerReceiver(mCodeScanReceiver, filter);

            if (!sm.isScanOpened()) {
                sm.openScan();
            }
            sm.setOutScanMode(0);
            if (sm.getOutScanMode() == 0) {
                this.bCodeScanReceiverRegistered = true;
                this.setState(STATE_READY);
                callbackContext.success();
            } else {
                this.setState(STATE_ERROR);
                callbackContext.error("Init Failed");
            }
        } else if (ACTION_DESTROY.equals(action)) {
            if ((mCodeScanReceiver != null)) {
                this.cordova.getActivity()
                    .unregisterReceiver(mCodeScanReceiver);
                this.bCodeScanReceiverRegistered = false;
                if (sm.isScanOpened()) {
                    sm.closeScan();
                }
            }
            callbackContext.success();
            this.onDestroy();
        } else if (ACTION_READ.equals(action) && (mState != STATE_READING)) {
            this.setState(STATE_READING);

            if (D) Log.d(TAG, "Reading...");

            cordova
                .getThreadPool()
                .execute(
                    new Runnable() {
                        public void run() {
                            while (true) {
                                if (mState == STATE_READ) {
                                    try {
                                        PluginResult result = new PluginResult(
                                            PluginResult.Status.OK,
                                            szComData
                                        );
                                        result.setKeepCallback(true);
                                        callbackContext.sendPluginResult(
                                            result
                                        );
                                        mState = STATE_READING;
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        mState = STATE_ERROR;
                                        callbackContext.error(e.getMessage());
                                        break;
                                    }
                                } else if (
                                    (mState == STATE_DESTROYED) ||
                                    (mState == STATE_ERROR)
                                ) {
                                    callbackContext.error("Not Read");
                                    break;
                                }
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    mState = STATE_ERROR;
                                    callbackContext.error(e.getMessage());
                                    break;
                                }
                            }
                        }
                    }
                );
        } else if (ACTION_GETSTATE.equals(action)) {
            JSONObject stateJSON = new JSONObject();
            try {
                stateJSON.put("state", mState);
                callbackContext.success(stateJSON);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
                this.setState(STATE_ERROR);
                callbackContext.error(e.getMessage());
            }
        } else {
            callbackContext.error(
                "Action '" + action + "' not supported (now) state = " + mState
            );
        }

        return true;
    }

    private void setState(int state) {
        this.mState = state;
    }

    // The BroadcastReceiver that listens BCR feedback and trigger
    private class BCRBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(ACTION_FEEDBACK)) {
                try {
                    szComData = getScanResult(intent);
                    mState = STATE_READ;
                } catch (Exception e) {
                    Log.e(
                        TAG,
                        "BroadcastReceiver.onReceive: Exception occured:" +
                        e.getMessage()
                    );
                    mState = STATE_ERROR;
                }
            }
        }
    }

    private JSONObject getScanResult(Intent initiatingIntent) {
        byte[] barcode = initiatingIntent.getByteArrayExtra("barocode");
        int barcodeLen = initiatingIntent.getIntExtra("length", 0);
        byte temp = initiatingIntent.getByteExtra("barcodeType", (byte) 0);
        String result = new String(barcode, 0, barcodeLen);

        JSONObject obj = new JSONObject();
        try {
            obj.put("text", result);
            obj.put("format", Byte.toString(temp));
            if (D) Log.d(TAG, "Read result = " + szComData.get("text"));
        } catch (Exception e) {
            Log.e(TAG, "getScanResult: Exception occured:" + e.getMessage());
        }
        if (D) Log.d(TAG, "getScanResult returned!");
        return obj;
    }
}
