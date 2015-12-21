package com.ksy.media.player.log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import org.json.JSONObject;

import com.ksy.media.player.util.Constants;
import com.ksy.media.player.util.Cpu;
import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * get data
 * 
 * @author LIXIAOPENG
 * 
 */
public class LogGetData {

	private static LogGetData mInstance;
	private static Object mLockObject = new Object();
	private static Context mContext;
	private static TelephonyManager tm;
	private static Cpu mCpuStats;
	private static ActivityManager mActivityManager;

	private LogGetData() {
	}

	private LogGetData(Context context) {
		tm = (TelephonyManager) context.getSystemService("phone");
		mActivityManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
	}

	public static LogGetData getInstance() {
		if (null == mInstance) {
			synchronized (mLockObject) {
				if (null == mInstance) {
					mInstance = new LogGetData();
				}
			}
		}
		return mInstance;
	}

	public static LogGetData getInstance(Context context) {
		if (null == mInstance) {
			synchronized (mLockObject) {
				if (null == mInstance) {
					mContext = context;
					mInstance = new LogGetData(context);
				}
			}
		}
		return mInstance;
	}

	/**
	 * get memory info
	 */
	public static long getMemory() {

		String str1 = "/proc/meminfo";// 系统内存信息文件
		String memoryInfo;
		String[] arrayOfString;
		long initial_memory = 0;

		try {
			FileReader localFileReader = new FileReader(str1);
			BufferedReader localBufferedReader = new BufferedReader(
					localFileReader, 8192);
			memoryInfo = localBufferedReader.readLine();// 读取meminfo第一行，系统总内存大小

			arrayOfString = memoryInfo.split("\\s+");
			for (String num : arrayOfString) {
				Log.i(memoryInfo, num + "\t");
			}

			// initial_memory = Integer.valueOf(arrayOfString[1]).intValue() *
			// 1024;// 获得系统总内存，单位是KB，乘以1024转换为Byte
			initial_memory = Integer.valueOf(arrayOfString[1]).intValue() / 1024;
			localBufferedReader.close();

		} catch (IOException e) {

		}

		// return Formatter.formatFileSize(mContext, initial_memory);//
		// Byte转换为KB或者MB，内存大小规格化

		return initial_memory;
	}

	/**
	 * get cpu info
	 */
	public static String getCpuInfo() {
		String cpuInfo = null;
		
		String cpu = getMaxCpu();
		String cpuCore = String.valueOf(Runtime.getRuntime()
				.availableProcessors());

		if (cpu != null && cpu != "") {
			double dot = Double.parseDouble(cpu) / 1000000;
			double cdot = getDecimal(dot);
			cpuInfo = String.valueOf(cdot);
		} else {
			cpuInfo = "unknown";
		}

		cpuInfo = cpuCore + "*" + cpuInfo;

		return cpuInfo;
	}

	// 四舍五入
	public static double getDecimal(double num) {
		if (Double.isNaN(num)) {
			return 0;
		}

		BigDecimal bd = new BigDecimal(num);
		num = bd.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();

		return num;
	}

	public static String getMaxCpu() {
		String result = "";
		ProcessBuilder cmd;
		try {
			String[] args = { "/system/bin/cat",
					"/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq" };
			cmd = new ProcessBuilder(args);
			Process process = cmd.start();
			InputStream in = process.getInputStream();
			byte[] re = new byte[24];
			while (in.read(re) != -1) {
				result = result + new String(re);
			}
			in.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			result = "N/A";
		}
		return result.trim();
	}

	/**
	 * get core version
	 */
	public static String getCoreVersion() {

		String coreVersion = Build.VERSION.RELEASE;

		return coreVersion;
	}

	/**
	 * getDeviceType
	 */
	public static String getDeviceType() {
	    
    	Build bd = new Build();
    	String model = bd.MODEL;
    	
		return model;
    }

	/**
	 * get device id & imei
	 */
	public static String getImei() {
		String imeiInfo = tm.getDeviceId();

		return imeiInfo;
	}

	/**
	 * get uuid
	 */
	public static String getUuid() {

		UUID uuid = UUID.randomUUID();

		return uuid.toString();
	}

	/**
	 * get net state
	 */
	public static String getNetState() {
		String net = null;

		ConnectivityManager cm = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if (ni != null && ni.isConnectedOrConnecting()) {
			switch (ni.getType()) {
			// wifi
			case ConnectivityManager.TYPE_WIFI:
				net = "WIFI";
				break;
			// mobile 网络
			case ConnectivityManager.TYPE_MOBILE:
				switch (ni.getSubtype()) {
				case TelephonyManager.NETWORK_TYPE_GPRS: // 联通2g
				case TelephonyManager.NETWORK_TYPE_CDMA: // 电信2g
				case TelephonyManager.NETWORK_TYPE_EDGE: // 移动2g
				case TelephonyManager.NETWORK_TYPE_1xRTT:
				case TelephonyManager.NETWORK_TYPE_IDEN:
					net = "2G";
					break;
				case TelephonyManager.NETWORK_TYPE_EVDO_A: // 电信3g
				case TelephonyManager.NETWORK_TYPE_UMTS:
				case TelephonyManager.NETWORK_TYPE_EVDO_0:
				case TelephonyManager.NETWORK_TYPE_HSDPA:
				case TelephonyManager.NETWORK_TYPE_HSUPA:
				case TelephonyManager.NETWORK_TYPE_HSPA:
				case TelephonyManager.NETWORK_TYPE_EVDO_B:
				case TelephonyManager.NETWORK_TYPE_EHRPD:
				case TelephonyManager.NETWORK_TYPE_HSPAP:
					net = "3G";
					break;
				case TelephonyManager.NETWORK_TYPE_LTE:// 4G
					net = "4G";
					break;
				// 未知,一般不会出现
				default:
					net = "UNKNOWN";
				}
				break;
			default:
				net = "UNKNOWN";
			}
		}

		return net;
	}

