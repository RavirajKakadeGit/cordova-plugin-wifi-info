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
import android.net.DhcpInfo;
import android.util.Log;
import java.net.InetAddress;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.io.InputStream;
import java.io.BufferedReader;
import java.lang.StringBuilder;
import android.net.TrafficStats;


import java.util.Properties;
import com.jcraft.jsch.*;
import java.io.*;
import android.os.Bundle;
import android.app.Activity;
import android.content.ContentResolver;


public class NetworkManager extends CordovaPlugin {
    private static final String Wifi_Lists = "getWifiList";
    private static final String Wifi_Configured_Networks_Lists = "getConfiguredNetworks";
    private static final String ConfigNewWifi = "configNewWifi";
    private static final String Connection_Info = "getConnectionInfo";
    private static final String GetDHCPInfo = "getDHCPInfo";
	private static final String GetWifiUsage = "getWifiUsage";
    private static final String ConnectSSH = "connectSSH";
    private static final String WIFI_SLEEP_POLICY = "setWifiSleepPolicy";
    private static final String changeDirectory = "changeDirectory";
    private WifiManager wifiManager;
    private static final String TAG = "NetworkManager";
    private CallbackContext callbackContext;
    private static Context context;
	private static final String SYSTEM_REBOOT_SHUTDOWN = "The system is going down for ";
	private long mStartRX = 0;
	private long mStartTX = 0;

        @Override
        public void initialize(CordovaInterface cordova, CordovaWebView webView) {
            super.initialize(cordova, webView);
            this.wifiManager = (WifiManager) cordova.getActivity().getSystemService(Context.WIFI_SERVICE);
            context =  cordova.getActivity().getApplicationContext();

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
            } else if(action.equals(Wifi_Configured_Networks_Lists)){
                return this.getConfiguredNetworks(callbackContext,data);
            } else if(action.equals(ConnectSSH)){
                return this.connectSSH(callbackContext,data);
            } else if(action.equals(GetDHCPInfo)){
               return this.getDHCPInfo(callbackContext,data);
            } else if(action.equals(GetWifiUsage)){
               return this.getWifiUsage(callbackContext,data);
            }else if(action.equals(WIFI_SLEEP_POLICY)){
               return this.setWifiSleepPolicy(callbackContext,data);
            } else {
               callbackContext.error("Incorrect action parameter: " + action);
            }

