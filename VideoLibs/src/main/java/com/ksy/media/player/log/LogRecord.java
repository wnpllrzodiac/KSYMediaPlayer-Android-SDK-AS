package com.ksy.media.player.log;

import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;


public class LogRecord {
	
	private String cpu;
	private long memory;
	private String core;
	private String device;
	private String uuid;
	private String system;
	private String net;
	private String gmt;
	private long date;
	private String userAgent;
	private String deviceIp;
	private String serverIp;
	private int cpuUsage;
	private int memoryUsage;
	private long firstFrameTime;
	private int cacheBufferSize;
	private String seekStatus;
	private String seekMessage;
	private String playStatus;
	private String playType; //直播点播
	private String protocol; //协议
	private String format; // 格式
	private String audioCodec; //音频编码
	private String videoCodec; //视频编码
	private String deviceType;
	     
	private String companyKey;
	

	private static LogRecord mInstance;
	private static Object mLockObject = new Object();
	
	public static LogRecord getInstance() {
		if (null == mInstance) {
			synchronized (mLockObject) {
				if (null == mInstance) {
					mInstance = new LogRecord();
				}
			}
		}
		return mInstance;
	}
	
	
	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}	
	
	public String getPlayType() {
		return playType;
	}

	public void setPlayType(String playType) {
		this.playType = playType;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}
	
	public String getAudioCodec() {
		return audioCodec;
	}

	public void setAudioCodec(String audioCodec) {
		this.audioCodec = audioCodec;
	}

	public String getVideoCodec() {
		return videoCodec;
	}

	public void setVideoCodec(String videoCodec) {
		this.videoCodec = videoCodec;
	}

	public String getCompanyKey() {
		return companyKey;
	}

	public void setCompanyKey(String companyKey) {
		this.companyKey = companyKey;
	}

	public String getCpu() {
		return cpu;
	}

	public void setCpu(String cpu) {
		this.cpu = cpu;
	}

	public long getMemory() {
		return memory;
	}

	public void setMemory(long memory) {
		this.memory = memory;
	}

	public String getCore() {
		return core;
	}

	public void setCore(String core) {
		this.core = core;
	}

	public String getDevice() {
		return device;
	}

	public void setDevice(String device) {
		this.device = device;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getSystem() {
		return system;
	}

	public void setSystem(String system) {
		this.system = system;
	}

	public String getNet() {
		return net;
	}

	public void setNet(String net) {
		this.net = net;
	}

	public String getGmt() {
		return gmt;
	}

	public void setGmt(String gmt) {
		this.gmt = gmt;
	}

	public long getDate() {
		return date;
	}

	public void setDate(long date) {
		this.date = date;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getDeviceIp() {
		return deviceIp;
	}

	public void setDeviceIp(String deviceIp) {
		this.deviceIp = deviceIp;
	}

	public String getServerIp() {
		return serverIp;
	}

	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}

	public int getCpuUsage() {
		return cpuUsage;
	}

	public void setCpuUsage(int cpuUsage) {
		this.cpuUsage = cpuUsage;
	}

	public int getMemoryUsage() {
		return memoryUsage;
	}

	public void setMemoryUsage(int memoryUsage) {
		this.memoryUsage = memoryUsage;
	}

	public long getFirstFrameTime() {
		return firstFrameTime;
	}

	public void setFirstFrameTime(long firstFrameTime) {
		this.firstFrameTime = firstFrameTime;
	}

	public int getCacheBufferSize() {
		return cacheBufferSize;
	}

	public void setCacheBufferSize(int cacheBufferSize) {
		this.cacheBufferSize = cacheBufferSize;
	}

	public String getSeekStatus() {
		return seekStatus;
	}

	public void setSeekStatus(String seekStatus) {
		this.seekStatus = seekStatus;
	}

	public String getSeekMessage() {
		return seekMessage;
	}

	public void setSeekMessage(String seekMessage) {
		this.seekMessage = seekMessage;
	}

	public String getPlayStatus() {
		return playStatus;
	}

	public void setPlayStatus(String playStatus) {
		this.playStatus = playStatus;
	}

	
	//"{\"_id\":\"uuid\",\"date\":\"date\",\"type\":\"101\",\"cpu\":\"cpu\",\"memory\":\"memory\",\"core\":\"core\",\"device\":\"device\",\"system\":\"system\",\"userAgent\":\"userAgent\",\"gmt\":\"gmt\"}"
	public String getBaseDataJson() {
      JSONObject obj = new JSONObject();
	    try {
			obj.put("_id", getUuid());
			obj.put("date", getDate());
		    obj.put("type", "101");
		    obj.put("cpu", getCpu());
		    obj.put("memory", getMemory());
		    obj.put("core", getCore());
		    obj.put("device", getDevice());
		    obj.put("system", getSystem());
		    obj.put("userAgent", getUserAgent());
		    obj.put("deviceModel", getDeviceType());
		    obj.put("gmt", getGmt());
		    
		} catch (JSONException e) {
			e.printStackTrace();
		}
	    
		return obj.toString();
	}
	
	public String getBaseDataEndJson() {
      JSONObject obj = new JSONObject();
        
	    try {
			obj.put("_id", getUuid());
			obj.put("date", getDate());
		    obj.put("type", "103");
		    obj.put("cpu", getCpu());
		    obj.put("memory", getMemory());
		    obj.put("core", getCore());
		    obj.put("device", getDevice());
		    obj.put("system", getSystem());
		    obj.put("userAgent", getUserAgent());
		    obj.put("deviceModel", getDeviceType());
//		    obj.put("gmt", getGmt());
		    
		} catch (JSONException e) {
			e.printStackTrace();
		}
	    
		return obj.toString();
	}
	
	//"{\"_id\":\"uuid\",\"date\":\"date\",\"type\":\"102\",\"playStatus\":\"playStatus\",\"gmt\":\"gmt\"}"
	public String getPlayStatusJson() {
       JSONObject obj = new JSONObject();
        
	    try {
			obj.put("_id", getUuid());
			obj.put("date", getDate());
		    obj.put("type", "102");
		    obj.put("playStatus", getPlayStatus());
//		    obj.put("gmt", getGmt());
		    
		} catch (JSONException e) {
			e.printStackTrace();
		}
	    
		return obj.toString();
	}
	
	//"{\"_id\":\"uuid\",\"date\":\"date\",\"type\":\"103\",\"firstFrameTime\":\"firstFrameTime\",\"playMetaData\":\"playMetaData\",\"gmt\":\"gmt\"}"
	public String getFirstFrameTimeJson() {
       JSONObject obj = new JSONObject();
        
	    try {
			obj.put("_id", getUuid());
			obj.put("date", getDate());
		    obj.put("type", "102");
		    obj.put("firstFrameTime", getFirstFrameTime());
		    obj.put("cacheBufferSize", getCacheBufferSize()); //String.valueOf(getCacheBufferSize())
		    obj.put("playType", getPlayType());
		    obj.put("protocol", getProtocol());
		    obj.put("format", getFormat());
		    obj.put("audioCodec", getAudioCodec());
		    obj.put("videoCodec", getVideoCodec());
//		    obj.put("gmt", getGmt());
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
	    
		return obj.toString();
	}
	
	
	//"{\"_id\":\"uuid\",\"date\":\"date\",\"type\":\"200\",\"field\":\"net\",\"net\":\"net\",\"deviceIp\":\"deviceIp\",\"serverIp\":\"serverIp\",\"gmt\":\"gmt\"}"
	public String getNetStateJson() {
       JSONObject obj = new JSONObject();
        
	    try {
			obj.put("_id", getUuid());
			obj.put("date", getDate());
		    obj.put("type", "200");
		    obj.put("field", "net");
		    obj.put("net", getNet());
		    obj.put("deviceIp", getDeviceIp());
		    obj.put("serverIp", getServerIp());
//		    obj.put("gmt", getGmt());
		    
		} catch (JSONException e) {
			e.printStackTrace();
		}
	    
		return obj.toString();
	}
	
	
	//"{\"_id\":\"uuid\",\"date\":\"date\",\"type\":\"200\",\"field\":\"usage\",\"deviceUsage\":\"{"cpuUsage":"cpuUsage","memoryUsage":"memoryUsage"}\",\"gmt\":\"gmt\"}" 
	public String getCapabilityJson() {
		JSONObject obj = new JSONObject();
		
		try {
			obj.put("_id", getUuid());
			obj.put("date", getDate());
			obj.put("type", "200");
			obj.put("field", "usage");
//			obj.put("deviceUsage", getCurrentUsage());
			obj.put("cpu_usage", getCpuUsage());
			obj.put("memory_usage", getMemoryUsage());
//			obj.put("gmt", getGmt());
			    
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return obj.toString();
	}
	
	/*private Object getCurrentUsage() {
        JSONObject obj = new JSONObject();
        
	    try {
			obj.put("cpuUsage", getCpuUsage());
			obj.put("memoryUsage", getMemoryUsage());
		    
		} catch (JSONException e) {
			e.printStackTrace();
		}
	    
		return obj;
	}*/
	
	//"{\"_id\":\"uuid\",\"date\":\"date\",\"type\":\"201\",\"field\":\"seekbegin\",\"seekBegin\":\"seekBegin\",\"gmt\":\"gmt\"}"
	public String getSeekBeginJson() {
        JSONObject obj = new JSONObject();
        
	    try {
			obj.put("_id", getUuid());
			obj.put("date", getDate());
		    obj.put("type", "201");
		    obj.put("field", "seek");
//		    obj.put("seekBegin", getSeekBegin()); //String.valueOf(getSeekBegin())
//		    obj.put("gmt", getGmt());
		    
		} catch (JSONException e) {
			e.printStackTrace();
		}
	    
		return obj.toString();
	}
	
	//"{\"_id\":\"uuid\",\"date\":\"date\",\"type\":\"202\",\"field\":\"seekend\",\"seekEnd\":\"seekEnd\",\"gmt\":\"gmt\"}"
	public String getSeekEndJson() {
        JSONObject obj = new JSONObject();
        
	    try {
			obj.put("_id", getUuid());
			obj.put("date", getDate());
		    obj.put("type", "203");
		    obj.put("field", "seek");
		    obj.put("status", getSeekStatus());
		    obj.put("message", getSeekMessage());
//		    obj.put("seekEnd", getSeekEnd());
//		    obj.put("gmt", getGmt());
		    
		} catch (JSONException e) {
			e.printStackTrace();
		}
	    
		return obj.toString();
	}
	
	//用户自定义数据接口
	public String getUserDefinedJson(Map<String, String> map, String field, String type) {
		JSONObject obj = new JSONObject();
		
		if (map != null && map.size() > 0) {
			for (String key : map.keySet()) {
//				Log.d(Constants.LOG_TAG, "key= "+ key + " and value= " + map.get(key));
				
				try {
					obj.put(key, map.get(key));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		
		try {
			obj.put("_id", getUuid());
			obj.put("date", getDate());
			obj.put("field", field);
			obj.put("type", type);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return obj.toString();
	}


}


