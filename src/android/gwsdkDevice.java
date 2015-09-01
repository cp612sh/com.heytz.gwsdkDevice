package com.heytz.gwsdkDevice;


import android.content.Context;
import android.util.Log;
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

    private boolean controlState;//锁定用户的控制状态
    private List<XPGWifiDevice> xpgWifiDeviceList;
    private String _appId, _productKey, _uid, _token, _mac, _key;
    private Object _value;
    private final String TAG = "==========gwsdkDevice==============";


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        // your init code here
        context = cordova.getActivity().getApplicationContext();
    }

    /**
     *
     * @param action          The action to execute.
     * @param args            The exec() arguments.{ appid,productKey,uid,token,mac,key,value   }
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return
     * @throws JSONException
     */
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        airLinkCallbackContext = callbackContext;
        if (action.equals("deviceControl")) {

            if (_appId == null) {
                _appId = args.getString(0);
                _productKey = args.getString(1);
                init();
            }
            _uid = args.getString(2);
            _token = args.getString(3);
            _mac = args.getString(4);
            _key = args.getString(5);
            _value = args.getString(6);
            this.getBoundDevice(_uid, _token, _productKey);

            return true;
        }

        return false;
    }

    /**
     *搜索设备 第一步
     * @param uid
     * @param token
     * @param specialPoductKeys
     * @throws JSONException
     */
    private void getBoundDevice(String uid, String token, String... specialPoductKeys) throws JSONException {
        controlState = true;
        XPGWifiSDK.sharedInstance().getBoundDevices(uid, token, specialPoductKeys);
    }

    /**
     * 发送控制命令的方法  第三步
     * @param xpgWifiDevice
     * @param key
     * @param value
     */
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

    /**
     * 如果device没有登录那么登录，发送控制命令到cWrite 第二步
     *
     * @param uid
     * @param token
     * @param mac
     * @param key
     * @param value
     */
    private void deviceLogin(String uid, String token, String mac, String key, Object value) {

        XPGWifiDevice d = null;
        for (int i = 0; i < xpgWifiDeviceList.size(); i++) {
            XPGWifiDevice device = xpgWifiDeviceList.get(i);
            if (device != null) {
                Log.w(TAG, device.getMacAddress());
                if (device != null && device.getMacAddress().equals(mac.toUpperCase())) {
                    d = device;
                    break;
                }
            }
        }

        if (d != null && controlState == true) {
            controlState = false;
            if (!d.isConnected()) {
                (d).login(uid, token);

                d.setListener(new XPGWifiDeviceListener() {
                    @Override
                    public void didLogin(XPGWifiDevice device, int result) {
                        cWrite(device, _key, _value);
                    }

                    @Override
                    public void didDeviceOnline(XPGWifiDevice device, boolean isOnline) {
                    }

                    @Override
                    public void didDisconnected(XPGWifiDevice device) {
                        Log.d(TAG, "did disconnected...");
                    }

                    @Override
                    public void didReceiveData(XPGWifiDevice device, java.util.concurrent.ConcurrentHashMap<String, Object> dataMap, int result) {
                        //回调
                    }

                    @Override
                    public void didQueryHardwareInfo(XPGWifiDevice device, int result, java.util.concurrent.ConcurrentHashMap<String, String> hardwareInfo) {
                    }
                });
            } else {
                cWrite(d, key, value);
            }

        }
    }

    /**
     * 要初始化监听的listener
     * 如果是第一次加载 那么初始化设置 第一次加载的判断为 是否存在_appId
     */
    private void init() throws JSONException {
        XPGWifiSDK.sharedInstance().startWithAppID(context, _appId);
        XPGWifiSDK.sharedInstance().setLogLevel(XPGWifiSDK.XPGWifiLogLevel.XPGWifiLogLevelAll, "dbugLog.log", true);
        XPGWifiSDK.sharedInstance().setListener(new XPGWifiSDKListener() {
            @Override
            public void didDiscovered(int error, List<XPGWifiDevice> deviceList) {
                if (error == 0 && deviceList.size() > 0) {
                    xpgWifiDeviceList = deviceList;
                    deviceLogin(_uid, _token, _mac, _key, _value);
                    Log.d(TAG, "deviceLoing()");
                }
            }

        });
    }
}