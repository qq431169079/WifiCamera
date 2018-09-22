package com.thecamhi.main;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.customview.dialog.Effectstype;
import com.customview.dialog.NiftyDialogBuilder;
import com.hichip.activity.FishEye.FishEyeActivity;
import com.hichip.activity.Share.SeleShaCameraListActivity;
import com.hichip.base.HiLog;
import com.hichip.callback.ICameraIOSessionCallback;
import com.hichip.content.HiChipDefines;
import com.hichip.control.HiCamera;
import com.hichip1.R;
import com.tencent.android.tpush.XGPushConfig;
import com.thecamhi.activity.AddCameraActivity;
import com.thecamhi.activity.EditCameraActivity;
import com.thecamhi.activity.LiveViewActivity;
import com.thecamhi.activity.setting.AliveSettingActivity;
import com.thecamhi.base.DatabaseManager;
import com.thecamhi.base.HiToast;
import com.thecamhi.base.HiTools;
import com.thecamhi.base.TitleView;
import com.thecamhi.base.TitleView.NavigationBarButtonListener;
import com.thecamhi.bean.CamHiDefines;
import com.thecamhi.bean.CamHiDefines.HI_P2P_ALARM_ADDRESS;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;
import com.thecamhi.bean.MyCamera.OnBindPushResult;
import com.thecamhi.utils.BitmapUtils;
import com.thecamhi.utils.SharePreUtils;
import com.thecamhi.widget.swipe.SwipeMenu;
import com.thecamhi.widget.swipe.SwipeMenuCreator;
import com.thecamhi.widget.swipe.SwipeMenuItem;
import com.thecamhi.widget.swipe.SwipeMenuListView;
import com.thecamhi.widget.swipe.SwipeMenuListView.OnMenuItemClickListener;

