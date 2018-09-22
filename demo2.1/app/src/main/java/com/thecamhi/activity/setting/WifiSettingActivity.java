package com.thecamhi.activity.setting;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hichip.activity.WifiSetToDeviceActivity;
import com.hichip.callback.ICameraIOSessionCallback;
import com.hichip.content.HiChipDefines;
import com.hichip.control.HiCamera;
import com.hichip.tools.Packet;
import com.hichip1.R;
import com.thecamhi.base.HiToast;
import com.thecamhi.base.TitleView;
import com.thecamhi.base.TitleView.NavigationBarButtonListener;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;
import com.thecamhi.main.HiActivity;
import com.thecamhi.main.MainActivity;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/**
 * WIFI设置窗口
 */
public class WifiSettingActivity extends HiActivity implements ICameraIOSessionCallback, OnItemClickListener, OnClickListener {

	public static final int SET_WIFI_END = 0X999;
	private MyCamera mCamera;
	private TextView wifi_setting_wifi_name;
	private TextView wifi_setting_safety;
	private Button manager_wifi_btn;

	private String[] videoApenc;
	private List<HiChipDefines.SWifiAp> wifi_list = Collections.synchronizedList(new ArrayList<HiChipDefines.SWifiAp>());
	HiChipDefines.HI_P2P_S_WIFI_PARAM wifi_param;
	private String ssid = " ";
	boolean send;
	private String uid;
	public  static final String WIFISELECTMODE = "WIFISELECTMODE";
	public  static final String WIFISELECTENCTYPE = "WIFISELECTENCTYPE";
	private RelativeLayout mRlCurrent;
	protected byte mode;
	protected byte enType;
	protected byte[] mPassWord;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wifi_setting);

		uid = getIntent().getStringExtra(HiDataValue.EXTRAS_KEY_UID);

		for (MyCamera camera : HiDataValue.CameraList) {
			if (uid.equals(camera.getUid())) {
				mCamera = camera;
				mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_WIFI_PARAM, null);
				break;
			}
		}

		initView();

	}

	private void initView() {

		TitleView title = (TitleView) findViewById(R.id.title_top);
		title.setTitle(getResources().getString(R.string.title_wifi_setting));
		title.setButton(TitleView.NAVIGATION_BUTTON_LEFT);
		title.setNavigationBarButtonListener(new NavigationBarButtonListener() {

			@Override
			public void OnNavigationButtonClick(int which) {
				switch (which) {
				case TitleView.NAVIGATION_BUTTON_LEFT:
					WifiSettingActivity.this.finish();
					break;
				}
			}
		});

		wifi_setting_wifi_name = (TextView) findViewById(R.id.wifi_setting_wifi_name);
		wifi_setting_safety = (TextView) findViewById(R.id.wifi_setting_safety);

		// wifi_setting_signal_intensity_ll=(LinearLayout)findViewById(R.id.wifi_setting_signal_intensity_ll);
		// wifi_setting_signal_intensity=(TextView)findViewById(R.id.wifi_setting_signal_intensity);

		// wifi_setting_wifi_state=(TextView)findViewById(R.id.wifi_setting_wifi_state);

		manager_wifi_btn = (Button) findViewById(R.id.manager_wifi_btn);
		manager_wifi_btn.setOnClickListener(this);
		videoApenc = getResources().getStringArray(R.array.video_apenc);
		mRlCurrent = (RelativeLayout) findViewById(R.id.rl_current);
		mRlCurrent.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(WifiSettingActivity.this, WifiSetToDeviceActivity.class);
				intent.putExtra(HiDataValue.EXTRAS_KEY_DATA, ssid);
				intent.putExtra(HiDataValue.EXTRAS_KEY_UID, uid);
				intent.putExtra(WIFISELECTMODE, mode);
				intent.putExtra(WIFISELECTENCTYPE, enType);
				intent.putExtra("password", mPassWord);
				startActivity(intent);
			}

		});

	}

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HiDataValue.HANDLE_MESSAGE_RECEIVE_IOCTRL: {
				if (msg.arg2 == 0) {
					// MyCamera camera = (MyCamera)msg.obj;
					Bundle bundle = msg.getData();
					byte[] data = bundle.getByteArray(HiDataValue.EXTRAS_KEY_DATA);
					switch (msg.arg1) {
					// 获取wifi信息回调
					case HiChipDefines.HI_P2P_GET_WIFI_PARAM:
						wifi_param = new HiChipDefines.HI_P2P_S_WIFI_PARAM(data);
						ssid = new String(wifi_param.strSSID);
						mPassWord = wifi_param.strKey;
						mode = wifi_param.Mode;
						enType = wifi_param.EncType;
						String safe = videoApenc[wifi_param.EncType];
						if (TextUtils.isEmpty(ssid.trim())) {
							wifi_setting_wifi_name.setText(getString(R.string.tip_no));
							wifi_setting_safety.setVisibility(View.GONE);
						} else {
							wifi_setting_wifi_name.setText(ssid);
							wifi_setting_safety.setText(safe);

						}

						break;
					// 获取wifi列表回调
					case HiChipDefines.HI_P2P_GET_WIFI_LIST:

						int cnt = Packet.byteArrayToInt_Little(data, 0);
						int size = HiChipDefines.SWifiAp.getTotalSize();
						wifi_list.clear();
						if (cnt > 0 && data.length >= 40) {
							int pos = 4;
							String string = "";
							for (int i = 0; i < cnt; i++) {
								byte[] bty_ssid = new byte[32];
								System.arraycopy(data, i * size + pos, bty_ssid, 0, 32);
								byte mode = data[i * size + pos + 32];
								byte enctype = data[i * size + pos + 33];
								byte signal = data[i * size + pos + 34];
								byte status = data[i * size + pos + 35];
								try {
									string = new String(bty_ssid, "UTF-8");
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
								if (!string.equals(ssid) && string.trim().getBytes().length <= 32) {
									wifi_list.add(new HiChipDefines.SWifiAp(string.trim().getBytes(), mode, enctype, signal, status));
								}
							}
						}
						if (wifi_list.size() > 0) {
							setListView();
						} else {
							HiToast.showToast(WifiSettingActivity.this, getString(R.string.no_wifi));
						}
						dismissLoadingProgress();

						break;

					case HiChipDefines.HI_P2P_SET_WIFI_PARAM: {
						dismissLoadingProgress();
						Intent intentBroadcast = new Intent();
						intentBroadcast.setAction(HiDataValue.ACTION_CAMERA_INIT_END);
						sendBroadcast(intentBroadcast);
						Intent intent = new Intent(WifiSettingActivity.this, MainActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent);

					}
						break;

					}
				} else {
					switch (msg.arg1) {
					case HiChipDefines.HI_P2P_SET_WIFI_PARAM: {

						dismissLoadingProgress();
						HiToast.showToast(WifiSettingActivity.this, getString(R.string.tips_wifi_connect_success));
					}
						break;
					}
				}
			}
				break;
			case SET_WIFI_END:
				if (!send) {
					return;
				}
				dismissLoadingProgress();
				HiToast.showToast(WifiSettingActivity.this, getResources().getString(R.string.tips_wifi_connect_success));
				break;

			}
		}

	};

	private void setListView() {
		ListView wifiList = (ListView) findViewById(R.id.wifi_setting_wifi_list);
		wifiList.setVisibility(View.VISIBLE);
		WiFiListAdapter wifiAdapter = new WiFiListAdapter(this);
		wifiList.setAdapter(wifiAdapter);
		wifiList.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

		startToDeviceActivity(wifi_list.get(position));
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

	@Override
	public void receiveSessionState(HiCamera arg0, int arg1) {
		// TODO Auto-generated method stub

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
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.manager_wifi_btn:
			showLoadingProgress();
			mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_WIFI_LIST, new byte[0]);

			break;

		default:
			break;
		}

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		send = false;
	}

	public class WiFiListAdapter extends BaseAdapter {

		private LayoutInflater mInflater;

		public WiFiListAdapter(Context context) {
			this.mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return wifi_list.size();
		}

		@Override
		public Object getItem(int position) {
			return wifi_list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@SuppressLint("InflateParams")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final HiChipDefines.SWifiAp wifi = wifi_list.get(position);

			if (wifi == null)
				return null;

			ViewHolder holder = null;

			if (convertView == null) {

				convertView = mInflater.inflate(R.layout.list_setting_wifi, null);

				holder = new ViewHolder();
				holder.txt_ssid = (TextView) convertView.findViewById(R.id.wifi_setting_item_ssid);
				holder.txt_safety = (TextView) convertView.findViewById(R.id.wifi_setting_item_safety);
				holder.txt_intensity = (TextView) convertView.findViewById(R.id.wifi_setting_item_signal_intensity);
				// holder.txt_state=(TextView)convertView.findViewById(R.id.wifi_setting_item_state);
				convertView.setTag(holder);

			} else {

				holder = (ViewHolder) convertView.getTag();
			}

			if (holder != null) {
				try {
					holder.txt_ssid.setText(new String(wifi.strSSID, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				// holder.txt_ssid.setText(Packet.getString(wifi.strSSID));
				holder.txt_safety.setText(videoApenc[wifi.EncType]);
				String strSignal = wifi.Signal + "" + "%";
				holder.txt_intensity.setText(strSignal);
			}

			return convertView;

		}

		public final class ViewHolder {
			public TextView txt_ssid;
			public TextView txt_safety;
			public TextView txt_intensity;
			// public TextView txt_state;
		}

	}

	private void startToDeviceActivity(HiChipDefines.SWifiAp wifi) {
		String string = new String(wifi.strSSID);
		Intent intent = new Intent(WifiSettingActivity.this, WifiSetToDeviceActivity.class);
		intent.putExtra(HiDataValue.EXTRAS_KEY_DATA, string);
		intent.putExtra(HiDataValue.EXTRAS_KEY_UID, uid);
		intent.putExtra(WIFISELECTMODE, wifi.Mode);
		intent.putExtra(WIFISELECTENCTYPE, wifi.EncType);
		startActivity(intent);
	}
}
