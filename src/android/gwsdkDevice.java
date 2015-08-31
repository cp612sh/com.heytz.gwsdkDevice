package com.heytz.gwsdkDevice;


import android.content.Context;
import com.xtremeprog.xpgconnect.XPGWifiDevice;
import com.xtremeprog.xpgconnect.XPGWifiDeviceListener;
import com.xtremeprog.xpgconnect.XPGWifiSDK;
import com.xtremeprog.xpgconnect.XPGWifiSDKListener;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;


/**
 * This class wrapping Gizwits WifiSDK called from JavaScript.
 */
public class gwsdkDevice extends CordovaPlugin {

    private CallbackContext airLinkCallbackContext;
       private Context context;


       @Override
       public void initialize(CordovaInterface cordova, CordovaWebView webView) {
           super.initialize(cordova, webView);
           // your init code here
           context = cordova.getActivity().getApplicationContext();
       }

       @Override
       public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
           airLinkCallbackContext = callbackContext;
           if (action.equals("deviceControl")) {
               /**
                * uid, token, mac, key, value
                */
               this.getBoundDevice(args.getString(0), args.getString(1));
               this.deviceLogin(args.getString(0), args.getString(1), args.getString(2), args.getString(3), args.getString(4));
               return true;
           }

           return false;
       }


       List<XPGWifiDevice> xpgWifiDeviceList;

       private void getBoundDevice(String uid, String token, String... specialPoductKeys) {
           XPGWifiSDK.sharedInstance().setListener(new XPGWifiSDKListener() {
               @Override
               public void didDiscovered(int error, List<XPGWifiDevice> deviceList) {
                   if (deviceList.size() > 0)
                       xpgWifiDeviceList = deviceList;
   //                else
                   //
               }

           });
           XPGWifiSDK.sharedInstance().getBoundDevices(uid, token, specialPoductKeys);
       }

       private void cWrite(XPGWifiDevice xpgWifiDevice, String key, Object value) {
           try {
               //创建JSONObject 对象，用于封装所有数据
               final JSONObject jsonsend = new JSONObject();
               //写入命令字段（所有产品一致）
               jsonsend.put("cmd", 1);
               //创建JSONObject 对象，用于封装数据点
               JSONObject jsonparam = new JSONObject();
               //写入数据点字段
               jsonparam.put(key, value);
               //写入产品字段（所有产品一致）
               jsonsend.put("entity0", jsonparam);
               //调用发送指令方法
               xpgWifiDevice.write(jsonsend.toString());
               airLinkCallbackContext.success("success");
           } catch (JSONException e) {
               e.printStackTrace();
               airLinkCallbackContext.error("error");
           }
       }

       public void deviceLogin(String uid, String token, String mac, String key, String value) {

           XPGWifiDevice d = null;
           for (int i = 0; i < xpgWifiDeviceList.size(); i++) {
               XPGWifiDevice device = xpgWifiDeviceList.get(i);
               if (device != null) {
                   if (device != null && device.getMacAddress().equals(mac)) {
                       d = device;
                       break;
                   }
               }
           }
           (d).login(uid, token);

           final XPGWifiDevice finalD = d;
                     final String finalKey = key;
                     final String finalValue  = value;
                     d.setListener(new XPGWifiDeviceListener() {
                         @Override
                         public void didLogin(XPGWifiDevice device, int result) {
                             cWrite(finalD, finalKey, finalValue);
                         }

               @Override
               public void didDeviceOnline(XPGWifiDevice device, boolean isOnline) {
               }

               @Override
               public void didDisconnected(XPGWifiDevice device) {
               }

               @Override
               public void didReceiveData(XPGWifiDevice device, java.util.concurrent.ConcurrentHashMap<String, Object> dataMap, int result) {
               }

               @Override
               public void didQueryHardwareInfo(XPGWifiDevice device, int result, java.util.concurrent.ConcurrentHashMap<String, String> hardwareInfo) {
               }
           });
       }
}