import java.io.File;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class CameraFragment extends HiFragment implements ICameraIOSessionCallback, OnItemClickListener {
	private View layoutView;
	private static final int MOTION_ALARM = 0; // �ƶ����
	private static final int IO_ALARM = 1; // ���ñ���
	private static final int AUDIO_ALARM = 2; // ��������
	private static final int UART_ALARM = 3; // ���ñ���

	private CameraListAdapter adapter;
	private CameraBroadcastReceiver receiver;
	private SwipeMenuListView mListView;

	private String[] str_state;
	private boolean delModel = false;
	int ranNum;
	private TitleView titleView;

	HiThreadConnect connectThread = null;

	public interface OnButtonClickListener {
		void onButtonClick(int btnId, MyCamera camera);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (receiver == null) {
			receiver = new CameraBroadcastReceiver();
			IntentFilter filter = new IntentFilter();
			filter.addAction(HiDataValue.ACTION_CAMERA_INIT_END);
			getActivity().registerReceiver(receiver, filter);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		layoutView = inflater.inflate(R.layout.fragment_camera, null);
		initView();
		ranNum = (int) (Math.random() * 10000);
		return layoutView;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		SwipeMenuCreator creator = new SwipeMenuCreator() {
			@Override
			public void create(SwipeMenu menu) {

				SwipeMenuItem deleteItem = new SwipeMenuItem(getActivity().getApplicationContext());
				deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9, 0x3F, 0x25)));
				deleteItem.setWidth(HiTools.dip2px(CameraFragment.this.getActivity(), 80));
				deleteItem.setHeight(HiTools.dip2px(CameraFragment.this.getActivity(), 200));
				menu.addMenuItem(deleteItem);
			}
		};

		mListView.setMenuCreator(creator);

		mListView.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public void onMenuItemClick(int position, SwipeMenu menu, int index) {

				MyCamera camera = HiDataValue.CameraList.get(position);
				switch (index) {
				case 0:
					showDeleteCameraDialog(camera, Effectstype.Slidetop);
					break;
				}
			}
		});

	}

	private void showDeleteCameraDialog(final MyCamera camera, Effectstype type) {
		final NiftyDialogBuilder dialog = NiftyDialogBuilder.getInstance(getActivity());
		dialog.withTitle(getString(R.string.tip_reminder)).withMessage(getString(R.string.tips_msg_delete_camera)).withEffect(type).setButton1Click(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		}).setButton2Click(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
				showjuHuaDialog();
				camera.bindPushState(false, bindPushResult);
				SharePreUtils.putBoolean("cache", getActivity(), "isFirst", false);
				SharePreUtils.putBoolean("cache", getActivity(), "isFirstPbOnline", false);
				sendUnRegister(camera, 0);
				Message msg = handler.obtainMessage();
				msg.what = HiDataValue.HANDLE_MESSAGE_DELETE_FILE;
				msg.obj = camera;
				handler.sendMessageDelayed(msg, 1000);
			}
		}).show();
	}

	private void initView() {
		titleView = (TitleView) layoutView.findViewById(R.id.fg_ca_title);
		titleView.setTitle(getString(R.string.title_camera_fragment));
		titleView.setButton(TitleView.NAVIGATION_BUTTON_RIGHT);
		if (HiDataValue.shareIsOpen) {
			titleView.setButton(TitleView.NAVIGATION_BUTTON_LEFT);
			titleView.setLeftBtnTextBackround(R.drawable.share);
			titleView.setLeftBackroundPadding(2, 2, 2, 2);
		}
		titleView.setNavigationBarButtonListener(new NavigationBarButtonListener() {
			@Override
			public void OnNavigationButtonClick(int which) {
				switch (which) {
				case TitleView.NAVIGATION_BUTTON_RIGHT:
					if (delModel) {
						titleView.setRightBtnTextBackround(R.drawable.edit);
					} else {
						titleView.setRightBtnTextBackround(R.drawable.finish);
					}

					delModel = !delModel;
					if (adapter != null) {
						adapter.notifyDataSetChanged();
					}
					break;
				case TitleView.NAVIGATION_BUTTON_LEFT:
					if (HiDataValue.CameraList.size() > 0) {
						Intent intent = new Intent(getActivity(), SeleShaCameraListActivity.class);
						startActivity(intent);
					} else {
						HiToast.showToast(getContext(), getString(R.string.tips_goto_add_camera));
					}
					break;
				}

			}
		});
		mListView = (SwipeMenuListView) layoutView.findViewById(R.id.lv_swipemenu);
		LinearLayout add_camera_ll = (LinearLayout) layoutView.findViewById(R.id.add_camera_ll);
		add_camera_ll.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), AddCameraActivity.class);
				startActivity(intent);
			}
		});
		str_state = getActivity().getResources().getStringArray(R.array.connect_state);
		adapter = new CameraListAdapter(getActivity());
		mListView.setAdapter(adapter);
		mListView.setOnItemClickListener(this);
		adapter.setOnButtonClickListener(new OnButtonClickListener() {
			@Override
			public void onButtonClick(int btnId, final MyCamera camera) {
				switch (btnId) {
				case R.id.setting_camera_item: {
					if (delModel) {
						Intent intent = new Intent();
						intent.putExtra(HiDataValue.EXTRAS_KEY_UID, camera.getUid());
						intent.setClass(getActivity(), EditCameraActivity.class);
						startActivity(intent);
					} else {
						Intent intent = new Intent();
						intent.putExtra(HiDataValue.EXTRAS_KEY_UID, camera.getUid());
						intent.setClass(getActivity(), AliveSettingActivity.class);
						startActivity(intent);
					}
				}
					break;

				case R.id.delete_icon_camera_item:
					showDeleteCameraDialog(camera, Effectstype.Slidetop);
					break;
				}
			}

		});
	}

	private void sendUnRegister(MyCamera mCamera, int enable) {
		if (mCamera.getPushState() == 1) {
			return;
		}

		if (!mCamera.getCommandFunction(CamHiDefines.HI_P2P_ALARM_TOKEN_UNREGIST)) {
			return;
		}

		byte[] info = CamHiDefines.HI_P2P_ALARM_TOKEN_INFO.parseContent(0, mCamera.getPushState(), (int) (System.currentTimeMillis() / 1000 / 3600), enable);
		mCamera.sendIOCtrl(CamHiDefines.HI_P2P_ALARM_TOKEN_UNREGIST, info);
	}

	protected void sendRegisterToken(MyCamera mCamera) {
		Log.i("tedu", "--fasdf-mCamera.getPushState()->" + mCamera.getPushState());
		if (mCamera.getPushState() == 1 || mCamera.getPushState() == 0) {

			return;
		}

		if (!mCamera.getCommandFunction(CamHiDefines.HI_P2P_ALARM_TOKEN_REGIST)) {
			return;
		}

		byte[] info = CamHiDefines.HI_P2P_ALARM_TOKEN_INFO.parseContent(0, mCamera.getPushState(), (int) (System.currentTimeMillis() / 1000 / 3600), 1);

		mCamera.sendIOCtrl(CamHiDefines.HI_P2P_ALARM_TOKEN_REGIST, info);
	}

	OnBindPushResult bindPushResult = new OnBindPushResult() {
		@Override
		public void onBindSuccess(MyCamera camera) {

			if (!camera.handSubXYZ()) {
				camera.setServerData(HiDataValue.CAMERA_ALARM_ADDRESS);
			} else {
				camera.setServerData(HiDataValue.CAMERA_ALARM_ADDRESS_THERE);
			}
			camera.updateServerInDatabase(getActivity());
			sendServer(camera);
			sendRegisterToken(camera);
		}

		@Override
		public void onBindFail(MyCamera camera) {
		}

		@Override
		public void onUnBindSuccess(MyCamera camera) {
			camera.bindPushState(true, bindPushResult);
		}

		@Override
		public void onUnBindFail(MyCamera camera) {
			// ��SubId��ŵ�sharePrefence
			if (camera.getPushState() > 0) {
				SharePreUtils.putInt("subId", getActivity(), camera.getUid(), camera.getPushState());
			}

		}

	};

	private class CameraBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (intent.getAction().equals(HiDataValue.ACTION_CAMERA_INIT_END)) {
				if (adapter != null) {
					adapter.notifyDataSetChanged();
				}

				if (HiDataValue.ANDROID_VERSION >= 6 && !HiTools.checkPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
					return;
				}
				new Handler().postAtTime(new Runnable()
				{
					
					@Override
					public void run()
					{
						// TODO Auto-generated method stub
						if (connectThread == null) {
							connectThread = new HiThreadConnect();
							connectThread.start();
						}
					}
				}, 100);


			}
		}
	}

	public class HiThreadConnect extends Thread {
		private int connnum = 0;

		public synchronized void run() {
			for (connnum = 0; connnum < HiDataValue.CameraList.size(); connnum++) {
				MyCamera camera = HiDataValue.CameraList.get(connnum);
				if (camera != null) {
					if (camera.getConnectState() == HiCamera.CAMERA_CONNECTION_STATE_DISCONNECTED) {
						camera.registerIOSessionListener(CameraFragment.this);
						camera.connect();
						try {
							Thread.sleep(150);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

				}
				Log.e("", "HiThreadConnect:" + connnum);
			}
			if (connectThread != null) {
				connectThread = null;
			}
		}

	}

	@Override
	public void onResume() {
		super.onResume();
		delToNor();

	}

	public void delToNor() {
		delModel = false;
		titleView.setRightBtnTextBackround(R.drawable.edit);
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	// camera��adapter
	public class CameraListAdapter extends BaseAdapter {
		Context context;
		private LayoutInflater mInflater;
		OnButtonClickListener mListener;
		private String strState;

		public void setOnButtonClickListener(OnButtonClickListener listener) {
			mListener = listener;
		}

		public CameraListAdapter(Context context) {

			mInflater = LayoutInflater.from(context);
			this.context = context;
		}

		@Override
		public int getCount() {
			return HiDataValue.CameraList.size();
		}

		@Override
		public Object getItem(int position) {
			return HiDataValue.CameraList.get(position);
		}

		@Override
		public long getItemId(int arg0) {

			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final MyCamera camera = HiDataValue.CameraList.get(position);
			if (camera == null) {
				return null;
			}
			ViewHolder holder = new ViewHolder();
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.camera_main_item, null);
				holder.setting = (ImageView) convertView.findViewById(R.id.setting_camera_item);
				holder.img_snapshot = (ImageView) convertView.findViewById(R.id.snapshot_camera_item);
				holder.txt_nikename = (TextView) convertView.findViewById(R.id.nickname_camera_item);
				holder.txt_uid = (TextView) convertView.findViewById(R.id.uid_camera_item);
				holder.txt_state = (TextView) convertView.findViewById(R.id.state_camera_item);
				holder.img_alarm = (ImageView) convertView.findViewById(R.id.img_alarm);
				holder.delete_icon = (ImageView) convertView.findViewById(R.id.delete_icon_camera_item);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			if (holder != null) {
				if (camera.snapshot == null) {
					holder.img_snapshot.setImageResource(R.drawable.videoclip);
				} else {
					Bitmap bitmap = BitmapUtils.setRoundedCorner(camera.snapshot, 50);
					holder.img_snapshot.setImageBitmap(bitmap);
				}

				holder.txt_nikename.setText(camera.getNikeName());
				holder.txt_uid.setText(camera.getUid());
				int state = camera.getConnectState();

				switch (state) {
				case 0:// DISCONNECTED
					holder.txt_state.setTextColor(getResources().getColor(R.color.color_disconnected));
					break;
				case -8:
				case 1:// CONNECTING
					holder.txt_state.setTextColor(getResources().getColor(R.color.color_connecting));
					break;
				case 2:// CONNECTED
					holder.txt_state.setTextColor(getResources().getColor(R.color.color_connected));
					break;
				case 3:// WRONG_PASSWORD
					holder.txt_state.setTextColor(getResources().getColor(R.color.color_pass_word));
					break;
				case 4:// STATE_LOGIN
					holder.txt_state.setTextColor(getResources().getColor(R.color.color_login));
					break;
				}
				if (state >= 0 && state <= 4) {
					strState = str_state[state];
					holder.txt_state.setText(strState);
				}
				if (state == -8) {// ҲҪ����Ϊ������...
					holder.txt_state.setText(str_state[2]);
				}
				if (camera.isSystemState == 1 && camera.getConnectState() == HiCamera.CAMERA_CONNECTION_STATE_LOGIN) {
					holder.txt_state.setText(getString(R.string.tips_restart));
				}
				if (camera.isSystemState == 2 && camera.getConnectState() == HiCamera.CAMERA_CONNECTION_STATE_LOGIN) {
					holder.txt_state.setText(getString(R.string.tips_recovery));
				}
				if (camera.isSystemState == 3 && camera.getConnectState() == HiCamera.CAMERA_CONNECTION_STATE_LOGIN) {
					holder.txt_state.setText(getString(R.string.tips_update));
				}
				holder.setting.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (mListener != null) {
							mListener.onButtonClick(R.id.setting_camera_item, camera);
						}
					}
				});

				holder.delete_icon.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						if (mListener != null) {
							mListener.onButtonClick(R.id.delete_icon_camera_item, camera);
						}

					}
				});

				if (delModel) {
					holder.delete_icon.setVisibility(View.VISIBLE);
				} else {
					holder.delete_icon.setVisibility(View.GONE);
				}

				if (camera.getAlarmState() == 0) {
					holder.img_alarm.setVisibility(View.GONE);
				} else {
					holder.img_alarm.setVisibility(View.VISIBLE);
				}
			}

			return convertView;
		}

		public class ViewHolder {
			public ImageView img_snapshot;
			public TextView txt_nikename;
			public TextView txt_uid;
			public TextView txt_state;
			public ImageView img_alarm;

			public ImageView setting;
			public ImageView delete_icon;

		}

	}

	@Override
	public void receiveIOCtrlData(HiCamera arg0, int arg1, byte[] arg2, int arg3) {
		if (arg1 == HiChipDefines.HI_P2P_GET_SNAP && arg3 == 0) {
			MyCamera camera = (MyCamera) arg0;
			if (!camera.reciveBmpBuffer(arg2)) {
				return;
			}
		}
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

		if (HiDataValue.isDebug)
			HiLog.v("uid:" + arg0.getUid() + "  state:" + arg1);

		Message msg = handler.obtainMessage();
		msg.what = HiDataValue.HANDLE_MESSAGE_SESSION_STATE;
		msg.arg1 = arg1;
		msg.obj = arg0;
		handler.sendMessage(msg);

	}

	private long startTime = 0;
	private long endTime = 0;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			MyCamera camera = (MyCamera) msg.obj;
			switch (msg.what) {
			case HiDataValue.HANDLE_MESSAGE_SESSION_STATE:
				if (adapter != null)
					adapter.notifyDataSetChanged();
				switch (msg.arg1) {
				case HiCamera.CAMERA_CONNECTION_STATE_DISCONNECTED:
					break;
				case HiCamera.CAMERA_CONNECTION_STATE_LOGIN:
					startTime = System.currentTimeMillis();
					camera.isSystemState = 0;
					setTime(camera);
					if (camera.getPushState() > 0) {
						Log.i("tedu", "--XGToken-->" + HiDataValue.XGToken);
						Log.i("tedu", "--setServer-->");
						camera.bindPushState(true, bindPushResult);
						setServer(camera);
					}
					if (!camera.getCommandFunction(HiChipDefines.HI_P2P_PB_QUERY_START_NODST)) {
						if (camera.getCommandFunction(HiChipDefines.HI_P2P_GET_TIME_ZONE_EXT)) {
							camera.sendIOCtrl(HiChipDefines.HI_P2P_GET_TIME_ZONE_EXT, new byte[0]);
						} else {
							camera.sendIOCtrl(HiChipDefines.HI_P2P_GET_TIME_ZONE, new byte[0]);
						}
					}
					break;
				case HiCamera.CAMERA_CONNECTION_STATE_WRONG_PASSWORD:
					break;
				case HiCamera.CAMERA_CONNECTION_STATE_CONNECTING:
					break;
				}
				break;
			case HiDataValue.HANDLE_MESSAGE_RECEIVE_IOCTRL:
				if (msg.arg2 == 0) {
					handIOCTRLSucce(msg, camera);
				}
				break;

			case HiDataValue.HANDLE_MESSAGE_DELETE_FILE:
				camera.disconnect();
				camera.deleteInCameraList();
				camera.deleteInDatabase(getActivity());
				adapter.notifyDataSetChanged();
				dismissjuHuaDialog();
				HiToast.showToast(getActivity(), getString(R.string.tips_remove_success));
				break;
			}
		}

		private void handIOCTRLSucce(Message msg, MyCamera camera) {
			Bundle bundle = msg.getData();
			byte[] data = bundle.getByteArray(HiDataValue.EXTRAS_KEY_DATA);
			switch (msg.arg1) {
			case HiChipDefines.HI_P2P_GET_SNAP:
				adapter.notifyDataSetChanged();
				if (camera.snapshot != null) {
					File rootFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/");
					File sargetFolder = new File(rootFolder.getAbsolutePath() + "/android/data/" + getActivity().getResources().getString(R.string.app_name));

					if (!rootFolder.exists()) {
						rootFolder.mkdirs();
					}
					if (!sargetFolder.exists()) {
						sargetFolder.mkdirs();
					}
				}
				break;

			case HiChipDefines.HI_P2P_GET_TIME_ZONE: {

				HiChipDefines.HI_P2P_S_TIME_ZONE timezone = new HiChipDefines.HI_P2P_S_TIME_ZONE(data);

				if (timezone.u32DstMode == 1) {
					camera.setSummerTimer(true);
				} else {
					camera.setSummerTimer(false);
				}

			}
				break;
			case HiChipDefines.HI_P2P_GET_TIME_ZONE_EXT: {
				HiChipDefines.HI_P2P_S_TIME_ZONE_EXT timezone = new HiChipDefines.HI_P2P_S_TIME_ZONE_EXT(data);
				if (timezone.u32DstMode == 1) {
					camera.setSummerTimer(true);
				} else {
					camera.setSummerTimer(false);
				}
				break;
			}
			case CamHiDefines.HI_P2P_ALARM_TOKEN_REGIST:

				break;
			case CamHiDefines.HI_P2P_ALARM_TOKEN_UNREGIST:
				break;
			case CamHiDefines.HI_P2P_ALARM_ADDRESS_SET:
				Log.i("tedu", "---::::-�ɹ��Ļص���->");
				break;
			case CamHiDefines.HI_P2P_ALARM_ADDRESS_GET:
				break;

			// ������ֱ�ƵĻص�
			case HiChipDefines.HI_P2P_ALARM_EVENT: {
				if (camera.getPushState() == 0) {
					return;
				}
				/*
				 * //��������ʱ���ÿ30��һ�λص��� if(System.currentTimeMillis() - camera.getLastAlarmTime() < 30000) { HiLog.e("Time lastAlarmTime:"+(System. currentTimeMillis() - lastAlarmTime)); return; }
				 */
				camera.setLastAlarmTime(System.currentTimeMillis());
				HiChipDefines.HI_P2P_EVENT event = new HiChipDefines.HI_P2P_EVENT(data);
				showAlarmNotification(camera, event, System.currentTimeMillis());
				saveAlarmData(camera, event.u32Event, (int) (System.currentTimeMillis() / 1000));
				camera.setAlarmState(1);
				camera.setAlarmLog(true);
				adapter.notifyDataSetChanged();
			}
				break;
			}
		}
	};

	// �������͵�֪ͨ�� ****API 11��֮�����****
	@SuppressWarnings("deprecation")
	private void showAlarmNotification(MyCamera camera, HiChipDefines.HI_P2P_EVENT event, long evtTime) {
		try {
			NotificationManager manager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
			Bundle extras = new Bundle();
			extras.putString(HiDataValue.EXTRAS_KEY_UID, camera.getUid());
			extras.putInt("type", 1);
			Intent intent = new Intent(getActivity(), MainActivity.class);
			intent.setAction(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			intent.putExtras(extras);
			PendingIntent pendingIntent = PendingIntent.getActivity(getActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			String[] alarmList = getResources().getStringArray(R.array.tips_alarm_list_array);
			String type = null;
			switch (event.u32Event) {
			case 0:
				type = alarmList[0];
				break;
			case 1:
				type = alarmList[1];
				break;
			case 2:
				type = alarmList[2];
				break;
			case 3:
				type = alarmList[3];
				break;
			case 6:
				String sType = new String(event.sType).trim();
				if ("key2".equals(sType)) {
					type = getString(R.string.alarm_sos);
				} else if ("key3".equals(sType)) {
					type = getString(R.string.alarm_ring);
				} else if ("door".equals(sType)) {
					type = getString(R.string.alarm_door);
				} else if ("infra".equals(sType)) {
					type = getString(R.string.alarm_infra);
				} else if ("beep".equals(sType)) {
					type = getString(R.string.alarm_doorbell);
				} else if ("fire".equals(sType)) {
					type = getString(R.string.alarm_smoke);
				} else if ("gas".equals(sType)) {
					type = getString(R.string.alarm_gas);
				} else if ("socket".equals(sType)) {
					type = getString(R.string.alarm_socket);
				} else if ("temp".equals(sType)) {
					type = getString(R.string.alarm_temp);
				} else if ("humi".equals(sType)) {
					type = getString(R.string.alarm_humi);
				}
				break;
			}
			Notification notification = new Notification.Builder(getActivity()).setSmallIcon(R.drawable.ic_launcher).setTicker(camera.getNikeName()).setContentTitle(camera.getUid()).setContentText(type).setContentIntent(pendingIntent).getNotification();
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
			notification.defaults = Notification.DEFAULT_ALL;
			ranNum++;
			manager.notify(ranNum, notification);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void setServer(MyCamera mCamera) {
		if (!mCamera.getCommandFunction(CamHiDefines.HI_P2P_ALARM_ADDRESS_SET)) {
			return;
		}
		// ������ݿⱣ��Ļ����ϵ�ַ�ͽ�󲢰��µĵ�ַ
		if (mCamera.getServerData() != null && !mCamera.getServerData().equals(HiDataValue.CAMERA_ALARM_ADDRESS)) {
			if (mCamera.getPushState() > 1) {
				if (HiDataValue.XGToken == null) {
					if (HiDataValue.ANDROID_VERSION >= 6) {
						if (!HiTools.checkPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
							ActivityCompat.requestPermissions(getActivity(), new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
						}
					}

					HiDataValue.XGToken = XGPushConfig.getToken(getActivity());
				}
				mCamera.bindPushState(false, bindPushResult);
				return;
			}
		}
		// Log.i("tedu", "---start--->");
		// sendServer(mCamera);
		// Log.i("tedu", "---end--->");
		// sendRegisterToken(mCamera);

	}

	protected void sendServer(MyCamera mCamera) {
		// //����
		// mCamera.sendIOCtrl(CamHiDefines.HI_P2P_ALARM_ADDRESS_GET, null);
		Log.i("tedu", "--mCamera.getServerData()-->" + mCamera.getServerData());
		if (mCamera.getServerData() == null) {
			mCamera.setServerData(HiDataValue.CAMERA_ALARM_ADDRESS);
			mCamera.updateServerInDatabase(getActivity());
		}
		if (!mCamera.getCommandFunction(CamHiDefines.HI_P2P_ALARM_ADDRESS_SET)) {
			return;
		}
		Log.i("tedu", "--mCamera.push-->" + mCamera.push);
		if (mCamera.push != null) {
			String[] strs = mCamera.push.getPushServer().split("\\.");
			if (strs.length == 4 && isInteger(strs[0]) && isInteger(strs[1]) && isInteger(strs[2]) && isInteger(strs[3])) {
				byte[] info = HI_P2P_ALARM_ADDRESS.parseContent(mCamera.push.getPushServer());
				mCamera.sendIOCtrl(CamHiDefines.HI_P2P_ALARM_ADDRESS_SET, info);
				Log.i("tedu", "--:::-->" + mCamera.push.getPushServer());
			}

		}
	}

	/*
	 * �Ƽ����ٶ���� �ж��Ƿ�Ϊ����
	 * 
	 * @param str ������ַ���
	 * 
	 * @return ����������true,���򷵻�false
	 */

	public static boolean isInteger(String str) {
		Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
		return pattern.matcher(str).matches();
	}

	private void saveAlarmData(MyCamera camera, int evtType, int evtTime) {

		DatabaseManager manager = new DatabaseManager(getActivity());
		manager.addAlarmEvent(camera.getUid(), evtTime, evtType);

	}

	private void setTime(MyCamera camera) {
		Calendar cal = Calendar.getInstance(TimeZone.getDefault());
		cal.setTimeInMillis(System.currentTimeMillis());

		byte[] time = HiChipDefines.HI_P2P_S_TIME_PARAM.parseContent(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));

		camera.sendIOCtrl(HiChipDefines.HI_P2P_SET_TIME_PARAM, time);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			getActivity().unregisterReceiver(receiver);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (connectThread != null) {
			connectThread.interrupt();
		}

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final MyCamera selectedCamera = HiDataValue.CameraList.get(position);
		if (delModel) {
			Intent intent = new Intent();
			intent.putExtra(HiDataValue.EXTRAS_KEY_UID, selectedCamera.getUid());
			intent.setClass(getActivity(), EditCameraActivity.class);
			startActivity(intent);
		} else {
			if (selectedCamera.getConnectState() == HiCamera.CAMERA_CONNECTION_STATE_LOGIN) {
				Bundle extras = new Bundle();
				extras.putString(HiDataValue.EXTRAS_KEY_UID, selectedCamera.getUid());
				Intent intent = new Intent();
				intent.putExtras(extras);
				if (selectedCamera.isFishEye()) {// �����۵Ļ� ����ת���۽���
					// ��ʼ�����۶�װ�ͱ�װ��ģʽ
					int num = SharePreUtils.getInt("mInstallMode", getActivity(), selectedCamera.getUid());
					selectedCamera.mInstallMode = num == -1 ? 0 : num;
					boolean bl = SharePreUtils.getBoolean("cache", getActivity(), "isFirst");
					selectedCamera.isFirst = bl;
					intent.setClass(getActivity(), FishEyeActivity.class);
				} else {
					intent.setClass(getActivity(), LiveViewActivity.class);
				}
				startActivity(intent);
				HiDataValue.isOnLiveView = true;
				selectedCamera.setAlarmState(0);
				adapter.notifyDataSetChanged();
			} else if (selectedCamera.getConnectState() == HiCamera.CAMERA_CONNECTION_STATE_DISCONNECTED || selectedCamera.getConnectState() == HiCamera.CAMERA_CONNECTION_STATE_WRONG_PASSWORD) {
				if (HiDataValue.ANDROID_VERSION >= 6 && !HiTools.checkPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
					showAlertDialog();
					return;
				}
				selectedCamera.connect();
				adapter.notifyDataSetChanged();
			} else {
				HiToast.showToast(getActivity(), getString(R.string.click_offline_setting));
				return;
			}
		}

	}

	private void showAlertDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(getString(R.string.tips_no_permission));
		builder.setPositiveButton(getString(R.string.setting), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent();
				intent.setAction("android.intent.action.MAIN");
				intent.setClassName("com.android.settings", "com.android.settings.ManageApplications");
				startActivity(intent);
			}
		});
		builder.setNegativeButton(getString(R.string.cancel), null);
		builder.setCancelable(false);
		builder.show();

	}

}