            return false;
        }

		private boolean getWifiUsage(CallbackContext callbackContext, JSONArray data){
		 try{
			StringBuilder stringBuilder = new StringBuilder();
			long rxBytes = (TrafficStats.getTotalRxBytes()- mStartRX)/1024/1024;			
			stringBuilder.append(Long.toString(rxBytes));
			callbackContext.success(stringBuilder.toString());
				return true;
		 }catch(Exception e){
                e.printStackTrace();
                callbackContext.error(e.toString());
                return false;
            }
	 } 
	 
        private boolean connectSSH(CallbackContext callbackContext, JSONArray data) {
        try{
            String username = data.getString(0);
            String password = data.getString(1);
            String hostname = data.getString(2);
			String command = data.getString(3);
			int flag = 0;


                JSch jsch = new JSch();
                Session session = jsch.getSession(username, hostname, 22);
                session.setPassword(password);

                // Avoid asking for key confirmation
                Properties prop = new Properties();
                prop.put("StrictHostKeyChecking", "no");
                session.setConfig(prop);
                session.connect();
				
                // SSH Channel
                ChannelExec channelssh = (ChannelExec) session.openChannel("exec");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                channelssh.setOutputStream(baos);
				
                // Execute command
				StringBuilder stringBuilder = new StringBuilder();
				if(command != null && !command.isEmpty()){
					if(command.startsWith("sudo"))
					{
						channelssh.setPty(true);
						channelssh.setCommand(command);
						 OutputStream out=channelssh.getOutputStream();
						 channelssh.connect();						 
						 out.write((password+"\n").getBytes());
						 out.flush();
																	
					}else{
                channelssh.setCommand(command);
                channelssh.connect();
				}
				if(username.equals("root") && (command.startsWith("reboot") || command.startsWith("shutdown"))){
						stringBuilder.append(SYSTEM_REBOOT_SHUTDOWN+command);
				}else if(!(username.equals("root")) && (command.startsWith("reboot") || command.startsWith("shutdown"))){
					    stringBuilder.append(command+":Need to be root or use sudo reboot/shutdown password");
				}
                InputStream inputStream = channelssh.getInputStream();

				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

				String line;

				while ((line = bufferedReader.readLine()) != null)
				{
					if ((command.startsWith("su") || command.startsWith("sudo")) && flag == 0)
					{	
						flag = 1;
						continue;
					}
                   stringBuilder.append(line);
                   stringBuilder.append('\n');

				} 
				flag = 0;				
			}
				//Disconnect Channel & Session
				channelssh.disconnect();
				session.disconnect();
				
				callbackContext.success(stringBuilder.toString());
				return true;
            } catch(Exception e){
                e.printStackTrace();
                callbackContext.error(e.toString());
                return false;
            }
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
                        obj.put("level" , wifiManager.calculateSignalLevel(scan.level, 5) );
                        obj.put("SSID", ssid_replaced);
                        obj.put("BSSID", scan.BSSID);
                        obj.put("macAddress", wifiInfo.getMacAddress());
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
                *    getConfiguredNetworks - return Config wifi lists
                **/
                private boolean getConfiguredNetworks(CallbackContext callbackContext, JSONArray data) {
                        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();

                        JSONObject returnObj = new JSONObject();

                        JSONArray wifiLists = new JSONArray();
                        for( WifiConfiguration i : list ) {
                            JSONObject obj = new JSONObject();
                            try {
                                obj.put("SSID", i.SSID);
                                obj.put("BSSID", i.BSSID);
                                obj.put("networkId", i.networkId);
                                obj.put("algorithm", i.allowedAuthAlgorithms);
                                obj.put("keyMgt", i.allowedKeyManagement);
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
        *    getDHCPInfo - return Connection info
        **/
        private boolean getDHCPInfo(CallbackContext callbackContext, JSONArray data) {
             try {
                DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
                int IpAddress = dhcpInfo.ipAddress;
                int dns1 = dhcpInfo.dns1;
                int dns2 = dhcpInfo.dns2;
                int gateway = dhcpInfo.gateway;
                int leaseDuration = dhcpInfo.leaseDuration;
                int netmask  = dhcpInfo.netmask;
                int serverAddress = dhcpInfo.serverAddress;

                IpAddress = (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) ?
                                Integer.reverseBytes(IpAddress) : IpAddress;
                dns1 = (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) ?
                                Integer.reverseBytes(dns1) : dns1;
                dns2 = (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) ?
                                Integer.reverseBytes(dns2) : dns2;
                gateway = (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) ?
                                Integer.reverseBytes(gateway) : gateway;
                serverAddress = (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) ?
                                Integer.reverseBytes(serverAddress) : serverAddress;

               byte[] bytes = BigInteger.valueOf(IpAddress).toByteArray();
               InetAddress ip = InetAddress.getByAddress(bytes);

               byte[] bytes1 = BigInteger.valueOf(dns1).toByteArray();
               InetAddress dn1 = InetAddress.getByAddress(bytes1);

               byte[] bytes2 = BigInteger.valueOf(dns2).toByteArray();
               InetAddress dn2 = InetAddress.getByAddress(bytes2);

               byte[] bytes3 = BigInteger.valueOf(gateway).toByteArray();
               InetAddress gateway1 = InetAddress.getByAddress(bytes3);

               byte[] bytes5 = BigInteger.valueOf(netmask).toByteArray();
               InetAddress netmask1 = InetAddress.getByAddress(bytes5);

                byte[] bytes6 = BigInteger.valueOf(serverAddress).toByteArray();
                InetAddress serverAddress1 = InetAddress.getByAddress(bytes6);

                JSONObject returnObj = new JSONObject();
                returnObj.put("IpAddress", ip.getHostAddress().toString());
                returnObj.put("dns1", dn1.getHostAddress());
                returnObj.put("dns2", dn2.getHostAddress());
                returnObj.put("gateway", gateway1.getHostAddress());
                returnObj.put("leaseDuration", leaseDuration);
                returnObj.put("netmask", netmask1.getHostAddress());
                returnObj.put("serverAddress", serverAddress1.getHostAddress());
                callbackContext.success(returnObj);
                return true;
             } catch (Exception e) {
               callbackContext.error(e.getMessage());
               return false;
             }
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

       private boolean setWifiSleepPolicy(CallbackContext callbackContext, JSONArray data) {
            try {
                String policyName = data.getString(0);
                if( policyName.equals("Never") ){
                    ContentResolver cr = context.getContentResolver();
                    int set = android.provider.Settings.System.WIFI_SLEEP_POLICY_NEVER;
                    android.provider.Settings.System.putInt(cr, android.provider.Settings.System.WIFI_SLEEP_POLICY, set);
                } else if( policyName.equals("Always")){
                    ContentResolver cr = context.getContentResolver();
                    int set = android.provider.Settings.System.WIFI_SLEEP_POLICY_DEFAULT;
                    android.provider.Settings.System.putInt(cr, android.provider.Settings.System.WIFI_SLEEP_POLICY, set);
                } else {
                    ContentResolver cr = context.getContentResolver();
                    int set = android.provider.Settings.System.WIFI_SLEEP_POLICY_NEVER_WHILE_PLUGGED;
                    android.provider.Settings.System.putInt(cr, android.provider.Settings.System.WIFI_SLEEP_POLICY, set);
                }
                callbackContext.success(policyName);
                return true;
            } catch (Exception e) {
                callbackContext.error(e.getMessage());
                return false;
            }
       }

}

