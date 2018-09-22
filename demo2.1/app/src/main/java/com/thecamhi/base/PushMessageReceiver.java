package com.thecamhi.base;

import android.content.Context;
import android.text.TextUtils;

import com.hichip.push.HiPushSDK;
import com.hichip.push.HiPushSDK.OnPushResult;
import com.hichip1.R;
import com.tencent.android.tpush.XGLocalMessage;
import com.tencent.android.tpush.XGPushBaseReceiver;
import com.tencent.android.tpush.XGPushClickedResult;
import com.tencent.android.tpush.XGPushConfig;
import com.tencent.android.tpush.XGPushManager;
import com.tencent.android.tpush.XGPushRegisterResult;
import com.tencent.android.tpush.XGPushShowedResult;
import com.tencent.android.tpush.XGPushTextMessage;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.utils.SharePreUtils;

import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unused")
public class PushMessageReceiver extends XGPushBaseReceiver {

	@Override
	public void onDeleteTagResult(Context arg0, int arg1, String arg2) {
	}

	@Override
	public void onNotifactionClickedResult(Context arg0, XGPushClickedResult arg1) {
	}

	@Override
	public void onNotifactionShowedResult(Context arg0, XGPushShowedResult arg1) {
	}

	@Override
	public void onRegisterResult(Context arg0, int arg1, XGPushRegisterResult arg2) {
	}

	@Override
	public void onSetTagResult(Context arg0, int arg1, String arg2) {
	}

	@Override
	public void onTextMessage(Context arg0, XGPushTextMessage arg1) {
		String key = arg1.getCustomContent();
		String uid = null;
		int type = 0;
		int time = 0;
		String rfType = null;
		if (key != null) {
			try {
				JSONObject arrJson = new JSONObject(key);
				String jsonc = arrJson.getString("content");
				JSONObject conJson = new JSONObject(jsonc);
				uid = conJson.getString("uid");
				type = conJson.getInt("type");
				if (type == 6) {// RF报警
					rfType = conJson.getString("rftype");
				}
				time = conJson.getInt("time");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			if (uid == null)
				return;
			if (HiDataValue.CameraList.size() > 0) {
				return;
			}
			DatabaseManager db = new DatabaseManager(arg0);
			if (!db.queryDeviceByUid(uid)) {
				mContext = arg0;
				mUid = uid;
				int subId = SharePreUtils.getInt("subId", arg0, uid);
				String server = " ";
				if (handSubXYZ(uid)) {
					server = HiDataValue.CAMERA_ALARM_ADDRESS_THERE;
				} else {
					server = HiDataValue.CAMERA_ALARM_ADDRESS;
				}
				pushSDK = new HiPushSDK(XGPushConfig.getToken(arg0), uid, HiDataValue.company, pushResult, server);
				if (subId > 0) {
					pushSDK.unbind(subId);
				} else {
					pushSDK.bind();
				}
				return;
			}
			String[] strAlarmType = arg0.getResources().getStringArray(R.array.tips_alarm_list_array);
			XGLocalMessage local_msg = new XGLocalMessage();
			// 设置本地消息类型，1:通知，2:消息
			local_msg.setType(1);
			// 设置消息标题
			local_msg.setTitle(uid);
			// 设置消息内容
			// if (type >= 0) local_msg.setContent(type==6?strAlarmType[4]:strAlarmType[type]);
			String msg=handType(type, rfType, strAlarmType);
			if(!TextUtils.isEmpty(msg)){
				local_msg.setContent(msg);
			}
			if (db != null) {
				db.updateAlarmStateByUID(uid, 1);
			}
			XGPushManager.addLocalNotification(arg0, local_msg);
		}

	}

	private String handType(int type, String rfType, String[] strAlarmType) {
		String msg = null;
		switch (type) {
		case 0:
			msg = strAlarmType[0];
			break;
		case 1:
			msg = strAlarmType[1];
			break;
		case 2:
			msg = strAlarmType[2];
			break;
		case 3:
			msg = strAlarmType[3];
			break;
		case 6:
			if ("key2".equals(rfType)) {
				msg = "SOS报警";
			} else if ("key3".equals(rfType)) {
				msg = "响铃报警";
			} else if ("door".equals(rfType)) {
				msg = "门磁报警";
			} else if ("infra".equals(rfType)) {
				msg = "红外报警";
			} else if ("beep".equals(rfType)) {
				msg = "门铃报警";
			} else if ("fire".equals(rfType)) {
				msg = "烟雾报警";
			} else if ("gas".equals(rfType)) {
				msg = "燃气报警";
			} else if ("socket".equals(rfType)) {
				msg = "插座报警";
			} else if ("temp".equals(rfType)) {
				msg = "温度报警";
			} else if ("humi".equals(rfType)) {
				msg = "湿度报警";
			}
			break;
		}
		return msg;
	}

	private HiPushSDK pushSDK;
	private Context mContext;
	private String mUid;

	private HiPushSDK.OnPushResult pushResult = new OnPushResult() {

		@Override
		public void pushBindResult(int subID, int type, int result) {
			if (type == HiPushSDK.PUSH_TYPE_BIND) {
				if (HiPushSDK.PUSH_RESULT_SUCESS == result) {
					if (pushSDK != null)
						pushSDK.unbind(subID);
					SharePreUtils.removeKey("subId", mContext, mUid);
				} else if (HiPushSDK.PUSH_RESULT_FAIL == result || HiPushSDK.PUSH_RESULT_NULL_TOKEN == result) {
				}
			} else if (type == HiPushSDK.PUSH_TYPE_UNBIND) {
				if (HiPushSDK.PUSH_RESULT_SUCESS == result) {
					SharePreUtils.removeKey("subId", mContext, mUid);
				} else if (HiPushSDK.PUSH_RESULT_FAIL == result) {
				}
			}
		}
	};

	@Override
	public void onUnregisterResult(Context arg0, int arg1) {
	}

	/**
	 * 处理UID前缀为XXX YYYY ZZZ
	 * 
	 * @return 如果是则返回 true
	 */
	public boolean handSubXYZ(String uid) {
		String subUid = uid.substring(0, 4);
		for (String str : HiDataValue.SUBUID) {
			if (str.equalsIgnoreCase(subUid)) {
				return true;
			}
		}
		return false;
	}

}