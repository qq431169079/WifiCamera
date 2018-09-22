package com.thecamhi.zxing.utils;

import android.text.TextUtils;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EllpsizeUtils_DingChang {

	/**
	 * 瀛楃涓茬殑鍗曚釜瀛楃鏄惁鏄眽瀛?
	 * 
	 * @param c
	 *            鍗曚釜瀛楃
	 * @return 瀛楃瀵瑰簲鐨凙SCIIS 鍊硷紝 璐熷?? 鏄眽瀛楋紱
	 */
	public static int ascii(String c) {
		byte x[] = new byte[2];// 杩欓噷鏄袱涓厓绱?
		x = c.getBytes();// 鎸夌収鍘熸湁鐨? 缂栫爜鏍煎紡鐢熸垚瀛楄妭鏁扮粍锛?

		// x=c.getBytes("utf-8");// 鎸夌収浠?涔堢紪鐮佹牸寮忕敓鎴? 瀛楄妭鏁扮粍锛?
		// x=c.getBytes(srcBegin, srcEnd, dst, dstBegin);

		if (x == null || x.length > 2 || x.length <= 0) {// 娌℃湁瀛楃锛屼负绌哄瓧绗︿覆锛堢┖鏍间篃鏄瓧绗︿覆锛?
			return -1;
		}
		if (x.length == 1) {// 鑻辨枃瀛楃
			return 1;
		}
		Pattern p=Pattern.compile("[\u4e00-\u9fa5]");
	    Matcher m=p.matcher(c);
	     if(m.matches()){
//	      Toast.makeText(Main.this,"杈撳叆鐨勬槸姹夊瓧", Toast.LENGTH_SHORT).show();
	    	 return -1;
	     }

		return 0;
	}

	/**
	 * 鍘绘帀棣栦綅绌烘牸锛屽仛鍏跺畠澶勭悊
	 * 
	 * @param string
	 * @return
	 */
	public static String goodStr(String string) {

		string = string.trim();
		return string;
	}
	/**
	 * 鍒ゆ柇 鏈夊灏戜釜 姹夊瓧  闀垮害 鍙栨暣鏁?
	 * @param string
	 * @return
	 */
	public static int letterSum(String string) {
		if (null != string) {
			string = goodStr(string);// 杩欎釜鍑芥暟鏄共浠?涔堢敤澶勭殑锛熷幓鎺? 棣栦綅绌烘牸
			if (string.length() <= 0) {
				return 0;
			} else {
				String str;
				double len = 0;
				for (int i = 0; i < string.length(); i++) {
					// 鏄惁鏄眽瀛? ascii<0;
					str = string.substring(i, i + 1);
					if (ascii(str) < 0) {
						len++;
					} else {
						len += 0.5;
					}
					
				}
				Log.e("num", (int) Math.round(len)+";  len="+ len);
				return (int) Math.round(len);
			}
		}
		return 0;

	}
	
	/**
	 * 鍒ゆ柇鏈夊嚑涓眽瀛楋紝涓嶆槸闀垮害锛? 鍙互淇敼鎴愶細 鑻辨枃瀛楃鏈夊嚑涓?
	 * @param string
	 * @return
	 */
	public static int chineseSum(String string) {
		if (!TextUtils.isEmpty(string)) {//瀛楃涓插唴瀹逛笉涓嶄负绌?
			string = goodStr(string);// 杩欎釜鍑芥暟鏄共浠?涔堢敤澶勭殑锛熷幓鎺? 棣栦綅绌烘牸
			if (string.length() <= 0) {
				return 0;
			} else {
				String str;
				double len = 0;
				for (int i = 0; i < string.length(); i++) {
					// 鏄惁鏄眽瀛? ascii<0;
					str = string.substring(i, i + 1);
					if (ascii(str) < 0) {//鏄眽瀛?
						len++;
					}
//					else {//涓嶆槸姹夊瓧
//						len += 0.5;
//					}
					
				}
				Log.e("num", (int) Math.round(len)+";  len="+ len);
				return (int) Math.round(len);
			}
		}
		return 0;
	}
	/**
	 * 鑾峰彇 澶氬皯鐨? 瀛楃涓?
	 * 
	 * @param string
	 *            瀛楃涓叉暟鎹紝
	 * @param size
	 *            瑕佽幏鍙栫殑闀垮害 ( 鏄暱搴? 涓嶆槸瀛楃涓暟锛屾槸闀垮害)锛氫腑鏂囦负涓?涓紝鑻辨枃涓?0.5涓?
	 *            娉ㄦ剰锛氬亣璁炬湁鍗佷釜闀垮害锛氬叏涓轰腑鏂囷紝鍒欎负10涓眽瀛楋紱20涓瓧姣嶏紝 鑻ユ煇瀛楃涓插瓧绗︽暟灏忎簬 10锛屽垯璇ュ瓧绗︿覆娌℃湁杈惧埌鐪佺暐瑕佹眰锛?
	 * @return
	 */
	public static String limitStr(String string, int size) {// 瑕佸闀跨殑 瀛楃涓?
		if (null != string) {
			string = goodStr(string);// 杩欎釜鍑芥暟鏄共浠?涔堢敤澶勭殑锛熷幓鎺? 棣栦綅绌烘牸
			if (string.length() <= size) {
				return string;
			} else {
				StringBuffer buffer = new StringBuffer();
				String str;
				double len = 0;
				for (int i = 0; i < string.length(); i++) {
					// 鏄惁鏄眽瀛? ascii<0;
					str = string.substring(i, i + 1);
					if (ascii(str) < 0) {
						buffer.append(str);
						len++;
					} else {
						buffer.append(str);
						len += 0.5;
					}
					if (len >= size)
						break;
				}
				return buffer.toString();
			}
		}
		return "";

	}

	/**
	 * 鑾峰彇 浠ョ壒瀹? 瀛楃涓瞖ndStr 涓虹粨灏剧殑瀛楃涓诧紝 
	 * 
	 * @param strData 瀛楃涓叉暟鎹?
	 * @param size
	 * @param endStr
	 * @return  杩斿洖浠ョ壒瀹氱渷鐣ョ鍙蜂负缁撳熬鐨勫瓧绗︿覆
	 */
	public static String limitStr_Ending(String strData, int size, String endStr) {
		strData = goodStr(strData);//鍘绘帀棣栦綅绌烘牸
		if (size < endStr.length() || strData.length() < endStr.length()) {// 缁撳熬鐨勫瓧绗︿覆杩囬暱锛屽瓙浠?
																			// 缁撳熬鐨勫瓧绗︿覆涓轰富
			Log.e("endStr is too long","endStr is too long! Please cut it.");
		}
		String  cutStr;
		cutStr = limitStr(strData, size);
		if (cutStr.length()!=strData.length()) {//濡傛灉瀛楃涓茶瑁佸噺浜? 鍒欐墽琛屼笅闈㈡搷浣?
			cutStr = cutStr.substring(0, cutStr.length() - letterSum(endStr))+ endStr;
		}

		return cutStr;
	}
}
