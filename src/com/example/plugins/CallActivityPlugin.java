package com.example.plugins;



import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Intent;
import android.net.Uri;

public class CallActivityPlugin extends CordovaPlugin {
    public static final String ACTION = "call";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals(ACTION)) {
            try {
                //下面两句最关键，利用intent启动新的Activity
//    			String url = "http://bbs.aoshitang.com/download.htm?id=19"; // web address
    			String url = args.getString(0);
    			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                this.cordova.startActivityForResult(this, intent, 1);
//    			startActivity(intent);
                //下面三句为cordova插件回调页面的逻辑代码
                PluginResult mPlugin = new PluginResult(PluginResult.Status.NO_RESULT);
                mPlugin.setKeepCallback(true);
                callbackContext.sendPluginResult(mPlugin);
                callbackContext.success("success");
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }
}
