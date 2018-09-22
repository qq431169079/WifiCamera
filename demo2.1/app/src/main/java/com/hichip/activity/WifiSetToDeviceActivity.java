package com.hichip.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.hichip.callback.ICameraIOSessionCallback;
import com.hichip.content.HiChipDefines;
import com.hichip.control.HiCamera;
import com.hichip.sdk.HiChipP2P;
import com.hichip1.R;
import com.thecamhi.activity.setting.WifiSettingActivity;
import com.thecamhi.base.HiToast;
import com.thecamhi.base.TitleView;
import com.thecamhi.base.TitleView.NavigationBarButtonListener;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;
import com.thecamhi.main.HiActivity;
import com.thecamhi.main.MainActivity;
import com.thecamhi.utils.EmojiFilter;
/**
 * WIFI设置 二级界面
 * @author lt
 */
public class WifiSetToDeviceActivity extends HiActivity implements ICameraIOSessionCallback {
	private TitleView mTitleView;
	private EditText mEtPw;
	private Button mBtnApplication;
	private TextView mTvSsid;
	private String mSsid;
	private String mUid;
	private MyCamera mCamera;
	protected byte wifiSelectMode;
	protected byte wifiSelectEncType;
	protected boolean send;
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case WifiSettingActivity.SET_WIFI_END:
				if (!send) {
					return;
				}
				dismissLoadingProgress();
				break;
			case HiDataValue.HANDLE_MESSAGE_RECEIVE_IOCTRL:
				if (msg.arg2 == 0) {
					Bundle bundle = msg.getData();
					byte[] data = bundle.getByteArray(HiDataValue.EXTRAS_KEY_DATA);
					switch (msg.arg1) {
					case HiChipDefines.HI_P2P_SET_WIFI_PARAM:
						dismissLoadingProgress();
						Intent intentBroadcast = new Intent();
						intentBroadcast.setAction(HiDataValue.ACTION_CAMERA_INIT_END);
						sendBroadcast(intentBroadcast);
						Intent intent = new Intent(WifiSetToDeviceActivity.this, MainActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent);
						break;
					}
				} else {
					switch (msg.arg1) {
					case HiChipDefines.HI_P2P_SET_WIFI_PARAM: {
						dismissLoadingProgress();
						Intent intentBroadcast = new Intent();
						intentBroadcast.setAction(HiDataValue.ACTION_CAMERA_INIT_END);
						sendBroadcast(intentBroadcast);
						Intent intent = new Intent(WifiSetToDeviceActivity.this, MainActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent);
					}
						break;
					}
				}
			}
		}
	};
	private byte[] mPassWord;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wifi_set_to_device);
		getIntentData();
		initView();
		initData();
		setListerners();
	}

	private void getIntentData() {
		mSsid = getIntent().getStringExtra(HiDataValue.EXTRAS_KEY_DATA);
		mUid = getIntent().getStringExtra(HiDataValue.EXTRAS_KEY_UID);
		wifiSelectMode = getIntent().getByteExtra(WifiSettingActivity.WIFISELECTMODE, (byte) 0);
		wifiSelectEncType = getIntent().getByteExtra(WifiSettingActivity.WIFISELECTENCTYPE, (byte) 0);
		mPassWord=getIntent().getByteArrayExtra("password");
	}

	private void setListerners() {
		mBtnApplication.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String ssid = mSsid.trim();
				String psw = mEtPw.getText().toString().trim();
				if (psw.getBytes().length > 64) {
					HiToast.showToast(WifiSetToDeviceActivity.this, getString(R.string.tips_input_tolong));
					return;
				}
				mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_WIFI_PARAM,
						HiChipDefines.HI_P2P_S_WIFI_PARAM.parseContent(HiChipP2P.HI_P2P_SE_CMD_CHN, 0, wifiSelectMode,
								wifiSelectEncType, ssid.getBytes(), psw.getBytes()));
				showLoadingProgress();
				send = true;
				mHandler.sendEmptyMessageDelayed(WifiSettingActivity.SET_WIFI_END, 10000);
			}
		});

	}

	private void initData() {
		for (MyCamera camera : HiDataValue.CameraList) {
			if (mUid.equals(camera.getUid())) {
				mCamera = camera;
				break;
			}
		}
		mTvSsid.setText(mSsid);
	}

	private void initView() {
		mTitleView = (TitleView) findViewById(R.id.wifitode_title_top);
		mTitleView.setTitle(getString(R.string.title_wifi_setting));
		mTitleView.setButton(TitleView.NAVIGATION_BUTTON_LEFT);
		mTitleView.setNavigationBarButtonListener(new NavigationBarButtonListener() {

			@Override
			public void OnNavigationButtonClick(int which) {
				switch (which) {
				case TitleView.NAVIGATION_BUTTON_LEFT:
					WifiSetToDeviceActivity.this.finish();
					break;
				}
			}
		});
		mEtPw = (EditText) findViewById(R.id.wifi_to_device_pw);
//		mEtPw.setFilters(new InputFilter[] { new InputFilter.LengthFilter(31),
//				new SpcialCharFilter(WifiSetToDeviceActivity.this), new EmojiFilter() });
		mEtPw.setFilters(new InputFilter[] {new InputFilter.LengthFilter(31),new EmojiFilter() });
		mBtnApplication = (Button) findViewById(R.id.wifi_to_devide_application);
		mTvSsid = (TextView) findViewById(R.id.wifi_to_device_tvSsid);
		if(mPassWord!=null&&mPassWord.length>0){
			String string=new String(mPassWord).trim();
			mEtPw.setText(string);
			mEtPw.setTransformationMethod(PasswordTransformationMethod.getInstance());
			//mEtPw.setSelection(string.length());//去掉光标定位,解决bugly上面可疑bug。
		}

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

	@Override
	protected void onDestroy() {
		super.onDestroy();
		send = false;
	}

	@Override
	public void receiveIOCtrlData(HiCamera arg0, int arg1, byte[] arg2, int arg3) {
		if (arg0 != mCamera)
			return;

		Bundle bundle = new Bundle();
		bundle.putByteArray(HiDataValue.EXTRAS_KEY_DATA, arg2);
		Message msg = mHandler.obtainMessage();
		msg.what = HiDataValue.HANDLE_MESSAGE_RECEIVE_IOCTRL;
		msg.obj = arg0;
		msg.arg1 = arg1;
		msg.arg2 = arg3;
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	@Override
	public void receiveSessionState(HiCamera arg0, int arg1) {

	}
}




