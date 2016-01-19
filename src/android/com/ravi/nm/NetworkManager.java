// Raviraj Kakade


package com.ravi.nm;

import org.apache.cordova.*;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.SupplicantState;
import android.content.Context;
import android.util.Log;


public class NetworkManager extends CordovaPlugin {
    private static final String Wifi_Lists = "getWifiList";
    private static final String ConfigNewWifi = "configNewWifi";
    private static final String Connection_Info = "getConnectionInfo";
    private WifiManager wifiManager;
    private static final String TAG = "NetworkManager";
    private CallbackContext callbackContext;

        @Override
        public void initialize(CordovaInterface cordova, CordovaWebView webView) {
            super.initialize(cordova, webView);
            this.wifiManager = (WifiManager) cordova.getActivity().getSystemService(Context.WIFI_SERVICE);
        }

        @Override
        public boolean execute(String action, JSONArray data, CallbackContext callbackContext)
                                throws JSONException {

            this.callbackContext = callbackContext;

            if(action.equals(Wifi_Lists)) {
                return this.getWifiDetails(callbackContext,data);
            } else if(action.equals(ConfigNewWifi)){
                return this.configNewWifiNetwork(callbackContext,data);
            } else if(action.equals(Connection_Info)){
                return this.getConnectionInfo(callbackContext,data);
            } else {
                callbackContext.error("Incorrect action parameter: " + action);
            }

            return false;
        }

        /***
        *    getWifiDetails - return all lists
        **/
        private boolean getWifiDetails(CallbackContext callbackContext, JSONArray data) {
                List<ScanResult> scanResults = wifiManager.getScanResults();
                WifiInfo wifiInfo = wifiManager.getConnectionInfo ();
                String ssid = wifiInfo.getSSID();
                ssid = ssid.replaceAll("^\"|\"$", "");
                JSONArray wifiLists = new JSONArray();
                for (ScanResult scan : scanResults) {
                    JSONObject obj = new JSONObject();
                    try {
                        String ssid_replaced = scan.SSID.replaceAll("^\"|\"$", "");
                        if(ssid.equals(ssid_replaced)){
                          obj.put("isConnected", true);
                        } else {
                          obj.put("isConnected", false);
                        }
                        obj.put("SSID", ssid_replaced);
                        obj.put("NEWSSID", ssid);
                        obj.put("BSSID", scan.BSSID);
                        obj.put("frequency", scan.frequency);
                        obj.put("capabilities", scan.capabilities);
                        wifiLists.put(obj);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callbackContext.error(e.toString());
                        return false;
                    }
                }

                 callbackContext.success(wifiLists);
                 return true;
        }

        /***
        *    getConnectionInfo - return Connection info
        **/
        private boolean getConnectionInfo(CallbackContext callbackContext, JSONArray data) {
          WifiInfo wifiInfo = wifiManager.getConnectionInfo ();
           String ssid = wifiInfo.getSSID();
           callbackContext.success(ssid);
           return true;
       }

        /***
        * Config New Wifi
        ***/
       private boolean configNewWifiNetwork(CallbackContext callbackContext, JSONArray data) {
         try {

            String ssid = data.getString(0);
            String security = data.getString(1);
            String pass = data.getString(2);

            // setup a wifi configuration
            WifiConfiguration wc = new WifiConfiguration();

            // basic settings
            wc.SSID = "\"".concat(ssid).concat("\"");
            wc.status = WifiConfiguration.Status.DISABLED;
            wc.priority = 40;

            if(security.equals("WPA") || security.equals("WPA2")){
              Log.d(TAG, " Security Type is WPA/WPA2");

              wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
              wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
              wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
              wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
              wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
              wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
              wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
              wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
              wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

              wc.preSharedKey = "\"".concat(pass).concat("\"");

               // connect wifi
              int networkId = wifiManager.addNetwork(wc);
              if (networkId != -1) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(networkId, true);
                wifiManager.reconnect();
                Log.d("TAG", "SSID " + ssid + " Security " + security   );
                callbackContext.success(ssid +  " added successfully ");
               } else {
                wifiManager.updateNetwork(wc);
                callbackContext.success(ssid +  " updated successfully ");
              }
            } else if(security.equals("WEP")){
              Log.d(TAG, " Security Type is WEP");

              wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
              wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
              wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
              wc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
              wc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
              wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
              wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
              wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
              wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

              wc.wepKeys[0] = "\"" + pass + "\""; ;
              wc.wepTxKeyIndex = 0;

               // connect wifi
              int networkId = wifiManager.addNetwork(wc);
              if (networkId != -1) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(networkId, true);
                wifiManager.reconnect();
                Log.d("TAG", "SSID " + ssid + " Security " + security   );
                callbackContext.success(ssid +  " added successfully ");
              } else {
                wifiManager.updateNetwork(wc);
                callbackContext.success(ssid + " updated successfully");
              }
            } else if(security.equals("ESS")){
              Log.d(TAG, " Security Type is ESS Open Wifi ");
              // open wifi
              wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
              wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
              wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
              wc.allowedAuthAlgorithms.clear();
              wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
              wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
              wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
              wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
              wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
              wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

               // connect wifi
              int networkId = wifiManager.addNetwork(wc);
              if (networkId != -1) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(networkId, true);
                wifiManager.reconnect();
                Log.d("TAG", "SSID " + ssid + " Security " + security   );
                callbackContext.success(ssid +  " added successfully ");
               } else {
                wifiManager.updateNetwork(wc);
                callbackContext.success(ssid +  " updated successfully ");
              }
            }else {
              Log.d(TAG, "Security Type Not Supported.");
              callbackContext.error("Security Type Not Supported.: " + security);
              return false;
            }



              if(wc.networkId == -1 ){
              // connect to and enable the connection
                int netId = wifiManager.addNetwork(wc);
                wifiManager.disconnect();
                wifiManager.enableNetwork(netId, true);
                wifiManager.reconnect();
                callbackContext.success(ssid + " " + security + " added successfully ");
              } else {
                wifiManager.updateNetwork(wc);
                callbackContext.success(ssid + " " + security + " updated successfully");
              }
              wifiManager.saveConfiguration();
              return true;
         } catch (Exception e) {
            callbackContext.error(e.getMessage());
            return false;
          }
       }

}

