package com.powerall.plugin.updateapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import com.phonegap.leho.R; 

public class UpdateAppPlugin extends CordovaPlugin {

 private final String TAG = "UpdateAppPlugin";
	/*版本号检查路径*/
	private String checkPath;
	/* 新版本号*/
	private int newVerCode;
	/* 新版本名称 */
	private String newVerName;
	/* APK 下载路径*/
	private String  downloadPath;
	/* APK 名称*/
	private String  apkName;
	/*更新明细*/
	private String updateInfo;
    /* 上下文*/
    private Context mContext;
    /* 更新进度条 */
    private ProgressBar mProgress;
    private Dialog mDownloadDialog;
    
    private SharedPreferences mPrefs;  
    private DownloadManager mDownloadManager;
    
    private HttpURLConnection conn;
    
    private static final String DL_ID = "downloadId";  
	@Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.mContext = cordova.getActivity();
        android.util.Log.d(TAG,"action=>"+action);
        
		if (action.equals("checkAndUpdate")) {
			callbackContext.success();
			this.checkPath = args.getString(0);
			android.util.Log.d(TAG,"checkPath=>"+checkPath);
			checkAndUpdate();
    }else if(action.equals("check")) {
			callbackContext.success();
			this.checkPath = args.getString(0);
			android.util.Log.d(TAG,"checkPath=>"+checkPath);
			check();
    }
        return false;
    }

	 private BroadcastReceiver receiver = new BroadcastReceiver() {   
	        @Override   
	        public void onReceive(Context context, Intent intent) {   
	            Log.v("intent", ""+intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0));  
	            queryDownloadStatus();   
	        }   
	    }; 
    
	    private void queryDownloadStatus() {   
	        DownloadManager.Query query = new DownloadManager.Query();   
	        query.setFilterById(mPrefs.getLong(DL_ID, 0));   
	        Cursor c = mDownloadManager.query(query); 
	        Log.d(TAG,"c.getCount=>"+c.getCount());
	        if(c.getCount() == 0){
	        	mPrefs.edit().clear().commit(); 
	        	downloadApk();
	        }
	        if(c.moveToFirst()) {   
	            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));   
	            switch(status) {   
	            case DownloadManager.STATUS_PAUSED:   
	                Log.d(TAG, "STATUS_PAUSED");  
	            case DownloadManager.STATUS_PENDING:   
	                Log.d(TAG, "STATUS_PENDING");  
	            case DownloadManager.STATUS_RUNNING:   
	                Log.d(TAG, "STATUS_RUNNING");  
	                break;   
	            case DownloadManager.STATUS_SUCCESSFUL:   
	                Log.d(TAG, "STATUS_SUCCESSFUL");  
	                installApk();
	                mContext.unregisterReceiver(receiver);  
	                mPrefs.edit().clear().commit(); 
	                break;   
	            case DownloadManager.STATUS_FAILED:   
	                Log.d(TAG, "STATUS_FAILED");  
	                mDownloadManager.remove(mPrefs.getLong(DL_ID, 0));   
	                mPrefs.edit().clear().commit(); 
	                mContext.unregisterReceiver(receiver);  
	                break;   
	            }   
	        }  
	    }  
    /**
     * 检查更新
     */
    private void checkAndUpdate(){
    	new Handler().postDelayed(new Runnable(){   
    	    public void run() {   
    	    	if(getServerVerInfo()){  		
    	    		int currentVerCode = getCurrentVerCode();
    	    		Log.d(TAG,"newVerCode:"+newVerCode+"===currentVerCode=>"+currentVerCode);
    	    		if(newVerCode>currentVerCode){
    	    			showAutoUpdateNoticeDialog();
    	    		}
    	    	}
    	    }   
    	}, 3000);   	
    }
		/**
     * 检查
     */
    private void check(){
    	if(getServerVerInfo()){  		
	    		int currentVerCode = getCurrentVerCode();
	    		Log.d(TAG,"newVerCode:"+newVerCode+"===currentVerCode=>"+currentVerCode);
	    		afterCheck(newVerCode == currentVerCode || newVerCode < currentVerCode);
    	}  	
    }

		private void afterCheck(boolean isNewest){
			if(isNewest){
					showIsNewestNoticeDialog();
			}else{
					showAutoUpdateNoticeDialog();
			}
		}
	/**
     * 获取应用当前版本代码
     * @param context
     * @return
     */
    private int getCurrentVerCode(){
    	String packageName = this.mContext.getPackageName();
    	int currentVer = -1;
    	try {
			currentVer = this.mContext.getPackageManager().getPackageInfo(packageName, 0).versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
    	return currentVer;
    }
    
    /**
     * 获取应用当前版本名称
     * @param context
     * @return
     */
    private String getCurrentVerName(){
    	String packageName = this.mContext.getPackageName();
    	String currentVerName = "";
    	try {
    		currentVerName = this.mContext.getPackageManager().getPackageInfo(packageName, 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
    	return currentVerName;
    }
    
    /**
     * 获取应用名称
     * @param context
     * @return
     */
    private String getAppName(){
    	//return this.mContext.getResources().getText(R.string.app_name).toString();
    	return "updateversion";
    }
    
    /**
     * 获取服务器上的版本信息
     * @param path
     * @return
     * @throws Exception
     */
    private boolean getServerVerInfo(){
    	Log.d(TAG,"getServerVerInfo");
		try {
			StringBuilder verInfoStr = new StringBuilder();
			URL url = new URL(checkPath);
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);
			conn.connect();
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(),"UTF-8"),8192);
			String line = null;
			while((line = reader.readLine()) != null){
				verInfoStr.append(line+"\n");
			}
			reader.close();
			conn.disconnect();
			Log.d(TAG,"verInfoStr=>"+verInfoStr.toString());
			JSONArray array = new JSONArray(verInfoStr.toString());
			if(array.length()>0){
				JSONObject obj = array.getJSONObject(0);
				newVerCode = obj.getInt("verCode");
				newVerName = obj.getString("verName");
				downloadPath = obj.getString("apkPath");
				updateInfo = obj.getString("updateInfo");
				apkName = obj.getString("apkName");
			}
		} catch (Exception e) {
			Log.d(TAG,"error:"+e.toString());
			if(conn != null)
				conn.disconnect();
			return false;
		} 
    	return true;
    	
    }
    
    private void showIsNewestNoticeDialog(){
    	Log.d(TAG,"showIsNewestNoticeDialog");    	
      AlertDialog.Builder builder = new Builder(mContext);
      builder.setTitle(R.string.update_dialog_title);
      String message = mContext.getResources().getString(R.string.update_dialog_message_newest_version)
      				+ getCurrentVerName();
      builder.setMessage(message);

      builder.setNegativeButton(R.string.update_dialog_confirm_btn, new OnClickListener(){
          public void onClick(DialogInterface dialog, int which){
              dialog.dismiss();
          }
      });
      Dialog noticeDialog = builder.create();
      noticeDialog.show();
    }
    
    /**
     * 显示软件更新对话框(自动)
     */
    private void showAutoUpdateNoticeDialog() {
    	Log.d(TAG,"showAutoUpdateNoticeDialog");

        AlertDialog.Builder builder = new Builder(mContext);
        builder.setTitle(R.string.update_dialog_title);
        String message = mContext.getResources().getString(R.string.update_dialog_message_current_version)
        				+ getCurrentVerName()
        				+ mContext.getResources().getString(R.string.update_dialog_message_new_version)
        				+ newVerName
        				+ mContext.getResources().getString(R.string.update_dialog_message_update_information)
        				+ updateInfo
        				+ mContext.getResources().getString(R.string.update_dialog_message_conform_update);
        builder.setMessage(message);

        builder.setPositiveButton(R.string.update_dialog_update_btn, new OnClickListener(){
            public void onClick(DialogInterface dialog, int which){
                dialog.dismiss();
                //showDownloadDialog();
                mContext.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));  
                downloadApk();
            }
        });
        builder.setNegativeButton(R.string.update_dialog_cancel_btn, new OnClickListener(){
            public void onClick(DialogInterface dialog, int which){
                dialog.dismiss();
            }
        });
        Dialog noticeDialog = builder.create();
        noticeDialog.show();
    }

    /**
     * 下载apk文件
     */
    private void downloadApk()
    {
        // 启动新线程下载软件
        //new downloadApkThread().start();
    	if(mDownloadManager == null)
    		mDownloadManager = (DownloadManager)mContext.getSystemService(Context.DOWNLOAD_SERVICE);
    	if(mPrefs == null)
    		mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    	Log.e(TAG,"mPrefs.contains(DL_ID)=>"+mPrefs.contains(DL_ID));
    	if(!mPrefs.contains(DL_ID)){
    		deleteFile();  
    		Log.d(TAG,"downloadPath=>"+downloadPath);
            Uri resource = Uri.parse(downloadPath);   
            DownloadManager.Request request = new DownloadManager.Request(resource);   
            request.setAllowedNetworkTypes(Request.NETWORK_MOBILE | Request.NETWORK_WIFI);   
            request.setAllowedOverRoaming(false);   

            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();  
            String mimeString = mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(downloadPath)); 
            Log.d(TAG,"mimeString=>"+mimeString);
            request.setMimeType(mimeString);  

            request.setNotificationVisibility (request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);  
            request.setVisibleInDownloadsUi(true);  

            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, apkName); 
            request.setTitle(mContext.getResources().getString(R.string.download_title_in_background) + apkName); 
            long id = mDownloadManager.enqueue(request);
            mPrefs.edit().putLong(DL_ID, id).commit();   
    	}else{
    		queryDownloadStatus();
    	}
    	
    }

    public void  deleteFile(){
    	File apkfile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), apkName);
		if (apkfile.exists()) {
			apkfile.delete();
		}
    }
	/**
	 * 安装APK文件
	 */
	private void installApk() {
		File apkfile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), apkName);
		if (!apkfile.exists()) {
			Log.d(TAG,"error:the file is not exists");
			return;
		}
		// 通过Intent安装APK文件
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setDataAndType(Uri.parse("file://" + apkfile.toString()),
				"application/vnd.android.package-archive");
		mContext.startActivity(i);
	}
    

}