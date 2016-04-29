package com.ionicframework.myapp480696;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by zhumd on 2016/1/27.
 */
public class DynamicUpdateActivity extends Activity {

    private String local_address = null;

    private static final String remote_address = "http://bbs.aoshitang.com/static/bbsapp/and_ver/";

    private String local_version_file = null;

    private static final String remote_version_file = remote_address + "version.json?" + System.currentTimeMillis();

    private static final String remote_zip = remote_address + "www.zip";

    private String local_zip = null;

    private static final float versionCode = 1.0f;

    ProgressDialog progressBar = null;

    AlertDialog.Builder builder = null;

    private String launchUrl = "file:///android_asset/www/index.html";

    private ExecutorService executor = Executors.newFixedThreadPool(5);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        local_address = getFilesDir().getAbsolutePath();
        local_version_file = local_address + "/version.json";
        local_zip = local_address + "/www.zip";
//        setContentView(R.layout.dyna_update);
        builder = new AlertDialog.Builder(DynamicUpdateActivity.this);
        new Thread() {
            public void run() {
                try {
                    copyWWW();
                } catch (Exception e) {
                    Log.e("dynamic update", e.getMessage());
                }

                try {
                    doUpdate();
                } catch (
                        Exception e
                        ) {
                    Log.e("dynamic update", e.getMessage());
                    nextActivity();
                }
            }

            private void copyWWW() throws Exception {
                File localVersionFile = new File(local_version_file);
                Float local_version = null;
                a:
                {
                    if (!localVersionFile.exists()) break a;
                    String s = new BufferedReader(new InputStreamReader(new FileInputStream(local_version_file), "utf-8")).readLine();
                    local_version = (float) new JSONObject(s).getDouble("version");
                }
                if (local_version == null || local_version < versionCode) {
                    unzip(new ZipInputStream(getClass().getResourceAsStream("/assets/www.zip")), new File(local_address));
                    PrintWriter pw = new PrintWriter(local_version_file);
                    pw.println("{\"version\":" + versionCode + "}");
                    pw.close();
                }
            }
        }.start();
    }

    public void doUpdate() throws Exception {
        Float local_version = null;
        Float remote_version;
        final File local_version_file = new File(this.local_version_file);
        a:
        {
            if (!local_version_file.exists()) break a;
            String s = new BufferedReader(new InputStreamReader(new FileInputStream(local_version_file), "utf-8")).readLine();
            local_version = (float) new JSONObject(s).getDouble("version");
        }

        if (!isNetworkConnected()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final Toast toast = Toast.makeText(DynamicUpdateActivity.this, "请检查网络连接，应用暂时不可用!", Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
            nextActivity();
            return;
        }
        try {
            HttpGet httpGet = new HttpGet(remote_version_file);
            HttpClient httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 2000);//连接时间
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 2000);//数据传输时间
            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                String s = EntityUtils.toString(entity);
                remote_version = (float) new JSONObject(s).getDouble("version");
            } else {
                remote_version = null;
            }
        } catch (Exception e) {
            Log.e("get remote version!", e.getMessage());
            remote_version = null;
        }
        if (remote_version != null) {
            if ((local_version != null && remote_version > local_version) || (local_version == null && remote_version > versionCode)) {
                //reset directory
//                    if(new File(local_address).getParentFile().exists()) FileUtils.deleteDirectory(new File(local_address).getParentFile());
//                    View layout_dyna_update = View.inflate(this, R.layout.dyna_update, null);
//                    progerssBar = (ProgressBar) layout_dyna_update.findViewById(R.id.progressBar);
                final float remote_version2 = remote_version;
                if (isNetworkConnected() && getNetworkType() != NETTYPE_WIFI) {
                    builder.setMessage("非wifi网络，确认更新吗？");
                    builder.setTitle("有更新");
                    builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, int which) {
                            dialog.dismiss();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    netUpdate(remote_version2);
                                    nextActivity();
                                }
                            }).start();
                        }
                    });
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, int which) {
                            dialog.dismiss();
                            nextActivity();
                        }
                    });
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            builder.create().show();
                        }
                    });
                    return;
                } else {
                    netUpdate(remote_version);
                }
            } else if (local_version != null && local_version >= versionCode) {
                launchUrl = "file://" + local_address + "/www/index.html";
            }
        } else {
            if (local_version != null && local_version >= versionCode) {
                launchUrl = "file://" + local_address + "/www/index.html";
            }

        }
        nextActivity();
    }

    private void nextActivity() {
        Intent intent = new Intent();
        intent.putExtra("launchUrl", launchUrl);
        intent.setClass(DynamicUpdateActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void netUpdate(float remote_version) {
        final String json = "{\"version\":" + remote_version + "}";
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            HttpGet httpGet2 = new HttpGet(remote_zip);
            HttpClient httpClient2 = new DefaultHttpClient();
            httpClient2.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);//连接时间
            httpClient2.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 5000);//数据传输时间
            HttpResponse resposne = httpClient2.execute(httpGet2);
            HttpEntity entity = resposne.getEntity();
            is = entity.getContent();
//                    fos = openFileOutput("www.zip", Context.MODE_PRIVATE);
            fos = new FileOutputStream(local_zip);
            byte[] buffer = new byte[1024];
            int read = -1;
            final int size = (int) entity.getContentLength();
            int sum = 0;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar = new ProgressDialog(DynamicUpdateActivity.this);
                    progressBar.setMessage("正在更新中，请稍后....");
                    progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressBar.setProgress(0);
                    progressBar.setCancelable(false);
                    progressBar.setMax(size);
                    progressBar.show();
                }
            });
            while ((read = is.read(buffer)) > 0) {
                fos.write(buffer, 0, read);
                final Message message = new Message();
                message.obj = (sum += read);
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    progressBar.setProgress(100);
//                                }
//                            });
                if (progressBar != null) progressBar.setProgress((Integer) message.obj);
            }
            File local_zip_file = new File(local_zip);
            unzip(new ZipInputStream(new FileInputStream(local_zip_file)), new File(local_address));

            PrintWriter pw = new PrintWriter(local_version_file);
            pw.println(json);
            pw.close();

            if (progressBar != null) progressBar.setProgress(100);
            if (progressBar != null) progressBar.dismiss();
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//
//                            }
//                        });
            launchUrl = "file://" + local_address + "/www/index.html";
        } catch (Exception e) {
            Log.e("download zip error", e.getMessage());
        } finally {
            try {
                if (is != null) is.close();
                if (fos != null) fos.close();
            } catch (IOException e) {
            }
        }
    }

    public void unzip(ZipInputStream zip, File directory) throws Exception {
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            File file = new File(local_address + File.separator + entry.getName());
            if (entry.isDirectory()) {
                file.mkdirs();
//                try {
//                    Process p = Runtime.getRuntime().exec("chmod 777 " + file);
//                    p.waitFor();
//                    p = Runtime.getRuntime().exec("chmod 777 " + file.getParentFile());
//                    p.waitFor();
//                } catch (IOException e1) {
//                } catch (InterruptedException e) {
//                }
                unzip(zip, directory);
            } else {
                int read = -1;
                byte buffer[] = new byte[1024];
//                Process p = Runtime.getRuntime().exec("chmod 777 " + file);
//                p.waitFor();
                FileOutputStream fos = new FileOutputStream(file, false);
                while ((read = zip.read(buffer)) > 0) {
                    fos.write(buffer, 0, read);
                }
                fos.close();
            }
            zip.closeEntry();
        }
    }


    /**
     * 检测网络是否可用
     *
     * @return
     */
    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting();
    }

    /**
     * 获取当前网络类型
     *
     * @return 0：没有网络   1：WIFI网络   2：WAP网络    3：NET网络
     */

    public static final int NETTYPE_WIFI = 0x01;
    public static final int NETTYPE_CMWAP = 0x02;
    public static final int NETTYPE_CMNET = 0x03;

    public int getNetworkType() {
        int netType = 0;
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return netType;
        }
        int nType = networkInfo.getType();
        if (nType == ConnectivityManager.TYPE_MOBILE) {
            String extraInfo = networkInfo.getExtraInfo();
            if (!(extraInfo == null || extraInfo.equals(""))) {
                if (extraInfo.toLowerCase().equals("cmnet")) {
                    netType = NETTYPE_CMNET;
                } else {
                    netType = NETTYPE_CMWAP;
                }
            }
        } else if (nType == ConnectivityManager.TYPE_WIFI) {
            netType = NETTYPE_WIFI;
        }
        return netType;
    }


}

