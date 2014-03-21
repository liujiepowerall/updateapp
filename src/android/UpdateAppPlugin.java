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
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class UpdateAppPlugin extends CordovaPlugin {

	/*�汾�ż��·��*/
	private String checkPath;
	/* �°汾��*/
	private int newVerCode;
	/* �°汾���� */
	private String newVerName;
	/* APK ����·��*/
	private String  downloadPath;
	/* ������ */
    private static final int DOWNLOAD = 1;
    /* ���ؽ��� */
    private static final int DOWNLOAD_FINISH = 2;
    /* ���ر���·�� */
    private String mSavePath;
    /* ��¼���������� */
    private int progress;
    /* �Ƿ�ȡ������ */
    private boolean cancelUpdate = false;
    /* ������*/
    private Context mContext;
    /* ���½����� */
    private ProgressBar mProgress;
    private Dialog mDownloadDialog;
    
	@Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.mContext = cordova.getActivity();
        android.util.Log.d("abc","action=>"+action);
		if (action.equals("checkAndUpdate")) {
			callbackContext.success();
			this.checkPath = args.getString(0);
			android.util.Log.d("abc","checkPath1=>"+checkPath);
			checkAndUpdate();
    }else if(action.equals("getCurrentVersion")){
        	//�Ż� ���̴������ݣ���������
//        	JSONObject obj = new JSONObject();
//        	obj.put("versionCode", this.getCurrentVerCode());
//        	obj.put("versionName", this.getCurrentVerName());
        	callbackContext.success(this.getCurrentVerCode()+"");
    }else if(action.equals("getServerVersion")){
        	this.checkPath = args.getString(0);
        	if(this.getServerVerInfo()){
        		//�Ż� ���̴������ݣ���������
//        		JSONObject obj = new JSONObject();
//            	obj.put("serverVersionCode", newVerCode);
//            	obj.put("serverVersionName", newVerName);
        		callbackContext.success(newVerCode+"");
        	}else{
        		callbackContext.error("can't connect to the server!please check [checkpath]");
        	}
        	
        }
        return false;
    }


    
    /**
     * ������
     */
    private void checkAndUpdate(){
    	if(getServerVerInfo()){  		
    		int currentVerCode = getCurrentVerCode();
    		Log.d("abc","newVerCode:"+newVerCode+"===currentVerCode=>"+currentVerCode);
    		if(newVerCode>currentVerCode){
    			this.showNoticeDialog();
    		}
    	}
    }


	/**
     * ��ȡӦ�õ�ǰ�汾����
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
     * ��ȡӦ�õ�ǰ�汾����
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
     * ��ȡӦ������
     * @param context
     * @return
     */
    private String getAppName(){
    	//return this.mContext.getResources().getText(R.string.app_name).toString();
    	return "updateversion";
    }
    
    /**
     * ��ȡ�������ϵİ汾��Ϣ
     * @param path
     * @return
     * @throws Exception
     */
    private boolean getServerVerInfo(){
    	Log.d("abc","getServerVerInfo");
		try {
			StringBuilder verInfoStr = new StringBuilder();
			URL url = new URL(checkPath);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);
			conn.connect();
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(),"UTF-8"),8192);
			String line = null;
			while((line = reader.readLine()) != null){
				verInfoStr.append(line+"\n");
			}
			reader.close();
			Log.d("abc","verInfoStr=>"+verInfoStr.toString());
			JSONArray array = new JSONArray(verInfoStr.toString());
			if(array.length()>0){
				JSONObject obj = array.getJSONObject(0);
				newVerCode = obj.getInt("verCode");
				newVerName = obj.getString("verName");
				downloadPath = obj.getString("apkPath");
			}
		} catch (Exception e) {
			Log.d("abc","error:"+e.toString());
			Log.d("abc","return false");
			return false;
		} 
		Log.d("abc","getServerVerInfo return true");
    	return true;
    	
    }
    
    /**
     * ��ʾ������¶Ի���
     */
    private void showNoticeDialog() {
    	Log.d("abc","showNoticeDialog");
        // ����Ի���
    	cancelUpdate = false;
        AlertDialog.Builder builder = new Builder(mContext);
        builder.setTitle("update"/*R.string.soft_update_title*/);
        builder.setMessage("update"/*R.string.soft_update_info*/);
        // ����
        builder.setPositiveButton("update"/*R.string.soft_update_updatebtn*/, new OnClickListener(){
            public void onClick(DialogInterface dialog, int which){
                dialog.dismiss();
                // ��ʾ���ضԻ���
                showDownloadDialog();
            }
        });
        // �Ժ����
        builder.setNegativeButton("later"/*R.string.soft_update_later*/, new OnClickListener(){
            public void onClick(DialogInterface dialog, int which){
                dialog.dismiss();
            }
        });
        Dialog noticeDialog = builder.create();
        noticeDialog.show();
    }

    /**
     * ��ʾ������ضԻ���
     */

    private void showDownloadDialog()
    {
        // ����������ضԻ���
        AlertDialog.Builder builder = new Builder(mContext);
        builder.setTitle("update"/*R.string.soft_updating*/);
        // �����ضԻ������ӽ�����
        /*
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        View v = inflater.inflate(R.layout.softupdate_progress, null);
        mProgress = (ProgressBar) v.findViewById(R.id.update_progress);
        */
        LinearLayout v = new LinearLayout(mContext);
        mProgress = new ProgressBar(mContext);
        v.addView(mProgress);
        builder.setView(v);
        // ȡ������
        builder.setNegativeButton("cancel"/*R.string.soft_update_cance*/, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // ����ȡ��״̬
                cancelUpdate = true;
            }
        });
        mDownloadDialog = builder.create();
        mDownloadDialog.show();
        // �����ļ�
        downloadApk();
    }

    /**
     * ����apk�ļ�
     */
    private void downloadApk()
    {
        // �������߳��������
        new downloadApkThread().start();
    }

    private Handler mHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
            // ��������
            case DOWNLOAD:
                // ���ý�����λ��
            	Log.d("abc","progress=>"+progress);
                mProgress.setProgress(progress);
                break;
            case DOWNLOAD_FINISH:
                // ��װ�ļ�
                installApk();
                break;
            default:
                break;
            }
        };
    };
    
    /**
     * �����ļ��߳�
     */
	private class downloadApkThread extends Thread {
		@Override
		public void run() {
			try {
				// �ж�SD���Ƿ���ڣ������Ƿ���ж�дȨ��
				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					// ��ô洢����·��
					String sdpath = Environment.getExternalStorageDirectory()+ "/";
					mSavePath = sdpath + "download";
					Log.d("abc","mSavePath:"+mSavePath);
					URL url = new URL(downloadPath);
					// ��������
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.connect();
					// ��ȡ�ļ���С
					int length = conn.getContentLength();
					// ����������
					InputStream is = conn.getInputStream();

					File file = new File(mSavePath);
					// �ж��ļ�Ŀ¼�Ƿ����
					if (!file.exists()) {
						file.mkdir();
					}
					File apkFile = new File(mSavePath, newVerName);
					FileOutputStream fos = new FileOutputStream(apkFile);
					int count = 0;
					// ����
					byte buf[] = new byte[1024];
					// д�뵽�ļ���
					do {
						int numread = is.read(buf);
						count += numread;
						// ���������λ��
						progress = (int) (((float) count / length) * 100);
						// ���½���
						mHandler.sendEmptyMessage(DOWNLOAD);
						if (numread <= 0) {
							// �������
							mHandler.sendEmptyMessage(DOWNLOAD_FINISH);
							break;
						}
						// д���ļ�
						fos.write(buf, 0, numread);
					} while (!cancelUpdate);// ���ȡ����ֹͣ����.
					fos.close();
					is.close();
				}
			} catch (MalformedURLException e) {
				Log.d("abc","error:"+e.toString());
			} catch (IOException e) {
				Log.d("abc","error:"+e.toString());
			}
			// ȡ�����ضԻ�����ʾ
			mDownloadDialog.dismiss();
		}
	};

	/**
	 * ��װAPK�ļ�
	 */
	private void installApk() {
		File apkfile = new File(mSavePath, newVerName);
		if (!apkfile.exists()) {
			return;
		}
		// ͨ��Intent��װAPK�ļ�
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setDataAndType(Uri.parse("file://" + apkfile.toString()),
				"application/vnd.android.package-archive");
		mContext.startActivity(i);
	}
    

}