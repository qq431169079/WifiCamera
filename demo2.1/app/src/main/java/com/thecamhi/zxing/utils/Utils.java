package com.thecamhi.zxing.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.widget.Toast;

public class Utils {
	// 鍙栧緱鐗堟湰鍙?
		public static String getVersionName(Context context) {
			try {
				PackageInfo manager = context.getPackageManager().getPackageInfo(
						context.getPackageName(), 0);
				return manager.versionName;
			} catch (NameNotFoundException e) {
				return "Unknown";
			}
		}

		// 鍙栧緱鐗堟湰鍙?
		public static int getVersionCode(Context context) {
			try {
				PackageInfo manager = context.getPackageManager().getPackageInfo(
						context.getPackageName(), 0);
				return manager.versionCode;
			} catch (NameNotFoundException e) {
				return -1;
			}
		}
		public static String getPhoneDetails(){
			return "Product Model: "
					+ android.os.Build.MODEL + ","//鑾峰彇鎵嬫満鍨嬪彿:HM NOTE 1S,
	                + android.os.Build.VERSION.SDK + ","//  SDK 鐗堟湰鍙? : 19
	                + android.os.Build.VERSION.RELEASE;//鑾峰彇鐗堟湰鍙?:4.4.4
		}
		/**
		 * @param context
		 * @param toastContent
		 */
		public static void toastUtilString(Context context ,String toastContent){
			if (toastContent==null) {
				String data=getPhoneDetails()+"\n VersionCode="+getVersionCode(context)+"\n getVersionName="+getVersionName(context);
				Toast.makeText(context, data, Toast.LENGTH_SHORT).show();
			}else {
				Toast.makeText(context, toastContent, Toast.LENGTH_SHORT).show();
			}
			 
		}
		public static void logEUtils(Context context) {
			String data=getPhoneDetails()+"\n VersionCode="+getVersionCode(context)+"\n getVersionName="+getVersionName(context);
			String packageName=context.getPackageName();
			Log.e(packageName,data );
		}
		public static void logICommon(Context context,String logStrData) {
			Log.i(context.getPackageName(), logStrData+"**");
		}
		
		
		/**
		 * 灏? dp 杞垚px
		 *  浠巇imens.xml 鑾峰彇鐨? 灏哄閮芥槸  px锛屽嵆浣夸綘鍐欑殑鏄痙p 鍜宻p
		 * @param dp
		 * @param context
		 * @return
		 */
		public static int  dp2Pix(int dp,Context context) {
			
			 float scale = context.getResources().getDisplayMetrics().density;
			 	
//			 	int widthPx=200*2;
			 int pix=(int) (dp*scale + 0.5f);
			
			return pix;
		}
		 /** 
		  * 
         * 灏唒x鍊艰浆鎹负dip鎴杁p鍊硷紝淇濊瘉灏哄澶у皬涓嶅彉 
         *  浠巇imens.xml 鑾峰彇鐨? 灏哄閮芥槸  px锛屽嵆浣夸綘鍐欑殑鏄痙p 鍜宻p
         * @param pxValue 
         * @param scale 
         *            锛圖isplayMetrics绫讳腑灞炴?ensity锛? 
         * @return 
         */  
        public static int px2dip(Context context, float pxValue) {  
            final float scale = context.getResources().getDisplayMetrics().density;  
            Log.e("scale==", scale+"*******");
            return (int) (pxValue / scale + 0.5f);  
        }  
      
        /** 
         * 灏哾ip鎴杁p鍊艰浆鎹负px鍊硷紝淇濊瘉灏哄澶у皬涓嶅彉 
         *  浠巇imens.xml 鑾峰彇鐨? 灏哄閮芥槸  px锛屽嵆浣夸綘鍐欑殑鏄痙p 鍜宻p
         * @param dipValue 
         * @param scale 
         *            锛圖isplayMetrics绫讳腑灞炴?ensity锛? 
         * @return 
         */  
        public static int dip2px(Context context, float dipValue) {  
            final float scale = context.getResources().getDisplayMetrics().density;  
            Log.e("scale==", scale+"*******");
            return (int) (dipValue * scale + 0.5f);  
        }  
      
        /** 
         * 灏唒x鍊艰浆鎹负sp鍊硷紝淇濊瘉鏂囧瓧澶у皬涓嶅彉 
         *  浠巇imens.xml 鑾峰彇鐨? 灏哄閮芥槸  px锛屽嵆浣夸綘鍐欑殑鏄痙p 鍜宻p
         * @param pxValue 
         * @param fontScale 
         *            锛圖isplayMetrics绫讳腑灞炴?caledDensity锛? 
         * @return 
         */  
        public static int px2sp(Context context, float pxValue) {  
            final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;  
            Log.e("scale==", fontScale+"*******");
            return (int) (pxValue / fontScale + 0.5f);  
        }  
      
        /** 
         * 灏唖p鍊艰浆鎹负px鍊硷紝淇濊瘉鏂囧瓧澶у皬涓嶅彉 
         *  浠巇imens.xml 鑾峰彇鐨? 灏哄閮芥槸  px锛屽嵆浣夸綘鍐欑殑鏄痙p 鍜宻p
         * @param spValue 
         * @param fontScale 
         *            锛圖isplayMetrics绫讳腑灞炴?caledDensity锛? 
         * @return 
         */  
        public static int sp2px(Context context, float spValue) {  
            final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;  
            Log.e("scale==", fontScale+"*******");
            return (int) (spValue * fontScale + 0.5f);  
        } 
}
