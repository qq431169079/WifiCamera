package com.thecamhi.base;



import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.bugly.crashreport.CrashReport.UserStrategy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CrashApplication extends Application{
	private static  CrashApplication app;
	@Override
	public void onCreate() {
		super.onCreate();
		app=this;
		CrashHandler.getInstance().init(this);
		Context context = getApplicationContext();
		// 获取当前包名
		String packageName = context.getPackageName();
		// 获取当前进程名
		String processName = getProcessName(android.os.Process.myPid());
		// 设置是否为上报进程
		UserStrategy strategy = new UserStrategy(context);
		strategy.setUploadProcess(processName == null || processName.equals(packageName));
		// 初始化Bugly  建议在测试阶段建议设置成true，发布时设置为false。
		CrashReport.initCrashReport(context, "f47a8c997b", true, strategy);
		// 如果通过“AndroidManifest.xml”来配置APP信息，初始化方法如下
		// CrashReport.initCrashReport(context, strategy);
		//CrashReport.startCrashReport();
		
	}
	
	public static synchronized CrashApplication getInstance(){
		return app;
	}
	
	
	/**
	 * 获取进程号对应的进程名
	 * 
	 * @param pid 进程号
	 * @return 进程名
	 */
	private static String getProcessName(int pid) {
	    BufferedReader reader = null;
	    try {
	        reader = new BufferedReader(new FileReader("/proc/" + pid + "/cmdline"));
	        String processName = reader.readLine();
	        if (!TextUtils.isEmpty(processName)) {
	            processName = processName.trim();
	        }
	        return processName;
	    } catch (Throwable throwable) {
	        throwable.printStackTrace();
	    } finally {
	        try {
	            if (reader != null) {
	                reader.close();
	            }
	        } catch (IOException exception) {
	            exception.printStackTrace();
	        }
	    }
	    return null;
	}
	
}









