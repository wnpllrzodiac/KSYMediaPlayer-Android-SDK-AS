package com.ksy.media.player.log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.text.TextUtils;
import android.util.Log;


public class LogClient {
	private static final int LOG_ONCE_LIMIT = 120;
	private static final long TIMER_INTERVAL = 5 * 1000; //60 * 60 * 1000;
//	private static final long SAVE_TIME_INTERVAL = 10 * 1000; //用户可选
	private static LogClient mInstance;
	private static Object mLockObject = new Object();
	private static Context mContext;
	private volatile boolean mStarted = false;
	private volatile boolean mSaveStarted = false;
	private Timer timer;
	private Timer saveTimer;
	private boolean isNeedloop;
	private int sendCount; 
	private String companyKey;
	String mIp;

	public static LogGetData logGetData;
	private LogRecord logRecord = LogRecord.getInstance();
	private static String pack = null;
	public static boolean mSwitch = false; //开关
	
	private LogClient() {
	}

	private LogClient(Context context) {
	}

	public static LogClient getInstance() {
		if (null == mInstance) {
			synchronized (mLockObject) {
				if (null == mInstance) {
					mInstance = new LogClient();
					logGetData = LogGetData.getInstance();
				}
			}
		}
		return mInstance;
	}
	
	
	public static LogClient getInstance(Context context) {
		if (null == mInstance) {
			synchronized (mLockObject) {
				if (null == mInstance) {
					mContext = context;
					mInstance = new LogClient(context);
					logGetData = LogGetData.getInstance(context);
					pack = getPackName(context);
				}
			}
		}
		return mInstance;
	}
	
	//get packname
	public static String getPackName(Context context) {
		PackageInfo info;
		String packageName = null;
		
	    try {    
	        info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);    
	        packageName = info.packageName;
	        
	    } catch (NameNotFoundException e) {    
	        e.printStackTrace();    
	    }
	    
