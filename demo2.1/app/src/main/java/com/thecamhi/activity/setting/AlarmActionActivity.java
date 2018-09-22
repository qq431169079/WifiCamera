package com.thecamhi.activity.setting;

import android.Manifest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.hichip.base.HiLog;
import com.hichip.callback.ICameraIOSessionCallback;
import com.hichip.content.HiChipDefines;
import com.hichip.control.HiCamera;
import com.hichip.data.HiDeviceInfo;
import com.hichip.widget.SwitchButton;
import com.hichip1.R;
import com.tencent.android.tpush.XGPushConfig;
import com.thecamhi.base.HiToast;
import com.thecamhi.base.HiTools;
import com.thecamhi.base.TitleView;
import com.thecamhi.base.TitleView.NavigationBarButtonListener;
import com.thecamhi.bean.CamHiDefines;
import com.thecamhi.bean.CamHiDefines.HI_P2P_ALARM_ADDRESS;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;
import com.thecamhi.bean.MyCamera.OnBindPushResult;
import com.thecamhi.main.HiActivity;

import java.util.regex.Pattern;

/**
 * 报警联动Activity
 * @author lt
 */
public class AlarmActionActivity extends HiActivity implements ICameraIOSessionCallback, com.hichip.widget.SwitchButton.OnCheckedChangeListener, OnCheckedChangeListener {
	private MyCamera mCamera;

