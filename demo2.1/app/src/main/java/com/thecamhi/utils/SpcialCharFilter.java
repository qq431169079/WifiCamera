package com.thecamhi.utils;

import android.content.Context;
import android.text.InputFilter;
import android.text.Spanned;

import com.hichip1.R;
import com.thecamhi.base.HiToast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EditText值能输入如下字符
 * @author lt
 *
 */
public class SpcialCharFilter implements InputFilter {
	private Context mContext;
	public SpcialCharFilter(Context mContext){
		this.mContext=mContext;
	}

	@Override
	public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
		   String regexStr="[0-9a-zA-Z`~!@#$%^&*()-_+=[{]}:;'\"|\\,<>.?/.￥﹉…^$..€]+";
		   //String regexStr = "[1234567890qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]+";  
           Pattern pattern = Pattern.compile(regexStr);  
           Matcher matcher = pattern.matcher(source.toString().trim()); 
           
           if (matcher.matches()) {  
               return null;  
           } else {
        	   if(start!=end){
        		   HiToast.showToast(mContext,mContext.getString(R.string.tip_not_spcialchar));
        	   }
               return "";  
           }  

       }  

}