		return packageName;    
	}

	
	//add data
	public void addData() { 
		
		new Thread() {
			public void run() {
				mIp = logGetData.getOutIp();
				if (mIp.indexOf("(")!= -1) {
					mIp = mIp.substring(0,  mIp.lastIndexOf("("));
				}
				
				logRecord.setDeviceIp(mIp);
			}

		}.start();
		
		
		logRecord.setUuid(logGetData.getUuid());
		logRecord.setCpu(logGetData.getCpuInfo());
		logRecord.setCore(logGetData.getCoreVersion());
		logRecord.setMemory(logGetData.getMemory());
		logRecord.setDevice(logGetData.getImei());
		logRecord.setGmt(logGetData.getGmt());
		logRecord.setNet(logGetData.getNetState());
		logRecord.setSystem("Android");
		logRecord.setUserAgent("Android");
		logRecord.setDeviceType(logGetData.getDeviceType());
	}
	
	/**
	* @param srcObj 源字节数组转换成String的字节数组
	* @return
	*/
	public static byte[]  StringToBt(String str) {
	    return StringToByte(str, "UTF-8");
	}


	public static byte[] StringToByte(String str, String charEncode) {
		byte[] destObj = null;
		try {
		if(null == str || str.trim().equals("")){ 
		  destObj = new byte[0]; 
		  return destObj; 
		  
		}else{ 
		  destObj = str.getBytes(charEncode);
		}
		} catch (UnsupportedEncodingException e) {
		  e.printStackTrace();
		}
		return destObj;
	}
	
	
	private void sendRecordJson(final RecordResult recordsResult,
			final int sendCount, final int allCount, final boolean isNeedloop) {
		ByteArrayEntity byteArrayEntity = null;
//		String jsonString = makeJsonLog(recordsResult.contentBuffer.toString()); //取什么
		String jsonString = recordsResult.contentBuffer.toString(); //单条或多条
		Log.d(Constants.LOG_TAG, "jsonString =" + jsonString);
		
		try {
			byteArrayEntity = new ByteArrayEntity(GzipUtil.compress(jsonString)
					.toByteArray());
			
//			byteArrayEntity = new ByteArrayEntity(StringToBt(jsonString)); //单条
			
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(Constants.LOG_TAG, "gzip is failed, send log ingored e" + e);
			return;
		}

		//第一步
		HttpClient httpClient = new DefaultHttpClient();
		
		//第二步：生成使用POST方法的请求对象
		companyKey = logRecord.getCompanyKey();
		if (companyKey.indexOf("/")!=-1) {
			companyKey = companyKey.replaceAll("/", "");
		}
		
		String mUrl = Constants.LOG_SERVER_URL + companyKey + "/";
		Log.d(Constants.LOG_TAG, "mUrl =" + mUrl);
		HttpPost httpPost = new HttpPost(mUrl);
		httpPost.addHeader("accept-encoding", "gzip, deflate");
		
//		httpPost.addHeader("Content-Type", "application/json"); //单条
		
		try {
			//将请求体放置在请求对象当中
			httpPost.setEntity(byteArrayEntity);
			// 执行请求对象
			try {
				//第三步：执行请求对象，获取服务器发还的相应对象
				HttpResponse response = httpClient.execute(httpPost);
				
				String result = convertStreamToString(response.getEntity().getContent());
				Log.d(Constants.LOG_TAG, "result = " + result);
				
				//第四步：检查相应的状态是否正常：检查状态码的值是200表示正常
				if (response.getStatusLine().getStatusCode() == 200) {
					Log.d(Constants.LOG_TAG, "recordsResult.idBuffer 1 =" + recordsResult.idBuffer.toString());
					
					DBManager.getInstance(mContext).deleteLogs(
							recordsResult.idBuffer.toString());
					
					Log.d(Constants.LOG_TAG, " 200  log send count:" + sendCount
							+ ",next count : " + (allCount - sendCount));
					
					recordsResult.release();
					if (isNeedloop) {
						
						if (allCount - sendCount > 0) {
							Log.d(Constants.LOG_TAG, "allCount - sendCount > 0");
							sendRecord(allCount - sendCount);
						} else {
							Log.d(Constants.LOG_TAG, "more than 120 mode, last send all over");
						}
					} else {
						Log.d(Constants.LOG_TAG, "less than 120 mode, send all over");
						sendRecord(allCount);
					}
					
				} else {
                   //failure
				   Log.e(Constants.LOG_TAG, "response.getStatusLine().getStatusCode()=" + response.getStatusLine().getStatusCode()); 
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(Constants.LOG_TAG, "HttpResponse error =" + e);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(Constants.LOG_TAG, "httpPost error ===" + e);
		}
	}

	 //InputStream convert string
	 public String convertStreamToString(InputStream is) {
		
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));   
		StringBuilder sb = new StringBuilder();      

		String line = null;   

		try {   
			while ((line = reader.readLine()) != null) {   
				sb.append(line + "/n");   
			}
		} catch (IOException e) {   
			e.printStackTrace();   
		} finally {   
			try {   
				is.close();   
			} catch (IOException e) {   
				e.printStackTrace();   
			}   
		}   
		return sb.toString();   
	}  
	 
	
	/*private String makeJsonLog(String recordsJson) {
		JSONArray array = new JSONArray();
		String[] singlgLogJson = recordsJson.split("/n"); //  /r/n
		for (int i = 0; i < singlgLogJson.length; i++) {
			JSONObject record;
			try {
				record = new JSONObject(singlgLogJson[i]);
				array.put(record);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return array.toString();
	}*/


	public void saveUsageData(int time) {
		if (mSaveStarted) {
			return;
		}
		
		mSaveStarted = true;
		saveTimer = new Timer();
		saveTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					logRecord.setCpuUsage(logGetData.getCpuUsage(pack));
					logRecord.setMemoryUsage(logGetData.getMemoryUsage());
					logRecord.setDate(logGetData.currentTimeGmt());
					
					Log.d(Constants.LOG_TAG, "logRecord.getCapabilityJson() =" + logRecord.getCapabilityJson());
					mInstance.put(logRecord.getCapabilityJson());
					
				} catch (Ks3ClientException e) {
					e.printStackTrace();
					Log.e(Constants.LOG_TAG, "saveUsageData e = " + e);
				}
			}
		}, 3000, time);
	}
	
	
	public void start() {
		if (mStarted) {
			return;
		}
		mStarted = true;
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				// Judge the way of send for test
				int current_count = DBManager.getInstance(mContext)
						.queryCount();
				
				Log.d(Constants.LOG_TAG, "send schedule, current thread id = "
						+ Thread.currentThread().getId() + ",log count = "
						+ current_count);
				if (NetworkUtil.isNetworkAvailable(mContext)) {
					Log.d(Constants.LOG_TAG, "network valiable");
					if (NetworkUtil.getNetWorkType(mContext) == ConnectivityManager.TYPE_WIFI) {
						Log.d(Constants.LOG_TAG, "network valiable,type wifi");
						if (current_count > 0) {
							Log.d(Constants.LOG_TAG, "send record");
							sendRecord(current_count);
						} else {
							Log.d(Constants.LOG_TAG, "no record");
						}
					} else {
						Log.e(Constants.LOG_TAG,
								"network valiable,type not wifi");
					}
				} else {
					Log.e(Constants.LOG_TAG, "network unvaliable");
					stop();
				}
			}
		}, 5000, TIMER_INTERVAL);
	}

	
	private void sendRecord(int all_count) {
		isNeedloop = all_count >= LOG_ONCE_LIMIT;
		sendCount = isNeedloop ? LOG_ONCE_LIMIT : all_count;
		RecordResult recordResults = new RecordResult();
		DBManager.getInstance(mContext).getRecords(sendCount, recordResults);
		if (!TextUtils.isEmpty(recordResults.contentBuffer.toString())
				&& !TextUtils.isEmpty(recordResults.idBuffer.toString())) {
			sendRecordJson(recordResults, sendCount, all_count, isNeedloop);
		} else {
			Log.e(Constants.LOG_TAG, "read record result is not correct");
		}
	}

	
	public void stop() {
		if (!mStarted) {
			return;
		}
		
		if (!mSaveStarted) {
			return;
		}
		
		if (null != timer) {
			timer.cancel();
		}
		
		if (null != saveTimer) {
			saveTimer.cancel();
		}
		
		mStarted = false;
		mSaveStarted = false;
		
	}

	public void put(String message) throws Ks3ClientException {
		Log.d(Constants.LOG_TAG, "put() new log: " + message);
		if (jsonCheck(message)) {
			DBManager.getInstance(mContext).insertLog(message);
		} else {
			throw new Ks3ClientException(
					"put() new log format is not correct, sdk will ingore it");
		}
	}

	private boolean jsonCheck(String message) {
		boolean isJson = true;
		try {
			JSONObject object = new JSONObject(message);
		} catch (JSONException e) {
			Log.e(Constants.LOG_TAG, "jsonCheck  e ==" + e);
			isJson = false;
		}
		return isJson;
	}

	public void put(LogRecord record) throws Ks3ClientException {
		if (record != null) {
			Log.d(Constants.LOG_TAG, "put() new log: " + record.toString());
			DBManager.getInstance(mContext).insertLog(record.toString());
		} else {
			throw new Ks3ClientException("record can not be null");
		}
	}

	/*
	 * private void saveToSDCard(String filename, String content) throws
	 * Exception { File file = new
	 * File(Environment.getExternalStorageDirectory(), filename); OutputStream
	 * out = new FileOutputStream(file); out.write(content.getBytes());
	 * out.close(); }
	 * 
	 * private void saveToSDCard(String filename, byte[] content) throws
	 * Exception { File file = new
	 * File(Environment.getExternalStorageDirectory(), filename); OutputStream
	 * out = new FileOutputStream(file); out.write(content); out.close(); }
	 */

}