	/**
	 * get gmt
	 */
	public static String getGmt() {

		Calendar calendar = Calendar.getInstance();
		TimeZone timeZone = calendar.getTimeZone();

		return timeZone.getDisplayName();
	}

	/**
	 * get current time GMT
	 */
	public static long currentTimeGmt() {

		// Calendar cd = Calendar.getInstance();
		// SimpleDateFormat sdf = new SimpleDateFormat(
		// "EEE d MMM yyyy HH:mm:ss 'GMT'");
		// sdf.setTimeZone(TimeZone.getTimeZone("GMT")); // set GMT
		//
		// return sdf.format(cd.getTime());

		Date date = new Date();

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"); // yyyy-MM-dd
																			// hh:mm:ss
		Log.d(Constants.LOG_TAG, "date.gettime=" + date.getTime());
		return date.getTime();

		// return df.format(date);
	}

	/**
	 * get out net ip
	 * @return ip
	 */
	public static String getOutIp() {
		String IP = "";
		try {
			String address = "http://ip.taobao.com/service/getIpInfo2.php?ip=myip";
			URL url = new URL(address);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setUseCaches(false);

			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				InputStream in = connection.getInputStream();

				// 将流转化为字符串
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(in));

				String tmpString = "";
				StringBuilder retJSON = new StringBuilder();
				while ((tmpString = reader.readLine()) != null) {
					retJSON.append(tmpString + "\n");
				}

				JSONObject jsonObject = new JSONObject(retJSON.toString());
				String code = jsonObject.getString("code");
				if (code.equals("0")) {
					JSONObject data = jsonObject.getJSONObject("data");
					IP = data.getString("ip") + "(" + data.getString("country")
							+ data.getString("area") + "区"
							+ data.getString("region") + data.getString("city")
							+ data.getString("isp") + ")";

					Log.d(Constants.LOG_TAG, "IP地址是：" + IP);
				} else {
					IP = "0.0.0.0";
					Log.e(Constants.LOG_TAG, "IP接口异常，无法获取IP地址！");
				}
			} else {
				IP = "0.0.0.0";
				Log.e(Constants.LOG_TAG, "网络连接异常，无法获取IP地址！");
			}
		} catch (Exception e) {
			IP = "0.0.0.0";
			Log.e(Constants.LOG_TAG, "获取IP地址时出现异常，异常信息是：" + e.toString());
		}
		return IP;
	}
	
	
	/**
	 * get deviceip
	 */
	/*public static String getDeviceIp() {

		String ip = null;

		if (getNetState() != null) {
			if (getNetState().equals("WIFI")) {
				try {
					WifiManager wifiManager = (WifiManager) mContext
							.getSystemService(Context.WIFI_SERVICE);
					WifiInfo wifiInfo = wifiManager.getConnectionInfo();
					int i = wifiInfo.getIpAddress();
					ip = int2ip(i);
					// return int2ip(i);
				} catch (Exception ex) {
					return "ex==" + ex.getMessage();
				}

			} else {

				try {
					for (Enumeration<NetworkInterface> en = NetworkInterface
							.getNetworkInterfaces(); en.hasMoreElements();) {
						NetworkInterface intf = en.nextElement();
						for (Enumeration<InetAddress> enumIpAddr = intf
								.getInetAddresses(); enumIpAddr
								.hasMoreElements();) {
							InetAddress inetAddress = enumIpAddr.nextElement();
							if (!inetAddress.isLoopbackAddress()) {
								ip = inetAddress.getHostAddress().toString();
								// return ip;
							}
						}
					}
				} catch (SocketException ex) {
					Log.e("WifiPreference IpAddress", ex.toString());
				}
			}

		} else {
			ip = "0.0.0.0";
		}
		
		return ip;
	}*/

	/**
	 * get cpu usage
	 */
	public static int getCpuUsage(String pack) {

		mCpuStats = new Cpu(pack);
		mCpuStats.parseTopResults();

		int cpuUsage = 0;
		if (mCpuStats.getProcessCpuUsage() != null) {
			cpuUsage = Integer.valueOf(mCpuStats.getProcessCpuUsage())
					.intValue();
		}

		return cpuUsage;
	}

	/**
	 * get memory usage
	 */
	public static int getMemoryUsage() {

		int pid = android.os.Process.myPid();
		android.os.Debug.MemoryInfo[] memoryInfoArray = mActivityManager
				.getProcessMemoryInfo(new int[] { pid });
		// float i = ((float)memoryInfoArray[0].getTotalPrivateDirty() / 1024);
		// memoryUsage = String.valueOf(i) + "MB";

		int memoryUsage = (memoryInfoArray[0].getTotalPrivateDirty() / 1024);

		return memoryUsage;
	}

	/**
	 * 将ip的整数形式转换成ip形式
	 * 
	 * @param ipInt
	 * @return
	 */
	public static String int2ip(int ipInt) {
		StringBuilder sb = new StringBuilder();
		sb.append(ipInt & 0xFF).append(".");
		sb.append((ipInt >> 8) & 0xFF).append(".");
		sb.append((ipInt >> 16) & 0xFF).append(".");
		sb.append((ipInt >> 24) & 0xFF);
		return sb.toString();
	}

}
