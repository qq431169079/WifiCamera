package com.thecamhi.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.hichip.base.HiLog;
import com.hichip.callback.ICameraIOSessionCallback;
import com.hichip.callback.ICameraPlayStateCallback;
import com.hichip.content.HiChipDefines;
import com.hichip.content.HiChipDefines.HI_P2P_WHITE_LIGHT_INFO;
import com.hichip.content.HiChipDefines.HI_P2P_WHITE_LIGHT_INFO_EXT;
import com.hichip.control.HiCamera;
import com.hichip.sdk.HiChipP2P;
import com.hichip.widget.SwitchButton;
import com.hichip1.R;
import com.thecamhi.base.HiToast;
import com.thecamhi.base.HiTools;
import com.thecamhi.base.MyLiveViewGLMonitor;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;
import com.thecamhi.main.HiActivity;
import com.thecamhi.model.LiveViewModel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

public class LiveViewActivity extends HiActivity implements ICameraIOSessionCallback, ICameraPlayStateCallback, View.OnClickListener, OnTouchListener {

	private MyCamera mCamera;
	private MyLiveViewGLMonitor mMonitor;
	private HiChipDefines.HI_P2P_WHITE_LIGHT_INFO_EXT light_info_ext;
	private HiChipDefines.HI_P2P_WHITE_LIGHT_INFO light_info;
	private int video_width;
	private int video_height;
	private ProgressBar prs_loading;
	private ImageView img_shade;
	private ImageView btn_live_listen;
	private ImageView btn_live_exit;
	private ImageView btn_live_mirror_flip;
	private ImageView btn_live_preset;
	private ImageView btn_live_record;
	private ImageView btn_live_snapshot;
	private ImageView resolution_ratio;
	private ImageView btn_live_zoom_focus;
	private ImageView btn_live_light;
	private ImageView btn_microphone, iv_alarm, iv_alarm_mark;
	private LinearLayout linearLayout1, lay_live_tools_bottom, btn_live_light_layout, ll_alarm;
	private TextView txt_recording;
	private ImageView mIvRecording;
	private PopupWindow mPopupWindow;
	private boolean isListening = false;
	private boolean isTalking = false;
	private final static int RECORDING_STATUS_NONE = 0;
	private final static int RECORDING_STATUS_LOADING = 1;
	private final static int RECORDING_STATUS_ING = 2;
	private int mRecordingState = RECORDING_STATUS_NONE;
	private boolean visible = false;
	FrameLayout live_view_screen = null;
	private long oldClickTime;
	private int lightModel = 0;// 0 non ,1 HI_P2P_WHITE_LIGHT_GET_EXT ,2
								// HI_P2P_WHITE_LIGHT_GET
	private int action = 0;
	private Timer timer;
	private TimerTask timerTask;
	private int mCameraVideoQuality;
	private ImageView mIvLoading2;
	protected String recordFile;
	private boolean isMF = false;// 镜像和翻转的回调标志位
	private boolean mIsSupportAlarm = false;
	// 报警是否在响铃
	private boolean isRing = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		HiTools.hideVirtualKey(this);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		Bundle bundle = this.getIntent().getExtras();
		String uid = bundle.getString(HiDataValue.EXTRAS_KEY_UID);
		// 通过遍历uid获取相应的摄像机
		for (MyCamera camera : HiDataValue.CameraList) {
			if (camera.getUid().equals(uid)) {
				mCamera = camera;
				mCamera.registerIOSessionListener(this);
				mCamera.registerPlayStateListener(this);
				mCameraVideoQuality = mCamera.getVideoQuality();
				break;
			}
		}
		// 放在initView上面,解决白光灯不显示的bug
		getLightModel();
		initView();
		getAlarmMode();
		showLoadingView();

	}

	private void getLightModel() {
		boolean b = mCamera.getCommandFunction(HiChipDefines.HI_P2P_WHITE_LIGHT_GET_EXT);
		if (b) {
			lightModel = 1;
			mCamera.sendIOCtrl(HiChipDefines.HI_P2P_WHITE_LIGHT_GET_EXT, null);
			return;
		}
		b = mCamera.getCommandFunction(HiChipDefines.HI_P2P_WHITE_LIGHT_GET);
		if (b) {
			lightModel = 2;
			mCamera.sendIOCtrl(HiChipDefines.HI_P2P_WHITE_LIGHT_GET, null);
		}
	}

	private void initView() {
		setContentView(R.layout.activity_live_view_landscape);
		// 监控的控件绑定camera
		mMonitor = (MyLiveViewGLMonitor) findViewById(R.id.monitor_live_view);
		mMonitor.setOnTouchListener(this);
		mMonitor.setCamera(mCamera);
		// 开始播放
		mCamera.setLiveShowMonitor(mMonitor);
		btn_live_exit = (ImageView) findViewById(R.id.btn_live_exit);
		btn_live_exit.setOnClickListener(this);

		linearLayout1 = (LinearLayout) findViewById(R.id.linearLayout1);
		lay_live_tools_bottom = (LinearLayout) findViewById(R.id.lay_live_tools_bottom);

		btn_live_snapshot = (ImageView) findViewById(R.id.btn_live_snapshot);
		btn_live_snapshot.setOnClickListener(this);

		btn_live_record = (ImageView) findViewById(R.id.btn_live_record);
		btn_live_record.setOnClickListener(this);

		txt_recording = (TextView) findViewById(R.id.txt_recording);
		mIvRecording = (ImageView) findViewById(R.id.iv_recording);

		btn_live_listen = (ImageView) findViewById(R.id.btn_live_listen);
		btn_live_listen.setOnClickListener(this);

		btn_microphone = (ImageView) findViewById(R.id.btn_microphone);
		btn_microphone.setOnTouchListener(this);
		btn_microphone.setVisibility(View.GONE);

		resolution_ratio = (ImageView) findViewById(R.id.resolution_ratio);
		resolution_ratio.setOnClickListener(this);

		btn_live_zoom_focus = (ImageView) findViewById(R.id.btn_live_zoom_focus);
		btn_live_zoom_focus.setOnClickListener(this);

		btn_live_preset = (ImageView) findViewById(R.id.btn_live_preset);
		btn_live_preset.setOnClickListener(this);

		btn_live_mirror_flip = (ImageView) findViewById(R.id.btn_live_mirror_flip);
		btn_live_mirror_flip.setOnClickListener(this);

		btn_live_light = (ImageView) findViewById(R.id.btn_live_light);
		btn_live_light.setOnClickListener(this);
		btn_live_light_layout = (LinearLayout) findViewById(R.id.btn_live_light_layout);
		btn_live_light_layout.setVisibility(lightModel == 0 ? View.GONE : View.VISIBLE);

		img_shade = (ImageView) findViewById(R.id.img_shade);
		mIvLoading2 = (ImageView) findViewById(R.id.iv_loading2);

		ll_alarm = (LinearLayout) findViewById(R.id.ll_alarm);
		iv_alarm = (ImageView) findViewById(R.id.iv_alarm);
		iv_alarm.setOnClickListener(this);
		iv_alarm_mark = (ImageView) findViewById(R.id.iv_alarm_mark);

	}

	private void getAlarmMode() {
		mIsSupportAlarm = mCamera.getCommandFunction(HiChipDefines.HI_P2P_RING);
		ll_alarm.setVisibility(mIsSupportAlarm ? View.VISIBLE : View.GONE);
		// 进来获取是否在响铃
		if (mIsSupportAlarm) {
			mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_RING, null);
		}
	}

	private void showLoadingView() {
		Animation rotateAnim = AnimationUtils.loadAnimation(LiveViewActivity.this, R.anim.rotate);
		mIvLoading2.setVisibility(View.VISIBLE);
		mIvLoading2.startAnimation(rotateAnim);
	}

	private class MySnapshot extends Thread {
		@Override
		public void run() {

			if (mCamera != null) {

				if (HiTools.isSDCardValid()) {

					File rootFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/");
					File sargetFolder = new File(rootFolder.getAbsolutePath() + "/Snapshot/");
					File yargetFolder = new File(sargetFolder.getAbsolutePath() + "/" + mCamera.getUid() + "/");
					if (!rootFolder.exists()) {
						rootFolder.mkdirs();
					}
					if (!sargetFolder.exists()) {
						sargetFolder.mkdirs();
					}
					if (!yargetFolder.exists()) {
						yargetFolder.mkdirs();
					}

					String filename = HiTools.getFileNameWithTime(0);
					final String file = yargetFolder.getAbsoluteFile() + "/" + filename;

					HiLog.v("btn_live_snapshot:" + file);

					//获取视频帧
					Bitmap frame = mCamera != null ? mCamera.getSnapshot() : null;
					if (frame != null && HiTools.saveImage(file, frame)) {
						SaveToPhone(file, filename);
						HiLog.v("btn_live_snapshot:" + true);
					}
				}
			}
			try{
				sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	// 代码的方式 播放帧动画
	private void startFrameAnimation(ImageView iv) {
		AnimationDrawable aw = new AnimationDrawable();
		aw.addFrame(getResources().getDrawable(R.drawable.alarm_1), 100);
		aw.addFrame(getResources().getDrawable(R.drawable.alarm_2), 100);
		aw.addFrame(getResources().getDrawable(R.drawable.alarm_3), 100);
		aw.addFrame(getResources().getDrawable(R.drawable.alarm_4), 100);
		aw.addFrame(getResources().getDrawable(R.drawable.alarm_5), 100);

		iv.setBackground(aw);
		aw.setOneShot(false);
		aw.start();
	}

	private void setViewVisible(boolean visible) {
		linearLayout1.setVisibility(visible ? View.VISIBLE : View.GONE);
		lay_live_tools_bottom.setVisibility(visible ? View.VISIBLE : View.GONE);
		this.visible = !visible;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mCamera != null) {
			mCamera.startLiveShow(mCamera.getVideoQuality(), mMonitor);
			mCamera.registerIOSessionListener(this);
			mCamera.registerPlayStateListener(this);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		// 退出界面时..关闭报警
		if (isRing) {
			mCamera.sendIOCtrl(HiChipDefines.HI_P2P_RING, HiChipDefines.HI_P2P_RING_Fun.parseContent(0));
		}
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		if (timerTask != null) {
			timerTask.cancel();
			timerTask = null;
		}

		if (mCamera != null) {
			mCamera.stopLiveShow();
			mCamera.unregisterPlayStateListener(this);
			mCamera.unregisterIOSessionListener(this);
		}
		if (action == 0) {
			finish();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mCamera.stopListening();
		handler.removeCallbacksAndMessages(null);
		mHandler.removeCallbacksAndMessages(null);
		HiLog.e("----------------------LiveViewActivity onDestroy--------------------------");

	}

	@Override
	public void onBackPressed() {
		action = 1;
		super.onBackPressed();
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HiDataValue.HANDLE_MESSAGE_SESSION_STATE: {
				switch (msg.arg1) {
				case HiCamera.CAMERA_CONNECTION_STATE_DISCONNECTED:
					setViewWhetherClick(false);
					showLoadingView();
					if (mCamera != null) {
						mCamera.stopLiveShow();
						if (isListening) {
							isListening = false;
							btn_live_listen.setImageResource(R.drawable.camhi_live_normal_speaker);
							btn_microphone.setVisibility(View.GONE);
						}
					}
					break;
				case HiCamera.CAMERA_CONNECTION_STATE_LOGIN:
					if (mCameraVideoQuality != mCamera.getVideoQuality()) {
						mCamera.stopRecording();
						btn_live_record.setImageResource(R.drawable.camhi_live_normal_recording);
						if (timer != null) {
							timer.cancel();
							timer = null;
						}
						if (timerTask != null) {
							timerTask.cancel();
							timerTask = null;
						}
						mCameraVideoQuality = mCamera.getVideoQuality();
					}
					mCamera.startLiveShow(mCamera.getVideoQuality(), mMonitor);
					break;
				case HiCamera.CAMERA_CONNECTION_STATE_WRONG_PASSWORD:
					break;
				case HiCamera.CAMERA_CONNECTION_STATE_CONNECTING:
					break;
				case HiCamera.CAMERA_CHANNEL_STREAM_ERROR:// 通道出错处理，断线重连
					if (mCamera != null) {
						mCamera.stopLiveShow();
						// mCamera.disconnect();
						// mCamera.connect();
						if (isListening) {
							isListening = false;
							btn_live_listen.setImageResource(R.drawable.camhi_live_normal_speaker);
							btn_microphone.setVisibility(View.GONE);
						}
					}
					break;
				}
			}
				break;
			case HiDataValue.HANDLE_MESSAGE_RECEIVE_IOCTRL: {
				if (msg.arg2 == 0) {// 成功的状态值
					handReceiveIoCtrlSuccess(msg);
				} else if (msg.arg2 == -1) {// 失败的状态值
					switch (msg.arg1) {
					case HiChipDefines.HI_P2P_SET_PTZ_PRESET:
						if (LiveViewModel.getInstance().mFlagPreset == 1) {
							HiToast.showToast(LiveViewActivity.this, getString(R.string.tip_preset_fail));
						}
						break;
					}
				}
			}
				break;
			// 播放状态
			case HiDataValue.HANDLE_MESSAGE_PLAY_STATE:
				setViewWhetherClick(true);
				Bundle bundle = msg.getData();
				int command = bundle.getInt("command");
				switch (command) {
				// 接收到了开始的状态
				case ICameraPlayStateCallback.PLAY_STATE_START:
					dismissLoadingView();
					video_width = bundle.getInt("width");
					video_height = bundle.getInt("height");
					Bitmap frame = mCamera != null ? mCamera.getSnapshot() : null;
					if (frame != null) {
						saveSnapshot(frame);
					}
					break;

				// 本地录像开始
				case ICameraPlayStateCallback.PLAY_STATE_RECORDING_START:
					mRecordingState = RECORDING_STATUS_ING;
					txt_recording.setVisibility(View.VISIBLE);
					mIvRecording.setVisibility(View.VISIBLE);
					mHandler.sendEmptyMessage(110);
					break;
				// 本地录像结束
				case ICameraPlayStateCallback.PLAY_STATE_RECORDING_END:
				case ICameraPlayStateCallback.PLAY_STATE_RECORD_ERROR:
					mRecordingState = RECORDING_STATUS_NONE;
					txt_recording.setVisibility(View.GONE);
					mIvRecording.setVisibility(View.GONE);
					mHandler.removeCallbacksAndMessages(null);
					File file = new File(recordFile);
					if (file.length() <= 1024 && file.isFile() && file.exists()) {
						file.delete();
					}
					break;
				}
				break;
			}

		}

		private void handReceiveIoCtrlSuccess(Message msg) {
			MyCamera camera = (MyCamera) msg.obj;
			Bundle bundle = msg.getData();
			byte[] data = bundle.getByteArray(HiDataValue.EXTRAS_KEY_DATA);

			switch (msg.arg1) {
			case HiChipDefines.HI_P2P_SET_PTZ_PRESET:
				int state = LiveViewModel.getInstance().mFlagPreset;
				if (state == 1) {
					HiToast.showToast(LiveViewActivity.this, getString(R.string.tips_preset_set_btn));
				} else if (state == 2) {
					// HiToast.showToast(LiveViewActivity.this, "调用成功啦");
				}
				break;
			case HiChipDefines.HI_P2P_GET_DISPLAY_PARAM:
				if (isMF) {// 为了规避第一次进来还要发设置的请求
					if (display_param != null) {
						dismissLoadingView();
						display_param = new HiChipDefines.HI_P2P_S_DISPLAY(data);
						display_param.u32Mirror = mirrorChecked;
						display_param.u32Flip = filpChecked;
						mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_DISPLAY_PARAM, display_param.parseContent());
					}
				} else {
					display_param = new HiChipDefines.HI_P2P_S_DISPLAY(data);
					mirrorChecked = display_param.u32Mirror;
					filpChecked = display_param.u32Flip;
					clickMirrorFlip(btn_live_mirror_flip);
				}
				isMF = false;
				break;
			case HiChipDefines.HI_P2P_WHITE_LIGHT_GET:
				if (lightModel != 2) {
					return;
				}
				light_info = new HI_P2P_WHITE_LIGHT_INFO(data);

				break;
			case HiChipDefines.HI_P2P_WHITE_LIGHT_GET_EXT:
				if (lightModel != 1) {
					return;
				}
				light_info_ext = new HI_P2P_WHITE_LIGHT_INFO_EXT(data);

				break;
			// +++
			case HiChipDefines.HI_P2P_RING:
				isRing = !isRing;
				if (isRing) {
					iv_alarm_mark.setVisibility(View.VISIBLE);
					startFrameAnimation(iv_alarm_mark);
					iv_alarm.setSelected(true);
				} else {
					iv_alarm_mark.setVisibility(View.GONE);
					iv_alarm.setSelected(false);
				}
				break;
			case HiChipDefines.HI_P2P_GET_RING:
				HiChipDefines.HI_P2P_RING_Fun fun = new HiChipDefines.HI_P2P_RING_Fun(data);
				if (fun.enable == 1) {
					isRing = !isRing;
					iv_alarm_mark.setVisibility(View.VISIBLE);
					startFrameAnimation(iv_alarm_mark);
					iv_alarm.setSelected(true);
				} else {
					iv_alarm_mark.setVisibility(View.GONE);
					iv_alarm.setSelected(false);
				}
				break;
			}
		}
	};

	// 保存国科的快照
	private void saveSnapshot(final Bitmap frame) {

		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... arg0) {
				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

					File rootFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/");
					File sargetFolder = new File(rootFolder.getAbsolutePath() + "/android/data/" + getResources().getString(R.string.app_name));

					if (!rootFolder.exists()) {
						rootFolder.mkdirs();
					}
					if (!sargetFolder.exists()) {
						sargetFolder.mkdirs();
					}

					HiTools.saveBitmap(frame, sargetFolder.getAbsolutePath() + "/" + mCamera.getUid());
					HiLog.v(sargetFolder.getAbsolutePath() + "/" + mCamera.getUid());
					mCamera.snapshot = frame;
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				Intent intent = new Intent();
				intent.setAction(HiDataValue.ACTION_CAMERA_INIT_END);
				sendBroadcast(intent);
				super.onPostExecute(result);
			}
		}.execute();

	}

	int moveX;
	int moveY;

	private void resetMonitorSize(boolean large, double move) {

		if (mMonitor.height == 0 && mMonitor.width == 0) {

			initMatrix((int) mMonitor.screen_width, (int) mMonitor.screen_height);
		}

		moveX = (int) (move / 2);
		moveY = (int) ((move * mMonitor.screen_height / mMonitor.screen_width) / 2);

		if (large) {
			HiLog.e(" larger and larger ");
			if (mMonitor.width <= 2 * mMonitor.screen_width && mMonitor.height <= 2 * mMonitor.screen_height) {

				mMonitor.left -= (moveX / 2);
				mMonitor.bottom -= (moveY / 2);
				mMonitor.width += (moveX);
				mMonitor.height += (moveY);
			}
		} else {
			HiLog.e(" smaller and smaller ");

			mMonitor.left += (moveX / 2);
			mMonitor.bottom += (moveY / 2);
			mMonitor.width -= (moveX);
			mMonitor.height -= (moveY);
		}

		if (mMonitor.left > 0 || mMonitor.width < (int) mMonitor.screen_width || mMonitor.height < (int) mMonitor.screen_height || mMonitor.bottom > 0) {
			initMatrix((int) mMonitor.screen_width, (int) mMonitor.screen_height);
		}

		HiLog.e("mMonitor.left=" + mMonitor.left + " mMonitor.bottom=" + mMonitor.bottom + "\n mMonitor.width=" + mMonitor.width + " mMonitor.height=" + mMonitor.height);

		if (mMonitor.width > (int) mMonitor.screen_width) {
			mMonitor.setState(1);
		} else {
			mMonitor.setState(0);
		}

		mMonitor.setMatrix(mMonitor.left, mMonitor.bottom, mMonitor.width, mMonitor.height);

	}

	private void initMatrix(int screen_width, int screen_height) {
		mMonitor.left = 0;
		mMonitor.bottom = 0;

		mMonitor.width = screen_width;
		mMonitor.height = screen_height;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		// +++
		case R.id.iv_alarm:
			int enable = isRing ? 0 : 1;
			mCamera.sendIOCtrl(HiChipDefines.HI_P2P_RING, HiChipDefines.HI_P2P_RING_Fun.parseContent(enable));
			break;
		case R.id.btn_live_listen:
			if (HiDataValue.ANDROID_VERSION >= 6 && (!checkPermission(Manifest.permission.RECORD_AUDIO) || !checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
				showAlertDialog();
				return;
			}
			clickListen((ImageView) v);
			break;
		case R.id.btn_live_exit:
			action = 1;
			finish();
			break;
		case R.id.btn_live_mirror_flip:
			mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_DISPLAY_PARAM, null);
			// clickMirrorFlip((ImageView) v);
			break;
		case R.id.btn_live_preset:
			// 数字键盘设置预置位
			setUpPreset((ImageView) v);
			break;
		case R.id.resolution_ratio:
			if (!isListening) {
				clickRatio((ImageView) v);
			} else {
				HiToast.showToast(LiveViewActivity.this, getString(R.string.tip_not_switch_quality));
			}

			break;
		case R.id.btn_live_light:
			showLight((ImageView) v);

			break;

		case R.id.btn_live_record:
			if (HiDataValue.ANDROID_VERSION >= 6 && (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
				showAlertDialog();
				return;
			}
			clickRecording((ImageView) v);
			break;
		case R.id.btn_live_zoom_focus:
			clickZoomFocus((ImageView) v);
			break;

		case R.id.btn_live_snapshot:
			if (HiDataValue.ANDROID_VERSION >= 6 && (!checkPermission(Manifest.permission.CAMERA) || !checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
				// Toast.makeText(LiveViewActivity.this,
				// getString(R.string.tips_no_permission), Toast.LENGTH_SHORT)
				// .show();
				showAlertDialog();
				return;
			}
			clickSnapshot();
			break;
		}
	}

	private void showAlertDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(LiveViewActivity.this);
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
		builder.show();

	}

	// 设置预置位
	private void setUpPreset(ImageView v) {
		View numKeyBoard = getLayoutInflater().inflate(R.layout.popup_preset_key, null);
		mPopupWindow = new PopupWindow(LiveViewActivity.this);
		mPopupWindow.setContentView(numKeyBoard);
		ColorDrawable cd = new ColorDrawable(-0000);
		mPopupWindow.setBackgroundDrawable(cd);
		mPopupWindow.setOutsideTouchable(true);
		mPopupWindow.setFocusable(true);
		mPopupWindow.setWidth(HiTools.dip2px(LiveViewActivity.this, 200));
		mPopupWindow.setHeight(LayoutParams.WRAP_CONTENT);
		int offsetx = HiTools.dip2px(this, 20);
		int location[] = new int[2];
		v.getLocationOnScreen(location);
		int offsety = HiTools.dip2px(this, 90);
		mPopupWindow.showAtLocation(v, 0, location[0] - offsetx, offsety - location[1]);
		// 处理键盘的逻辑
		LiveViewModel.getInstance().handKeyBoard(LiveViewActivity.this, numKeyBoard, mCamera);

	}

	private boolean checkPermission(String permission) {
		int checkCallPhonePermission = ContextCompat.checkSelfPermission(LiveViewActivity.this, permission);
		if (checkCallPhonePermission == PackageManager.PERMISSION_GRANTED) {
			return true;
		}
		return false;
	}

	// private int isTouchMoved; //not move=0, move=1, two point=2
	// private int state=0; //normal=0, larger=1.arrow=2;
	private float action_down_x;
	private float action_down_y;

	float lastX;
	float lastY;

	int xlenOld;
	int ylenOld;

	float move_x;
	float move_y;

	public float left;
	public float width;
	public float height;
	public float bottom;

	double nLenStart = 0;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v.getId() == R.id.monitor_live_view) {
			int nCnt = event.getPointerCount();
			if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN && 2 == nCnt) {
				mMonitor.setTouchMove(2);
				for (int i = 0; i < nCnt; i++) {
					float x = event.getX(i);
					float y = event.getY(i);

					Point pt = new Point((int) x, (int) y);
				}

				xlenOld = Math.abs((int) event.getX(0) - (int) event.getX(1));
				ylenOld = Math.abs((int) event.getY(0) - (int) event.getY(1));
				nLenStart = Math.sqrt((double) xlenOld * xlenOld + (double) ylenOld * ylenOld);

			} else if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE && 2 == nCnt) {
				mMonitor.setTouchMove(2);
				// mMonitor.setState(3);
				for (int i = 0; i < nCnt; i++) {
					float x = event.getX(i);
					float y = event.getY(i);

					Point pt = new Point((int) x, (int) y);

				}

				int xlen = Math.abs((int) event.getX(0) - (int) event.getX(1));
				int ylen = Math.abs((int) event.getY(0) - (int) event.getY(1));

				int moveX = Math.abs(xlen - xlenOld);
				int moveY = Math.abs(ylen - ylenOld);

				double nLenEnd = Math.sqrt((double) xlen * xlen + (double) ylen * ylen);
				if (moveX < 20 && moveY < 20) {

					return false;
				}

				if (nLenEnd > nLenStart) {
					resetMonitorSize(true, nLenEnd);
				} else {
					resetMonitorSize(false, nLenEnd);
				}

				xlenOld = xlen;
				ylenOld = ylen;
				nLenStart = nLenEnd;

				return true;
			} else if (nCnt == 1) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					action_down_x = event.getRawX();
					action_down_y = event.getRawY();
					lastX = action_down_x;
					lastY = action_down_y;
					mMonitor.setTouchMove(0);
					break;
				case MotionEvent.ACTION_MOVE:

					if (mMonitor.getTouchMove() != 0)
						break;
					move_x = event.getRawX();
					move_y = event.getRawY();

					if (Math.abs(move_x - action_down_x) > 40 || Math.abs(move_y - action_down_y) > 40) {
						mMonitor.setTouchMove(1);
					}
					break;
				case MotionEvent.ACTION_UP: {
					if (mMonitor.getTouchMove() != 0) {
						break;
					}
					setViewVisible(visible);
					HiTools.hideVirtualKey(this);

					break;
				}
				}
			}
		} else if (v.getId() == R.id.btn_microphone) {

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN: {
				if (isRing) {
					iv_alarm_mark.setVisibility(View.GONE);
					iv_alarm.setSelected(false);
					isRing = !isRing;
				}

				btn_live_listen.setClickable(false);
				btn_live_listen.setImageResource(R.drawable.camhi_live_normal_speaker);
				if (System.currentTimeMillis() - oldClickTime < 1000) {
					break;
				}
				oldClickTime = System.currentTimeMillis();
				if (mRecordingState == RECORDING_STATUS_ING) {
					mCamera.PausePlayAudio();
				} else {
					mCamera.stopListening();
				}

				mCamera.startTalk();

				isTalking = true;
				break;
			}
			case MotionEvent.ACTION_MOVE: {
				break;
			}
			case MotionEvent.ACTION_UP: {
				btn_live_listen.setClickable(true);
				btn_live_listen.setImageResource(R.drawable.camhi_live_select_speaker);
				mCamera.stopTalk();
				if (mRecordingState == RECORDING_STATUS_ING) {
					mCamera.ResumePlayAudio();
				} else {
					mCamera.startListening();
				}
				// mCamera.ResumePlayAudio();
				// mCamera.startListening();
				isTalking = false;
				break;
			}
			}
		}
		return false;
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

	private int select_preset = 0;

	// 预设位
	private void clickPreset(ImageView iv) {
		@SuppressLint("InflateParams")
		View customView = getLayoutInflater().inflate(R.layout.popview_preset, null, false);

		mPopupWindow = new PopupWindow(customView);
		ColorDrawable cd = new ColorDrawable(-0000);
		mPopupWindow.setBackgroundDrawable(cd);
		mPopupWindow.setOutsideTouchable(true);
		mPopupWindow.setFocusable(true);
		mPopupWindow.setWidth(LayoutParams.WRAP_CONTENT);
		mPopupWindow.setHeight(LayoutParams.WRAP_CONTENT);

		// w:210 h:40+5*3+35*2 = 125

		/*
		 * if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) { int offsetx = -HiTools.dip2px(this, 80); int offsety = HiTools.dip2px(this, 40); mPopupWindow.showAsDropDown(iv,offsetx,offsety); } else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
		 */
		int offsetx = HiTools.dip2px(this, 20);
		int location[] = new int[2];
		iv.getLocationOnScreen(location);
		int offsety = HiTools.dip2px(this, 90);
		mPopupWindow.showAtLocation(iv, 0, location[0] - offsetx, offsety - location[1]);
		// }

		RadioGroup radio_group_preset = (RadioGroup) customView.findViewById(R.id.radio_group_preset);
		radio_group_preset.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {

				if (checkedId == R.id.radio_quality_0) {
					select_preset = 0;
				}
				if (checkedId == R.id.radio_quality_1) {
					select_preset = 1;
				}
				if (checkedId == R.id.radio_quality_2) {
					select_preset = 2;
				}
				if (checkedId == R.id.radio_quality_3) {
					select_preset = 3;
				}
				if (checkedId == R.id.radio_quality_4) {
					select_preset = 4;
				}
				if (checkedId == R.id.radio_quality_5) {
					select_preset = 5;
				}
				if (checkedId == R.id.radio_quality_6) {
					select_preset = 6;
				}
				if (checkedId == R.id.radio_quality_7) {
					select_preset = 7;
				}

				HiLog.v("onCheckedChanged:" + select_preset);
			}
		});

		Button btn_set = (Button) customView.findViewById(R.id.btn_preset_set);
		btn_set.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// isSetPTZReset = true;
				mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_PTZ_PRESET, HiChipDefines.HI_P2P_S_PTZ_PRESET.parseContent(HiChipP2P.HI_P2P_SE_CMD_CHN, HiChipDefines.HI_P2P_PTZ_PRESET_ACT_SET, select_preset));
				HiToast.showToast(LiveViewActivity.this, getString(R.string.tips_preset_set_btn));
			}
		});

		Button btn_call = (Button) customView.findViewById(R.id.btn_preset_call);
		btn_call.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_PTZ_PRESET, HiChipDefines.HI_P2P_S_PTZ_PRESET.parseContent(HiChipP2P.HI_P2P_SE_CMD_CHN, HiChipDefines.HI_P2P_PTZ_PRESET_ACT_CALL, select_preset));
			}
		});

	}

	HiChipDefines.HI_P2P_S_DISPLAY display_param = null;
	protected int filpChecked;
	protected int mirrorChecked;

	// 镜像，翻转的转换
	private void clickMirrorFlip(ImageView iv) {
		if (display_param == null) {
			return;
		}
		@SuppressLint("InflateParams")
		View customView = getLayoutInflater().inflate(R.layout.popview_mirror_flip, null, false);
		mPopupWindow = new PopupWindow(customView);
		ColorDrawable cd = new ColorDrawable(-0000);
		mPopupWindow.setBackgroundDrawable(cd);
		mPopupWindow.setOutsideTouchable(true);
		mPopupWindow.setFocusable(true);
		mPopupWindow.setWidth(LayoutParams.WRAP_CONTENT);
		mPopupWindow.setHeight(LayoutParams.WRAP_CONTENT);
		int offsetx = HiTools.dip2px(this, 0);
		int location[] = new int[2];
		iv.getLocationOnScreen(location);
		int btnh = HiTools.dip2px(this, 50 + 80 / 2);

		mPopupWindow.showAtLocation(iv, 0, location[0] - offsetx, btnh - location[1]);
		// 翻转
		SwitchButton toggle_flip = (SwitchButton) customView.findViewById(R.id.toggle_flip);
		toggle_flip.setChecked(display_param.u32Flip == 1 ? true : false);
		toggle_flip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton view, boolean isChecked) {
				filpChecked = isChecked ? 1 : 0;
				mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_DISPLAY_PARAM, null);
				showLoadingView();
				isMF = true;
			}
		});
		// 镜像
		SwitchButton toggle_mirror = (SwitchButton) customView.findViewById(R.id.toggle_mirror);
		toggle_mirror.setChecked(display_param.u32Mirror == 1 ? true : false);
		toggle_mirror.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton view, boolean isChecked) {
				mirrorChecked = isChecked ? 1 : 0;
				mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_DISPLAY_PARAM, null);
				showLoadingView();
				isMF = true;
			}
		});

	}

	@Override
	public void receiveSessionState(HiCamera arg0, int arg1) {
		if (mCamera != arg0)
			return;
		Message msg = handler.obtainMessage();
		msg.what = HiDataValue.HANDLE_MESSAGE_SESSION_STATE;
		msg.arg1 = arg1;
		msg.obj = arg0;
		handler.sendMessage(msg);

	}

	/*
	 * @Override public void callbackState(int arg0, int arg1, int arg2) {
	 * 
	 * Bundle bundle = new Bundle(); bundle.putInt("command", arg0); bundle.putInt("width", arg1); bundle.putInt("height", arg2); Message msg = handler.obtainMessage(); msg.what = HANDLE_MESSAGE_PLAY_STATE; msg.setData(bundle); handler.sendMessage(msg);
	 * 
	 * }
	 */

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		initView();
		// resetMonitorSize();

	}

	/*
	 * private class ProgressThread extends HiThread { public void run() { while(isRunning) { sleep(100);
	 * 
	 * Message msg = handler.obtainMessage(); msg.what = HiDataValue.HANDLE_MESSAGE_PROGRESSBAR_RUN; handler.sendMessage(msg); } } }
	 */

	private PopupWindow.OnDismissListener mOnDismissListener = new PopupWindow.OnDismissListener() {

		@Override
		public void onDismiss() {
			// setToolsButtonSelected(0);
		}
	};

	// 拍照，保存到本地文件夹 :/storage/sdcard1/CamHigh/Snapshot/Camera的UID/IMG_+时间+.jpg
	private void clickSnapshot() {
		if (mCamera != null) {

			if (HiTools.isSDCardValid()) {

				File rootFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/");
				File sargetFolder = new File(rootFolder.getAbsolutePath() + "/Snapshot/");
				File yargetFolder = new File(sargetFolder.getAbsolutePath() + "/" + mCamera.getUid() + "/");
				// File targetFolder=new
				// File(yargetFolder.getAbsolutePath()+"/"+getTimeForNow()+"/");
				if (!rootFolder.exists()) {
					rootFolder.mkdirs();
				}
				if (!sargetFolder.exists()) {
					sargetFolder.mkdirs();
				}
				if (!yargetFolder.exists()) {
					yargetFolder.mkdirs();
				}
				// if (!targetFolder.exists()) {
				// targetFolder.mkdir();
				// }

				String filename = HiTools.getFileNameWithTime(0);
				final String file = yargetFolder.getAbsoluteFile() + "/" + filename;

				HiLog.v("btn_live_snapshot:" + file);

				Bitmap frame = mCamera != null ? mCamera.getSnapshot() : null;
				if (frame != null && HiTools.saveImage(file, frame)) {

					SaveToPhone(file, filename);
					Toast.makeText(LiveViewActivity.this, getText(R.string.tips_snapshot_success), Toast.LENGTH_SHORT).show();
					HiLog.v("btn_live_snapshot:" + true);
				} else {
					Toast.makeText(LiveViewActivity.this, getText(R.string.tips_snapshot_failed), Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(LiveViewActivity.this, getText(R.string.tips_no_sdcard).toString(), Toast.LENGTH_SHORT).show();
			}
		}

	}

	private void SaveToPhone(String path, String fileName) {
		// 其次把文件插入到系统图库
		try {
			MediaStore.Images.Media.insertImage(getContentResolver(), path, fileName, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 最后通知图库更新
		sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + path)));

	}

	private void clickRatio(ImageView iv) {

		@SuppressLint("InflateParams")
		View customView = getLayoutInflater().inflate(R.layout.popview_resolution_ratio, null, false);

		mPopupWindow = new PopupWindow(customView);
		mPopupWindow.setOnDismissListener(mOnDismissListener);
		ColorDrawable cd = new ColorDrawable(-000);
		mPopupWindow.setBackgroundDrawable(cd);
		mPopupWindow.setOutsideTouchable(true);
		mPopupWindow.setFocusable(true);
		mPopupWindow.setWidth(LayoutParams.WRAP_CONTENT);
		mPopupWindow.setHeight(LayoutParams.WRAP_CONTENT);
		int offsetx = HiTools.dip2px(this, 10 - 2);
		int location[] = new int[2];
		iv.getLocationOnScreen(location);
		int offsety = HiTools.dip2px(this, 90 + 10);
		mPopupWindow.showAtLocation(iv, 0, location[0] - offsetx, location[1] - offsety);
		final TextView ratio_high = (TextView) customView.findViewById(R.id.ratio_high);
		final TextView ratio_fluent = (TextView) customView.findViewById(R.id.ratio_fluent);
		int videoQuality = mCamera.getVideoQuality();
		if (videoQuality == 0) {
			ratio_high.setSelected(true);
			ratio_fluent.setSelected(false);
		} else if (videoQuality == 1) {
			ratio_fluent.setSelected(true);
			ratio_high.setSelected(false);
		}
		ratio_high.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mPopupWindow.dismiss();
				ratio_high.setSelected(true);
				ratio_fluent.setSelected(false);
				switchVideoQuality(0);
			}
		});

		ratio_fluent.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mPopupWindow.dismiss();
				ratio_fluent.setSelected(true);
				ratio_high.setSelected(false);
				switchVideoQuality(1);
			}
		});
	}

	private void switchVideoQuality(int quality) {
		if (mCamera == null) {
			return;
		}
		int videoQuality = mCamera.getVideoQuality();
		videoQuality = quality;

		if (videoQuality == mCamera.getVideoQuality()) {

			return;
		}
		btn_live_record.setImageResource(R.drawable.camhi_live_normal_recording);
		txt_recording.setVisibility(View.GONE);
		mCamera.setVideoQuality(videoQuality);
		mCamera.updateInDatabase(LiveViewActivity.this);

		mCamera.stopLiveShow();
		mCamera.disconnect();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				mCamera.connect();
			}
		}, 500);

	}

	private void showLight(ImageView iv) {
		if (lightModel == 0) {
			return;
		}

		View customView = getLayoutInflater().inflate(R.layout.popview_light_set, null, false);

		mPopupWindow = new PopupWindow(customView);

		ColorDrawable cd = new ColorDrawable(-000);
		mPopupWindow.setBackgroundDrawable(cd);
		mPopupWindow.setOutsideTouchable(true);
		mPopupWindow.setFocusable(true);
		mPopupWindow.setWidth(LayoutParams.WRAP_CONTENT);
		mPopupWindow.setHeight(LayoutParams.WRAP_CONTENT);

		RadioButton[] lightRadioBtnsRES = new RadioButton[3];
		RadioButton[] lightRadioBtnsNOR = new RadioButton[2];

		RadioGroup live_view_ext_layout = (RadioGroup) customView.findViewById(R.id.live_view_ext_layout);
		RadioGroup live_view_nor_layout = (RadioGroup) customView.findViewById(R.id.live_view_nor_layout);
		lightRadioBtnsRES[0] = (RadioButton) customView.findViewById(R.id.live_view_ext_btn_normal);
		lightRadioBtnsRES[1] = (RadioButton) customView.findViewById(R.id.live_view_ext_btn_color);
		lightRadioBtnsRES[2] = (RadioButton) customView.findViewById(R.id.live_view_ext_btn_auto);
		lightRadioBtnsNOR[0] = (RadioButton) customView.findViewById(R.id.live_view_nor_btn_open);
		lightRadioBtnsNOR[1] = (RadioButton) customView.findViewById(R.id.live_view_nor_btn_close);

		if (lightModel == 2) {
			live_view_ext_layout.setVisibility(View.GONE);
			live_view_nor_layout.setVisibility(View.VISIBLE);
			if (light_info != null && light_info.u32State < 2) {
				lightRadioBtnsNOR[light_info.u32State].setChecked(true);
			}

		} else {
			live_view_ext_layout.setVisibility(View.VISIBLE);
			live_view_nor_layout.setVisibility(View.GONE);
			if (light_info_ext != null && light_info_ext.u32State < 3) {
				lightRadioBtnsRES[light_info_ext.u32State].setChecked(true);
			}

		}

		live_view_ext_layout.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup rg, int id) {
				if (light_info_ext == null) {
					HiLog.e("lightMode=" + lightModel);
					return;
				}
				switch (id) {
				case R.id.live_view_ext_btn_normal:
					light_info_ext.u32State = 0;
					break;
				case R.id.live_view_ext_btn_color:
					light_info_ext.u32State = 1;
					break;
				case R.id.live_view_ext_btn_auto:
					light_info_ext.u32State = 2;
					break;

				}

				mCamera.sendIOCtrl(HiChipDefines.HI_P2P_WHITE_LIGHT_SET_EXT, HiChipDefines.HI_P2P_WHITE_LIGHT_INFO_EXT.parseContent(light_info_ext.u32Chn, light_info_ext.u32State));

			}
		});

		live_view_nor_layout.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup rg, int id) {
				if (light_info == null) {
					HiLog.e("lightMode=" + lightModel);
					return;
				}
				switch (id) {
				case R.id.live_view_nor_btn_open:

					light_info.u32State = 0;
					break;
				case R.id.live_view_nor_btn_close:
					light_info.u32State = 1;
					break;

				}

				mCamera.sendIOCtrl(HiChipDefines.HI_P2P_WHITE_LIGHT_SET, HiChipDefines.HI_P2P_WHITE_LIGHT_INFO.parseContent(light_info.u32Chn, light_info.u32State));

			}
		});

		// width = 210 height = 90
		int offsetx = HiTools.dip2px(this, 20);
		int location[] = new int[2];
		iv.getLocationOnScreen(location);
		int offsety = HiTools.dip2px(this, 20 + 125 / 2);

		mPopupWindow.showAtLocation(iv, 0, location[0] - offsetx, offsety - location[1]);

	}

	// 拉近拉远聚焦等操作
	private void clickZoomFocus(ImageView iv) {
		@SuppressLint("InflateParams")
		View customView = getLayoutInflater().inflate(R.layout.popview_zoom_focus, null, false);

		mPopupWindow = new PopupWindow(customView);
		ColorDrawable cd = new ColorDrawable(-0000);
		mPopupWindow.setBackgroundDrawable(cd);
		mPopupWindow.setOutsideTouchable(true);
		mPopupWindow.setFocusable(true);
		mPopupWindow.setWidth(LayoutParams.WRAP_CONTENT);
		mPopupWindow.setHeight(LayoutParams.WRAP_CONTENT);

		// width = 210 height = 90
		int offsetx = HiTools.dip2px(this, 10);
		int location[] = new int[2];
		iv.getLocationOnScreen(location);
		int offsety = HiTools.dip2px(this, 90);

		mPopupWindow.showAtLocation(iv, 0, location[0] - offsetx, offsety - location[1]);

		// 拉近操作
		Button btnZoomin = (Button) customView.findViewById(R.id.btn_zoomin);
		btnZoomin.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_PTZ_CTRL, HiChipDefines.HI_P2P_S_PTZ_CTRL.parseContent(HiChipP2P.HI_P2P_SE_CMD_CHN, HiChipDefines.HI_P2P_PTZ_CTRL_ZOOMIN, HiChipDefines.HI_P2P_PTZ_MODE_RUN, (short) MyLiveViewGLMonitor.PTZ_STEP, (short) 10));

				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_PTZ_CTRL, HiChipDefines.HI_P2P_S_PTZ_CTRL.parseContent(HiChipP2P.HI_P2P_SE_CMD_CHN, HiChipDefines.HI_P2P_PTZ_CTRL_STOP, HiChipDefines.HI_P2P_PTZ_MODE_RUN, (short) MyLiveViewGLMonitor.PTZ_STEP, (short) 10));
				}
				return false;
			}
		});
		// 拉远按钮
		Button btnZoomout = (Button) customView.findViewById(R.id.btn_zoomout);
		btnZoomout.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_PTZ_CTRL, HiChipDefines.HI_P2P_S_PTZ_CTRL.parseContent(HiChipP2P.HI_P2P_SE_CMD_CHN, HiChipDefines.HI_P2P_PTZ_CTRL_ZOOMOUT, HiChipDefines.HI_P2P_PTZ_MODE_RUN, (short) MyLiveViewGLMonitor.PTZ_STEP, (short) 10));
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_PTZ_CTRL, HiChipDefines.HI_P2P_S_PTZ_CTRL.parseContent(HiChipP2P.HI_P2P_SE_CMD_CHN, HiChipDefines.HI_P2P_PTZ_CTRL_STOP, HiChipDefines.HI_P2P_PTZ_MODE_RUN, (short) MyLiveViewGLMonitor.PTZ_STEP, (short) 10));
				}
				return false;
			}
		});
		// 聚焦+
		Button btnFocusin = (Button) customView.findViewById(R.id.btn_focusin);
		btnFocusin.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_PTZ_CTRL, HiChipDefines.HI_P2P_S_PTZ_CTRL.parseContent(HiChipP2P.HI_P2P_SE_CMD_CHN, HiChipDefines.HI_P2P_PTZ_CTRL_FOCUSIN, HiChipDefines.HI_P2P_PTZ_MODE_RUN, (short) MyLiveViewGLMonitor.PTZ_STEP, (short) 10));
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_PTZ_CTRL, HiChipDefines.HI_P2P_S_PTZ_CTRL.parseContent(HiChipP2P.HI_P2P_SE_CMD_CHN, HiChipDefines.HI_P2P_PTZ_CTRL_STOP, HiChipDefines.HI_P2P_PTZ_MODE_RUN, (short) MyLiveViewGLMonitor.PTZ_STEP, (short) 10));
				}
				return false;
			}
		});
		// 聚焦-
		Button btnFocusout = (Button) customView.findViewById(R.id.btn_focusout);
		btnFocusout.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_PTZ_CTRL, HiChipDefines.HI_P2P_S_PTZ_CTRL.parseContent(HiChipP2P.HI_P2P_SE_CMD_CHN, HiChipDefines.HI_P2P_PTZ_CTRL_FOCUSOUT, HiChipDefines.HI_P2P_PTZ_MODE_RUN, (short) MyLiveViewGLMonitor.PTZ_STEP, (short) 10));
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_PTZ_CTRL, HiChipDefines.HI_P2P_S_PTZ_CTRL.parseContent(HiChipP2P.HI_P2P_SE_CMD_CHN, HiChipDefines.HI_P2P_PTZ_CTRL_STOP, HiChipDefines.HI_P2P_PTZ_MODE_RUN, (short) MyLiveViewGLMonitor.PTZ_STEP, (short) 10));
				}
				return false;
			}
		});
	}

	// 点击录像按钮，保存录像文件
	private void clickRecording(ImageView v) {
		if (mRecordingState == RECORDING_STATUS_NONE) {
			mRecordingState = RECORDING_STATUS_LOADING;
			final File cameraFolder = new File(HiDataValue.LOCAL_VIDEO_PATH + "/" + mCamera.getUid());
			if (!cameraFolder.exists()) {
				cameraFolder.mkdirs();
			}
			timer = new Timer();
			timerTask = new TimerTask() {
				@Override
				public void run() {
					if (mRecordingState == RECORDING_STATUS_ING) {
						mCamera.stopRecording();
					}
					recordFile = cameraFolder.getAbsoluteFile() + "/" + HiTools.getFileNameWithTime(1);
					long available = HiTools.getAvailableSize();
					if (available < 100&&available>0) {// 设备内存小于100M
						HiToast.showToast(LiveViewActivity.this, getString(R.string.failed_recording));
						return;
					}
					mCamera.startRecording(recordFile);
				}
			};
			timer.schedule(timerTask, 0, 10 * 60 * 1000);
			btn_live_record.setImageResource(R.drawable.camhi_live_select_recording);

		} else if (mRecordingState == RECORDING_STATUS_ING) {
			mRecordingState = RECORDING_STATUS_LOADING;
			mCamera.stopRecording();
			btn_live_record.setImageResource(R.drawable.camhi_live_normal_recording);
		}
	}

	// 点击声音按钮开始监听语音，按住喇叭说话，松开接收
	private void clickListen(ImageView iv) {
		// TODO
		if (mRecordingState == RECORDING_STATUS_ING) {// 正在录像中...
			if (timer != null) {
				timer.cancel();
				timer = null;
			}
			if (timerTask != null) {
				timerTask.cancel();
				timerTask = null;
			}
			mCamera.stopRecording();
			final File cameraFolder = new File(HiDataValue.LOCAL_VIDEO_PATH + "/" + mCamera.getUid());
			if (!cameraFolder.exists()) {
				cameraFolder.mkdirs();
			}
			timer = new Timer();
			timerTask = new TimerTask() {

				@Override
				public void run() {
					recordFile = cameraFolder.getAbsoluteFile() + "/" + HiTools.getFileNameWithTime(1);
					double available = HiTools.getAvailableSize();
					if (available < 100&&available>0) {// 设备内存小于100M
						HiToast.showToast(LiveViewActivity.this, getString(R.string.failed_recording));
						return;
					}
					mCamera.startRecording(recordFile);
				}
			};
			timer.schedule(timerTask, 100, 10 * 60 * 1000);
		}
		if (isListening) {
			iv.setImageResource(R.drawable.camhi_live_normal_speaker);
			mCamera.stopListening();
			btn_microphone.setVisibility(View.GONE);
		} else {
			iv.setImageResource(R.drawable.camhi_live_select_speaker);
			btn_microphone.setVisibility(View.VISIBLE);
			mCamera.startListening();
		}
		isListening = !isListening;

	}

	public String getTimeForNow() {
		SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
		return sf.format(System.currentTimeMillis());
	}

	@Override
	public void callbackPlayUTC(HiCamera arg0, int arg1) {

	}

	@Override
	public void callbackState(HiCamera camera, int arg1, int arg2, int arg3) {
		if (mCamera != camera)
			return;
		Bundle bundle = new Bundle();
		bundle.putInt("command", arg1);
		bundle.putInt("width", arg2);
		bundle.putInt("height", arg3);
		Message msg = handler.obtainMessage();
		msg.what = HiDataValue.HANDLE_MESSAGE_PLAY_STATE;
		msg.setData(bundle);
		handler.sendMessage(msg);
	}

	/**
	 * 主要是处理录像时小红点闪烁问题
	 */
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == 110) {
				if (mIvRecording.getVisibility() == View.GONE) {
					mIvRecording.setVisibility(View.VISIBLE);
				} else {
					mIvRecording.setVisibility(View.GONE);
				}
				mHandler.sendEmptyMessageDelayed(110, 1000);
			}

		};

	};

	private void setViewWhetherClick(boolean whetherClick) {
		btn_live_light.setClickable(whetherClick);
		btn_live_preset.setClickable(whetherClick);
		btn_live_zoom_focus.setClickable(whetherClick);
		btn_live_mirror_flip.setClickable(whetherClick);
		btn_live_listen.setClickable(whetherClick);
		btn_live_snapshot.setClickable(whetherClick);
		btn_live_record.setClickable(whetherClick);
		resolution_ratio.setClickable(whetherClick);
	}

	private void dismissLoadingView() {
		mIvLoading2.clearAnimation();
		mIvLoading2.setVisibility(View.GONE);
	}
}