	private HiChipDefines.HI_P2P_S_ALARM_PARAM param;
	private HiChipDefines.HI_P2P_SNAP_ALARM snapParam;
	private SwitchButton alarm_push_push_tgbtn, alarm_push_sd_video_tgbtn, alarm_push_email_alarm_tgbtn, alarm_push_save_picture_tgbtn, alarm_push_video_tgbtn;
	private RadioGroup mRgPictureNum;
	private int mPictureNum = 1;
	private final static int HANDLE_MESSAGE_BIND_SUCCESS = 0x80000001;
	private final static int HANDLE_MESSAGE_BIND_FAIL = 0x80000002;
	private final static int HANDLE_MESSAGE_UNBIND_SUCCESS = 0x80000003;
	private final static int HANDLE_MESSAGE_UNBIND_FAIL = 0x80000004;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_action_with_alarm);
		String uid = getIntent().getStringExtra(HiDataValue.EXTRAS_KEY_UID);
		for (MyCamera camera : HiDataValue.CameraList) {
			if (uid.equals(camera.getUid())) {
				mCamera = camera;
				mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_ALARM_PARAM, null);
				mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_SNAP_ALARM_PARAM, null);
				break;
			}
		}

		initView();
	}

	private void initView() {
		TitleView title = (TitleView) findViewById(R.id.title_top);
		title.setTitle(getResources().getString(R.string.title_action_with_alarm));
		title.setButton(TitleView.NAVIGATION_BUTTON_LEFT);
		title.setNavigationBarButtonListener(new NavigationBarButtonListener() {

			@Override
			public void OnNavigationButtonClick(int which) {
				switch (which) {
				case TitleView.NAVIGATION_BUTTON_LEFT:
					AlarmActionActivity.this.finish();
					break;
				}

			}
		});

		alarm_push_push_tgbtn = (SwitchButton) findViewById(R.id.alarm_push_push_tgbtn);

		if (mCamera.getPushState() > 0) {
			alarm_push_push_tgbtn.setChecked(true);
		} else {
			alarm_push_push_tgbtn.setChecked(false);
		}
		alarm_push_push_tgbtn.setOnCheckedChangeListener(this);

		alarm_push_sd_video_tgbtn = (SwitchButton) findViewById(R.id.alarm_push_sd_video_tgbtn);
		alarm_push_email_alarm_tgbtn = (SwitchButton) findViewById(R.id.alarm_push_email_alarm_tgbtn);
		alarm_push_save_picture_tgbtn = (SwitchButton) findViewById(R.id.alarm_push_save_picture_tgbtn);
		alarm_push_video_tgbtn = (SwitchButton) findViewById(R.id.alarm_push_video_tgbtn);
		initRGPicView();
		LinearLayout action_alarm_picture_num_ll = (LinearLayout) findViewById(R.id.action_alarm_picture_num_ll);
		if (mCamera.getChipVersion() == HiDeviceInfo.CHIP_VERSION_HISI && mCamera.getCommandFunction(HiChipDefines.HI_P2P_GET_SNAP_ALARM_PARAM)) {
			action_alarm_picture_num_ll.setVisibility(View.VISIBLE);
		}

	}

	private void initRGPicView() {
		mRgPictureNum = (RadioGroup) findViewById(R.id.radioGroup_alarm_action);
	}

	private void sendRegister() {
		if (mCamera.getPushState() == 1) {
			return;
		}
		if (!mCamera.getCommandFunction(CamHiDefines.HI_P2P_ALARM_TOKEN_REGIST)) {
			return;
		}

		byte[] info = CamHiDefines.HI_P2P_ALARM_TOKEN_INFO.parseContent(0, mCamera.getPushState(), (int) (System.currentTimeMillis() / 1000 / 3600), mCamera.getPushState() > 0 ? 1 : 0);
		mCamera.sendIOCtrl(CamHiDefines.HI_P2P_ALARM_TOKEN_REGIST, info);
	}

	private void sendUnRegister() {
		if (mCamera.getPushState() == 0) {
			return;
		}
		if (!mCamera.getCommandFunction(CamHiDefines.HI_P2P_ALARM_TOKEN_UNREGIST)) {
			HiLog.v("UNREGIST FUCTION: false ");
			return;
		}

		byte[] info = CamHiDefines.HI_P2P_ALARM_TOKEN_INFO.parseContent(0, mCamera.getPushState(), (int) (System.currentTimeMillis() / 1000 / 3600), mCamera.getPushState() > 0 ? 1 : 0);
		mCamera.sendIOCtrl(CamHiDefines.HI_P2P_ALARM_TOKEN_UNREGIST, info);
	}

	private OnBindPushResult bindPushResult = new OnBindPushResult() {

		@Override
		public void onBindSuccess(MyCamera camera) {
			Message msg = handler.obtainMessage();
			msg.what = HANDLE_MESSAGE_BIND_SUCCESS;
			msg.obj = camera;
			handler.sendMessage(msg);
		}

		@Override
		public void onBindFail(MyCamera camera) {
			Message msg = handler.obtainMessage();
			msg.what = HANDLE_MESSAGE_BIND_FAIL;
			handler.sendMessage(msg);
		}

		@Override
		public void onUnBindSuccess(MyCamera camera) {
			Message msg = handler.obtainMessage();
			msg.what = HANDLE_MESSAGE_UNBIND_SUCCESS;
			handler.sendMessage(msg);
		}

		@Override
		public void onUnBindFail(MyCamera camera) {
			Message msg = handler.obtainMessage();
			msg.what = HANDLE_MESSAGE_UNBIND_FAIL;
			handler.sendMessage(msg);
		}

	};

	protected void sendFTPSetting() {
		if (param == null)
			return;
		param.u32SDRec = alarm_push_sd_video_tgbtn.isChecked() ? 1 : 0;
		param.u32EmailSnap = alarm_push_email_alarm_tgbtn.isChecked() ? 1 : 0;
		param.u32FtpSnap = alarm_push_save_picture_tgbtn.isChecked() ? 1 : 0;
		param.u32FtpRec = alarm_push_video_tgbtn.isChecked() ? 1 : 0;
		showjuHuaDialog();
		mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_ALARM_PARAM, param.parseContent());
		if (snapParam == null)
			return;
		snapParam.u32Number = mPictureNum;
		snapParam.u32Interval = snapParam.u32Interval < 5 ? 5 : snapParam.u32Interval;
		mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_SNAP_ALARM_PARAM, snapParam.parseContent());

	}

	@Override
	public void receiveIOCtrlData(HiCamera arg0, int arg1, byte[] arg2, int arg3) {
		if (arg0 != mCamera)
			return;
		Bundle bundle = new Bundle();
		bundle.putByteArray(HiDataValue.EXTRAS_KEY_DATA, arg2);
		Message msg = handler.obtainMessage();
		msg.what = HiDataValue.HANDLE_MESSAGE_RECEIVE_IOCTRL;
		msg.obj = arg0;
		msg.arg1 = arg1;
		msg.arg2 = arg3;
		msg.setData(bundle);
		handler.sendMessage(msg);

	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HANDLE_MESSAGE_BIND_SUCCESS:
				MyCamera myCamera = (MyCamera) msg.obj;
				dismissjuHuaDialog();
				if (!myCamera.handSubXYZ()) {
					myCamera.setServerData(HiDataValue.CAMERA_ALARM_ADDRESS);
				} else {
					myCamera.setServerData(HiDataValue.CAMERA_ALARM_ADDRESS_THERE);
				}
				mCamera.updateInDatabase(AlarmActionActivity.this);
				sendServer(myCamera);
				sendRegister();
				break;
			case HANDLE_MESSAGE_BIND_FAIL:
				HiToast.showToast(AlarmActionActivity.this, getString(R.string.tip_open_faild));
				alarm_push_push_tgbtn.setChecked(false);
				mChecked = true;
				dismissjuHuaDialog();
				mCamera.updateInDatabase(AlarmActionActivity.this);
				break;
			case HANDLE_MESSAGE_UNBIND_SUCCESS:
				sendUnRegister();
				mCamera.setPushState(HiDataValue.DEFAULT_PUSH_STATE);
				dismissjuHuaDialog();
				mCamera.updateInDatabase(AlarmActionActivity.this);
				break;
			case HANDLE_MESSAGE_UNBIND_FAIL:
				HiToast.showToast(AlarmActionActivity.this, getString(R.string.tip_close_failed));
				alarm_push_push_tgbtn.setChecked(true);
				mChecked = true;
				dismissjuHuaDialog();
				mCamera.updateInDatabase(AlarmActionActivity.this);
				break;

			case HiDataValue.HANDLE_MESSAGE_RECEIVE_IOCTRL: {
				if (msg.arg2 == 0) {
					Bundle bundle = msg.getData();
					byte[] data = bundle.getByteArray(HiDataValue.EXTRAS_KEY_DATA);
					switch (msg.arg1) {
					case HiChipDefines.HI_P2P_GET_ALARM_PARAM:
						param = new HiChipDefines.HI_P2P_S_ALARM_PARAM(data);
						alarm_push_sd_video_tgbtn.setChecked(param.u32SDRec == 1 ? true : false);
						alarm_push_sd_video_tgbtn.setOnCheckedChangeListener(AlarmActionActivity.this);
						alarm_push_email_alarm_tgbtn.setChecked(param.u32EmailSnap == 1 ? true : false);
						alarm_push_email_alarm_tgbtn.setOnCheckedChangeListener(AlarmActionActivity.this);
						alarm_push_save_picture_tgbtn.setChecked(param.u32FtpSnap == 1 ? true : false);
						alarm_push_save_picture_tgbtn.setOnCheckedChangeListener(AlarmActionActivity.this);
						alarm_push_video_tgbtn.setChecked(param.u32FtpRec == 1 ? true : false);
						alarm_push_video_tgbtn.setOnCheckedChangeListener(AlarmActionActivity.this);
						break;
					case HiChipDefines.HI_P2P_GET_SNAP_ALARM_PARAM:
						snapParam = new HiChipDefines.HI_P2P_SNAP_ALARM(data);
						((RadioButton) mRgPictureNum.getChildAt(snapParam.u32Number - 1)).setChecked(true);
						mRgPictureNum.setOnCheckedChangeListener(AlarmActionActivity.this);
						break;
					case HiChipDefines.HI_P2P_SET_ALARM_PARAM:
						mCamera.updateInDatabase(AlarmActionActivity.this);
						dismissjuHuaDialog();
						break;
					case HiChipDefines.HI_P2P_SET_SNAP_ALARM_PARAM:
						dismissjuHuaDialog();
						break;
					case CamHiDefines.HI_P2P_ALARM_TOKEN_UNREGIST:

						break;
					case HiChipDefines.HI_P2P_IPCRF_ALARM_GET:
						break;
					case HiChipDefines.HI_P2P_IPCRF_ALARM_SET:
						dismissjuHuaDialog();
						break;

					}
				} else {
					switch (msg.arg1) {
					case HiChipDefines.HI_P2P_SET_ALARM_PARAM:
						HiToast.showToast(AlarmActionActivity.this, getResources().getString(R.string.alarm_action_save_failed));
						break;
					}
				}
			}
			}
		}
	};

	@Override
	public void receiveSessionState(HiCamera arg0, int arg1) {
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mCamera != null) {
			mCamera.registerIOSessionListener(this);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mCamera != null) {
			mCamera.unregisterIOSessionListener(this);
		}
	}

	private boolean mChecked = false;

	/**
	 * SwitchButton 的监听
	 * @param view
	 * @param isChecked
	 */
	@Override
	public void onCheckedChanged(CompoundButton view, boolean isChecked) {
		switch (view.getId()) {
		case R.id.alarm_push_push_tgbtn:
			if (mChecked) {
				mChecked = false;
				return;
			}
			if (HiDataValue.XGToken == null) {
				if (HiDataValue.ANDROID_VERSION >= 6) {
					if (!HiTools.checkPermission(AlarmActionActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
						ActivityCompat.requestPermissions(AlarmActionActivity.this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
					}
				}
				HiDataValue.XGToken = XGPushConfig.getToken(AlarmActionActivity.this);
				if (HiDataValue.XGToken == null) {
					view.setChecked(!view.isChecked());
					// 修改提示语：信鸽获取异常请稍后重试
					HiToast.showToast(AlarmActionActivity.this, "null token");
					return;
				}
			}
			showjuHuaDialog();
			mCamera.bindPushState(view.isChecked(), bindPushResult);
			break;
		case R.id.alarm_push_sd_video_tgbtn:
			sendFTPSetting();
			break;
		case R.id.alarm_push_email_alarm_tgbtn:
			sendFTPSetting();
			break;
		case R.id.alarm_push_save_picture_tgbtn:
			sendFTPSetting();
			break;
		case R.id.alarm_push_video_tgbtn:
			sendFTPSetting();
			break;
		}
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		switch (checkedId) {
		case R.id.radio_one:
			mPictureNum = 1;
			sendFTPSetting();
			break;
		case R.id.radio_two:
			mPictureNum = 2;
			sendFTPSetting();
			break;
		case R.id.radio_there:
			mPictureNum = 3;
			sendFTPSetting();
			break;
		}
	}
	
	protected void sendServer(MyCamera mCamera) {
		// //测试
		// mCamera.sendIOCtrl(CamHiDefines.HI_P2P_ALARM_ADDRESS_GET, null);
		if (mCamera.getServerData() == null) {
			mCamera.setServerData(HiDataValue.CAMERA_ALARM_ADDRESS);
			mCamera.updateServerInDatabase(this);
		}
		if (!mCamera.getCommandFunction(CamHiDefines.HI_P2P_ALARM_ADDRESS_SET)) {
			return;
		}
		if (mCamera.push != null) {
			String[] strs = mCamera.push.getPushServer().split("\\.");
			if (strs.length == 4&&isInteger(strs[0])&&isInteger(strs[1])&&isInteger(strs[2])&&isInteger(strs[3])) {
				byte[] info = HI_P2P_ALARM_ADDRESS.parseContent(mCamera.push.getPushServer());
				mCamera.sendIOCtrl(CamHiDefines.HI_P2P_ALARM_ADDRESS_SET, info);
			}

		}
	}
	/*
	 *推荐，速度最快 判断是否为整数
	 * @param str 传入的字符串
	 * @return 是整数返回true,否则返回false
	 */

	public static boolean isInteger(String str) {
		Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
		return pattern.matcher(str).matches();
	}
}
