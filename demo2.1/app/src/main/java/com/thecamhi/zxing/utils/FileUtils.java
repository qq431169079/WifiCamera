package com.thecamhi.zxing.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;

public class FileUtils {
	/**
	 * 鑾峰彇app 鐨勭紦瀛樼洰褰?
	 * 
	 * @param context
	 * @return
	 */
	public static String getCacheDir(Context context) {

		File cacheDir = context.getCacheDir();// 鏂囦欢鎵?鍦ㄧ洰褰曚负getFilesDir();
		String cachePath = cacheDir.getPath();
		return cachePath;
	}
	/**
     * 鑾峰彇 app 鏂囦欢瀛樺偍鏍圭洰褰? 
     * @param context
     * @return
     */
	public static String getFileRoot(Context context) {  
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {  
            File external = context.getExternalFilesDir(null);  
            if (external != null) {  
                return external.getAbsolutePath();  
            }  
        }  
        return context.getFilesDir().getAbsolutePath();  
    } 
	 
    /**
     * 鑾峰緱涓?涓叏绉拌矾寰?  鐨勮矾寰?
     * @param fileName  鏂囦欢鍚?+鍚庣紑
     * @return
     * 澶栭儴瀛樿串璺緞  String filePath = Environment.getExternalStorageDirectory() + File.separator + "test.jpg"; 
     * 鍐呴儴缂撳瓨	  String dir = FileUtils.getCacheDir(context) + "Image" + File.separator+"test.jpg";
     * 
     */
    public static String getFileAllPath(String fileName){
    	String filePath = Environment.getExternalStorageDirectory().getPath() + File.separator + fileName;
    	return filePath;
    }
	
}
