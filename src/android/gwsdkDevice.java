package com.heytz.gwsdkDevice;


import android.content.Context;
import android.util.Base64;
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

import java.util.Iterator;
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
            JSONObject arr=new JSONObject(value.toString());
            //创建JSONObject 对象，用于封装所有数据
             JSONObject jsonsend = new JSONObject();
            //写入命令字段（所有产品一致）
            jsonsend.put("cmd", 1);
            //jsonsend.put("aciton", 1);
            //创建JSONObject 对象，用于封装数据点
            JSONObject jsonparam = new JSONObject();
            //写入数据点字段
//            jsonparam.put(key, value);
            Iterator it = arr.keys();
            while (it.hasNext()) {
                String jsonKey=(String) it.next();
                String jsonValue=arr.getString(jsonKey);
                jsonparam.put(jsonKey, getData(jsonValue));
            }
//            jsonparam.put("command", getData(arr.getString("command")));
//            jsonparam.put("mac",  getData(arr.getString("mac")));
//            jsonparam.put("control",  getData(arr.getString("control")));
//            jsonparam.put("percent",  getData(arr.getString("percent")));
//            jsonparam.put("angle",  getData(arr.getString("angle")));
            //写入产品字段（所有产品一致）
            jsonsend.put("entity0", jsonparam );
            //{"entity0":"{\"command\":\"0009\",\"control\":\"02\",\"mac\":\"000000008d418d12\",\"percent\":\"00\",\"angle\":\"00\"}","cmd":1}
            // 0x00 0x01 0x02 0x03 0x04 0x05 0x06 0x07 0x08 0x09 0x0a 0x0b 0x0c 0x0d 0x0e 0x0f
            // 0x00 0x01 0x02 0x03 0x04 0x05 0x06 0x07 0x08 0x09 0x0a 0x0b 0x0c 0x0d 0x0e 0x0f
            //调用发送指令方法
            xpgWifiDevice.write(jsonsend.toString());
            airLinkCallbackContext.success("success");
        } catch (JSONException e) {
            e.printStackTrace();
            airLinkCallbackContext.error("error");
        }
    }
    public static String getData(String str) {
        return new String(Base64.encode(StringToBytes(str), Base64.NO_WRAP));
    }
    public static byte[] StringToBytes(String paramString) {
        byte[] arrayOfByte = new byte[paramString.length() / 2];
        for (int i = 0;; i += 2) {
            if (i >= paramString.length())
                return arrayOfByte;
            String str = paramString.substring(i, i + 2);
            arrayOfByte[(i / 2)] = ((byte) Integer.valueOf(str, 16).intValue());
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
                           //普通数据点类型，有布尔型、整形和枚举型数据，该种类型一般为可读写
                                                    if (dataMap.get("data") != null){
                                                        Log.i("info", (String)dataMap.get("data"));

                                                    }
                                                    //设备报警数据点类型，该种数据点只读，设备发生报警后该字段有内容，没有发生报警则没内容
                                                    if (dataMap.get("alters") != null){
                                                        Log.i("info", (String)dataMap.get("alters"));

                                                    }
                                                    //设备错误数据点类型，该种数据点只读，设备发生错误后该字段有内容，没有发生报警则没内容
                                                    if (dataMap.get("faults") != null){
                                                        Log.i("info", (String)dataMap.get("faults"));

                                                    }
                                                    //二进制数据点类型，适合开发者自行解析二进制数据
                                                    if (dataMap.get("binary") != null){
                                                        Log.i("info", "Binary data:");
                                                        //收到后自行解析
                                                    }
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