package com.hichip.activity.FishEye;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.customview.CircularView;
import com.hichip.base.HiLog;
import com.hichip.callback.ICameraIOSessionCallback;
import com.hichip.callback.ICameraPlayStateCallback;
import com.hichip.control.HiCamera;
import com.hichip.control.HiGLMonitor;
import com.hichip1.R;
import com.thecamhi.base.HiToast;
import com.thecamhi.base.HiTools;
import com.thecamhi.base.MyLiveViewGLMonitor;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;
import com.thecamhi.main.HiActivity;
import com.thecamhi.utils.BitmapUtils;
import com.thecamhi.utils.SharePreUtils;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class FishEyeActivity extends HiActivity implements OnClickListener, OnTouchListener, ICameraIOSessionCallback, ICameraPlayStateCallback {
	private MyCamera mMyCamera;
	public  MyLiveViewGLMonitor mMonitor;
	private Button btn_return;
	public  RelativeLayout rl_guide;
	private RelativeLayout rl_voice, rl_talk, rl_snapshot, rl_record_video, rl_cruise;
	private ImageView iv_voice, iv_recording, iv_talk, iv_loading2, iv_live_cruise;
	private boolean mIsCruise = false;
	public  static int mFrameMode = 1; // 1.圆 2.圆柱 3.二画面 4.四画面 5.碗
	private ImageView mIvFullScreen, iv_record_video;
	public  int mWallMode = 1;// 1-壁装全景 0-壁装放大局部画面
	private RelativeLayout rl_top;
	private LinearLayout ll_buttom;
	private TextView txt_recording, tv_timezone, tv_install_mode_, tv_know, tv_one, tv_two;
	private int mCameraVideoQuality;
	private int RECORDING_STATUS_NONE = 0;
	private int RECORDING_STATUS_LOADING = 1;
	private int RECORDING_STATUS_ING = 2;
	private int mRecordingState = RECORDING_STATUS_NONE;
	private Timer timer;
	private TimerTask timerTask;
	private String recordFile;
	private boolean isListening = false;
	private boolean isTalking = false;
	public  static int misFullScreen = 1; // 1是竖屏 0是横屏
	private RadioGroup rg_view_model;
	public  RadioButton rbtn_wall_overallview, rbtn_circle;
	private RadioButton rbtn_cylinder, rbtn_two, rbtn_four, rbtn_bowl;
	private CircularView circular;
	private RelativeLayout rl_view_model;
	private LinearLayout ll_four_bg_live, ll_focus_one_live, ll_focus_two_live, ll_focus_three_live, ll_focus_four_live;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_fish_eye);
		getIntentData();
		initView();
		initFishView();
		// 是否显示引导界面
		if (mMyCamera.mInstallMode == 0) {
			rl_guide.setVisibility(mMyCamera.isFirst ? View.GONE : View.VISIBLE);
		}
		// 显示
		setListeners();
	}


	@Override
	protected void onResume() {
		super.onResume();
		showLoadingView();
		if (mMyCamera != null) {
			new Thread() {
				public void run() {
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							mMyCamera.startLiveShow(mMyCamera.getVideoQuality(), mMonitor);
						}
					});
				};
			}.start();
			mMyCamera.registerIOSessionListener(this);
			mMyCamera.registerPlayStateListener(this);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		if (timerTask != null) {
			timerTask.cancel();
			timerTask = null;
		}

		if (mMyCamera != null) {
			mMyCamera.stopLiveShow();
			mMyCamera.unregisterIOSessionListener(this);
			mMyCamera.unregisterPlayStateListener(this);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mFrameMode = 1;
		misFullScreen = 1;
		if (mHandler != null) {
			mHandler.removeCallbacksAndMessages(null);
		}
	}

	private void initFishView() {
		mMonitor.SetViewType(mMonitor.TOP_ALL_VIEW);/* 设置为鱼眼顶装 */
		// setScreenSize 竖屏的时候用宽宽 横屏的时候用宽高
		mMonitor.SetScreenSize(getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getWidth());
		// mMonitor.StartFishView(" ", 0, 0);
		if (mMyCamera.mInstallMode == 0) {
			mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_CIRCLE, mFrameMode);
			handTopModelView();
			rbtn_circle.setChecked(true);
		} else {
			mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_SIDE, 1);
			handWallModelView();
			rbtn_wall_overallview.setChecked(true);
		}
		mMonitor.setCamera(mMyCamera);
		mMonitor.SetCruise(mIsCruise); /* 巡航开启 */
		mMyCamera.setVideoQuality(0);
		mMyCamera.setLiveShowMonitor(mMonitor);
	}

	private void setListeners() {
		mMonitor.setOnTouchListener(this);
		btn_return.setOnClickListener(this);
		mIvFullScreen.setOnClickListener(this);
		rl_voice.setOnClickListener(this);
		tv_install_mode_.setOnClickListener(this);
		rl_talk.setOnClickListener(this);
		rl_snapshot.setOnClickListener(this);
		rl_record_video.setOnClickListener(this);
		rl_cruise.setOnClickListener(this);
		tv_know.setOnClickListener(this);
		rg_view_model.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
				case R.id.rbtn_circle:// 圆
					ll_four_bg_live.setVisibility(View.GONE);
					if (mMyCamera.mInstallMode == 0) {
						mFrameMode = 1;
						mIvFullScreen.setVisibility(View.GONE);
						mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_CIRCLE, mFrameMode);
						mMonitor.mIsZoom = false;
					} else {
						mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_SIDE, 0);
						mWallMode = 0;
					}
					break;
				case R.id.rbtn_cylinder:// 圆柱
					mFrameMode = 2;
					mMonitor.mIsZoom = false;
					mIvFullScreen.setVisibility(View.GONE);
					ll_four_bg_live.setVisibility(View.GONE);
					mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_CARTOON, 1);
					break;
				case R.id.rbtn_two:// 二画面
					mFrameMode = 3;
					mMonitor.mIsZoom = false;
					mIvFullScreen.setVisibility(View.VISIBLE);
					ll_four_bg_live.setVisibility(View.GONE);
					mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_COLUMN, 2);
					break;
				case R.id.rbtn_four:// 四画面
					mFrameMode = 4;
					mMonitor.mIsZoom = false;
					mIvFullScreen.setVisibility(View.VISIBLE);
					ll_four_bg_live.setVisibility(View.VISIBLE);
					mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_CIRCLE, mFrameMode);
					break;
				case R.id.rbtn_wall_overallview: // 1 是壁装全景
					ll_four_bg_live.setVisibility(View.GONE);
					mIsCruise=false;
					iv_live_cruise.setSelected(mIsCruise);
					mMonitor.SetCruise(mIsCruise);
					mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_SIDE, 1);
					mWallMode = 1;
					break;
				case R.id.rbtn_bowl: // 碗
					ll_four_bg_live.setVisibility(View.GONE);
					mIvFullScreen.setVisibility(View.GONE);
					mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_CARTOON, 0);// 立体碗
					mFrameMode = 5;
					break;
				}
			}
		});

	}

	private void initView() {
		mMonitor = (MyLiveViewGLMonitor) findViewById(R.id.monitor_live_view);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getWidth());
		mMonitor.setLayoutParams(params);
		btn_return = (Button) findViewById(R.id.btn_return);
		rl_voice = (RelativeLayout) findViewById(R.id.rl_voice);
		iv_loading2 = (ImageView) findViewById(R.id.iv_loading2);
		rl_talk = (RelativeLayout) findViewById(R.id.rl_talk);
		iv_talk = (ImageView) findViewById(R.id.iv_talk);
		mIvFullScreen = (ImageView) findViewById(R.id.iv_full_screen);
		mIvFullScreen.setVisibility(mFrameMode == 4 || mFrameMode == 3 ? View.VISIBLE : View.GONE);
		rl_top = (RelativeLayout) findViewById(R.id.rl_top);
		ll_buttom = (LinearLayout) findViewById(R.id.ll_buttom);
		txt_recording = (TextView) findViewById(R.id.txt_recording);
		iv_recording = (ImageView) findViewById(R.id.iv_recording);
		iv_voice = (ImageView) findViewById(R.id.iv_voice);
		tv_timezone = (TextView) findViewById(R.id.tv_timezone);
		tv_install_mode_ = (TextView) findViewById(R.id.tv_install_mode_);
		tv_install_mode_.setText(mMyCamera.mInstallMode == 0 ? getString(R.string.fish_top) : getString(R.string.fish_wall));
		rl_snapshot = (RelativeLayout) findViewById(R.id.rl_snapshot);
		rl_record_video = (RelativeLayout) findViewById(R.id.rl_record_video);
		iv_record_video = (ImageView) findViewById(R.id.iv_record_video);
		rl_cruise = (RelativeLayout) findViewById(R.id.rl_cruise);
		iv_live_cruise = (ImageView) findViewById(R.id.iv_live_cruise);
		rg_view_model = (RadioGroup) findViewById(R.id.rg_view_model);
		rbtn_circle = (RadioButton) findViewById(R.id.rbtn_circle);
		rbtn_cylinder = (RadioButton) findViewById(R.id.rbtn_cylinder);
		rbtn_two = (RadioButton) findViewById(R.id.rbtn_two);
		rbtn_four = (RadioButton) findViewById(R.id.rbtn_four);
		rbtn_wall_overallview = (RadioButton) findViewById(R.id.rbtn_wall_overallview);
		tv_know = (TextView) findViewById(R.id.tv_know);
		rl_guide = (RelativeLayout) findViewById(R.id.rl_guide);
		tv_one = (TextView) findViewById(R.id.tv_one);
		tv_two = (TextView) findViewById(R.id.tv_two);
		circular = (CircularView) findViewById(R.id.circular);
		circular.setMonitor(mMonitor);
		rbtn_bowl = (RadioButton) findViewById(R.id.rbtn_bowl);
		rl_view_model = (RelativeLayout) findViewById(R.id.rl_view_model);
		ll_focus_one_live = (LinearLayout) findViewById(R.id.ll_focus_one_live);
		ll_focus_one_live.requestFocus();
		ll_focus_two_live = (LinearLayout) findViewById(R.id.ll_focus_two_live);
		ll_focus_three_live = (LinearLayout) findViewById(R.id.ll_focus_three_live);
		ll_focus_four_live = (LinearLayout) findViewById(R.id.ll_focus_four_live);
		ll_four_bg_live = (LinearLayout) findViewById(R.id.ll_four_bg_live);
		ll_four_bg_live.setLayoutParams(params);
	}

	private void getIntentData() {
		String uid = getIntent().getExtras().getString(HiDataValue.EXTRAS_KEY_UID);
		for (MyCamera camera : HiDataValue.CameraList) {
			if (camera.getUid().equals(uid)) {
				this.mMyCamera = camera;
				mCameraVideoQuality = mMyCamera.getVideoQuality();
				break;
			}
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.rl_talk:
			if (mRecordingState == RECORDING_STATUS_ING) {
				mMyCamera.PausePlayAudio();
			} else {
				mMyCamera.stopListening();
			}
			if (isTalking) {
				mMyCamera.stopTalk();
			} else {
				mMyCamera.startTalk();
			}
			if (isListening) {
				iv_voice.setSelected(false);
				mMyCamera.stopListening();
				isListening = !isListening;
			}
			isTalking = !isTalking;
			iv_talk.setSelected(isTalking);

			break;
		case R.id.btn_return:
			finish();
			break;
		case R.id.iv_full_screen:
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			} else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
			break;
		case R.id.rl_voice:
			if (HiDataValue.ANDROID_VERSION >= 6 && (!HiTools.checkPermission(FishEyeActivity.this, Manifest.permission.RECORD_AUDIO) || !HiTools.checkPermission(FishEyeActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
				showAlertDialog();
				return;
			}
			clickListen(v);
			break;
		case R.id.rl_snapshot:
			if (HiDataValue.ANDROID_VERSION >= 6 && (!HiTools.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
				showAlertDialog();
				return;
			}
			clickSnapshot();
			break;
		case R.id.rl_record_video:
			if (HiDataValue.ANDROID_VERSION >= 6 && (!HiTools.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
				showAlertDialog();
				return;
			}
			clickRecording(v);
			break;
		case R.id.rl_cruise:
			iv_live_cruise.setSelected(mIsCruise == false ? true : false);
			mMonitor.SetCruise(mIsCruise = !mIsCruise);
			if(mMyCamera.mInstallMode==1&&mWallMode==1){//壁装全景  
				mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_SIDE, 0);//壁装局部放大
				mWallMode=0;
				rbtn_circle.setChecked(true);
			}
			break;
		case R.id.tv_know:
			SharePreUtils.putBoolean("cache", FishEyeActivity.this, "isFirst", true);
			mMyCamera.isFirst = true;
			rl_guide.setVisibility(View.GONE);
			break;
		case R.id.tv_install_mode_:
			View customView = View.inflate(FishEyeActivity.this, R.layout.pup_install_mode, null);
			final PopupWindow pWindow = new PopupWindow(customView);
			ColorDrawable cd = new ColorDrawable(-0000);
			pWindow.setBackgroundDrawable(cd);
			pWindow.setOutsideTouchable(true);
			pWindow.setFocusable(true);
			pWindow.setWidth(LayoutParams.WRAP_CONTENT);
			pWindow.setHeight(LayoutParams.WRAP_CONTENT);
			pWindow.setAnimationStyle(R.style.mypopwindow_anim_style);
			int[] location = new int[2];
			v.getLocationOnScreen(location);
			int offsetY = HiTools.dip2px(FishEyeActivity.this, 45);
			pWindow.showAtLocation(v, 0, location[0], location[1] + offsetY);
			final LinearLayout ll_top = (LinearLayout) customView.findViewById(R.id.ll_top);
			final LinearLayout ll_wall = (LinearLayout) customView.findViewById(R.id.ll_wall);
			if (mMyCamera.mInstallMode == 0) {
				ll_top.setSelected(true);
			} else {
				ll_wall.setSelected(true);
			}
			final Animation anim = AnimationUtils.loadAnimation(FishEyeActivity.this, R.anim.alpha_view_model);
			// 顶装(圆)
			ll_top.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mMyCamera.isFirst = SharePreUtils.getBoolean("cache", FishEyeActivity.this, "isFirst");
					rl_guide.setVisibility(mMyCamera.isFirst ? View.GONE : View.VISIBLE);
					tv_install_mode_.setText(getString(R.string.fish_top));
					mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_CIRCLE, 1);
					mMonitor.mIsZoom = false;
					ll_top.setSelected(true);
					ll_wall.setSelected(false);
					pWindow.dismiss();
					mMyCamera.mInstallMode = 0;
					SharePreUtils.putInt("mInstallMode", FishEyeActivity.this, mMyCamera.getUid(), mMyCamera.mInstallMode);

					rbtn_circle.setChecked(true);
					rl_view_model.startAnimation(anim);
					// rg_view_model.startAnimation(anim);

					handTopModelView();
				}

			});
			ll_wall.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					tv_install_mode_.setText(getString(R.string.fish_wall));
					mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_SIDE, 1);// 1 才是全景
					mMonitor.mIsZoom = false;
					ll_top.setSelected(false);
					ll_wall.setSelected(true);
					pWindow.dismiss();
					mMyCamera.mInstallMode = 1;
					SharePreUtils.putInt("mInstallMode", FishEyeActivity.this, mMyCamera.getUid(), mMyCamera.mInstallMode);

					mIvFullScreen.setVisibility(View.GONE);
					rbtn_wall_overallview.setChecked(true);
					// rg_view_model.startAnimation(anim);
					rl_view_model.startAnimation(anim);
					handWallModelView();
				}

			});
			break;
		}
	}

	private void handTopModelView() {
		rbtn_cylinder.setVisibility(View.VISIBLE);
		rbtn_two.setVisibility(View.VISIBLE);
		rbtn_four.setVisibility(View.VISIBLE);
		rbtn_wall_overallview.setVisibility(View.GONE);
		rbtn_bowl.setVisibility(View.VISIBLE);
	}

	private void handWallModelView() {
		rbtn_cylinder.setVisibility(View.GONE);
		rbtn_two.setVisibility(View.GONE);
		rbtn_four.setVisibility(View.GONE);
		rbtn_bowl.setVisibility(View.GONE);
		rbtn_wall_overallview.setVisibility(View.VISIBLE);
	}

	private void clickSnapshot() {
		if (mMyCamera != null) {
			if (HiTools.isSDCardValid()) {
				File rootFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/");
				File sargetFolder = new File(rootFolder.getAbsolutePath() + "/Snapshot/");
				File yargetFolder = new File(sargetFolder.getAbsolutePath() + "/" + mMyCamera.getUid() + "/");
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
				String filename = HiTools.getFileNameWithTime(0);
				final String file = yargetFolder.getAbsoluteFile() + "/" + filename;

				Bitmap frame = mMyCamera != null ? mMyCamera.getSnapshot() : null;
				if (frame != null && HiTools.saveImage(file, frame)) {
					SaveToPhone(file, filename);
					Toast.makeText(this, getText(R.string.tips_snapshot_success), Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this, getText(R.string.tips_snapshot_failed), Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(this, getText(R.string.tips_no_sdcard).toString(), Toast.LENGTH_SHORT).show();
			}
		}

	}

	private void SaveToPhone(String path, String fileName) {
		try {
			MediaStore.Images.Media.insertImage(getContentResolver(), path, fileName, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + path)));
	}

	// 点击声音按钮开始监听语音，按住喇叭说话，松开接收
	private void clickListen(View iv) {
		if (mRecordingState == RECORDING_STATUS_ING) {// 正在录像中...
			if (timer != null) {
				timer.cancel();
				timer = null;
			}
			if (timerTask != null) {
				timerTask.cancel();
				timerTask = null;
			}
			mMyCamera.stopRecording();
			final File cameraFolder = new File(HiDataValue.LOCAL_VIDEO_PATH + "/" + mMyCamera.getUid());
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
						HiToast.showToast(FishEyeActivity.this, getString(R.string.failed_recording));
						return;
					}
					mMyCamera.startRecording(recordFile);
				}
			};
			timer.schedule(timerTask, 100, 10 * 60 * 1000);
		}
		if (isListening) {
			mMyCamera.stopListening();
		} else {
			mMyCamera.startListening();
		}
		if (isTalking) {
			iv_talk.setSelected(false);
			mMyCamera.stopTalk();
			isTalking = !isTalking;
		}
		isListening = !isListening;
		iv_voice.setSelected(isListening);

	}

	private void clickRecording(View v) {
		//iv_record_video.setSelected(mRecordingState == RECORDING_STATUS_NONE ? true : false);
		if (mRecordingState == RECORDING_STATUS_NONE) {
			mRecordingState = RECORDING_STATUS_LOADING;
			final File cameraFolder = new File(HiDataValue.LOCAL_VIDEO_PATH + "/" + mMyCamera.getUid());
			if (!cameraFolder.exists()) {
				cameraFolder.mkdirs();
			}
			timer = new Timer();
			timerTask = new TimerTask() {
				@Override
				public void run() {
					if (mRecordingState == RECORDING_STATUS_ING) {
						mMyCamera.stopRecording();
					}
					recordFile = cameraFolder.getAbsoluteFile() + "/" + HiTools.getFileNameWithTime(1);
					double available =HiTools.getAvailableSize();
					if (available < 100&&available>0) {
						HiToast.showToast(FishEyeActivity.this, getString(R.string.failed_recording));
						return;
					}
					mMyCamera.startRecording(recordFile);
				}
			};
			timer.schedule(timerTask, 0, 10 * 60 * 1000);
			iv_record_video.setSelected(true);

		} else if (mRecordingState == RECORDING_STATUS_ING) {
			mRecordingState = RECORDING_STATUS_LOADING;
			mMyCamera.stopRecording();
			iv_record_video.setSelected(false);
		}
	}

	private float action_down_X;
	private float action_down_Y;

	private float move_X;
	private float move_Y;
	private int xlenOld;
	private int ylenOld;
	private double nLenStart;

	// Moniter手势处理
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v.getId() == R.id.monitor_live_view) {
			int nCnt = event.getPointerCount();
			if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN && 2 == nCnt) {// 两个手指
				mMonitor.setTouchMove(2);
				for (int i = 0; i < nCnt; i++) {
					float x = event.getX(i);
					float y = event.getY(i);
				}
				xlenOld = Math.abs((int) event.getX(0) - (int) event.getX(1));
				ylenOld = Math.abs((int) event.getY(0) - (int) event.getY(1));
				nLenStart = Math.sqrt((double) xlenOld * xlenOld + (double) ylenOld * ylenOld);
			} else if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE && 2 == nCnt) {
				mMonitor.setTouchMove(2);
				for (int i = 0; i < nCnt; i++) {
					float x = event.getX(i);
					float y = event.getY(i);
				}
				int xlen = Math.abs((int) event.getX(0) - (int) event.getX(1));
				int ylen = Math.abs((int) event.getY(0) - (int) event.getY(1));
				int moveX = Math.abs(xlen - xlenOld);
				int moveY = Math.abs(ylen - ylenOld);
				double nLenEnd = Math.sqrt((double) xlen * xlen + (double) ylen * ylen);
				if (moveX < 10 && moveY < 10) {
					return true;
				}
				if (nLenEnd > nLenStart) {// 放大
					mMonitor.SetZoom(true);
					mMonitor.SetZoom(true);
					mMonitor.SetZoom(true);
					mMonitor.SetZoom(true);
				} else {// 缩小
					mMonitor.SetZoom(false);
					mMonitor.SetZoom(false);
					mMonitor.SetZoom(false);
					mMonitor.SetZoom(false);
				}
				xlenOld = xlen;
				ylenOld = ylen;
				nLenStart = nLenEnd;
				return true;
			} else if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP && 2 == nCnt) {
				mMonitor.setTouchMove(0);
			} else if (nCnt == 1) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					float x = event.getRawX();
					float y = event.getRawY();
					if (mFrameMode == 4) {
						float screen_width = getWindowManager().getDefaultDisplay().getWidth();
						float screen_height=getWindowManager().getDefaultDisplay().getHeight();
						float pxTopview = HiTools.dip2px(this, 45) + getStatusBarHeight();

						Animation alphaAnimation = new AlphaAnimation(1, 0.1f);
						alphaAnimation.setDuration(666);
						alphaAnimation.setInterpolator(new LinearInterpolator());
						alphaAnimation.setRepeatCount(4);
						alphaAnimation.setRepeatMode(Animation.REVERSE);
                       
						// 区域 1
						boolean area_one=false; 
						// 区域2
						boolean area_two=false;
						// 区域3
						boolean area_there=false;
						// 区域4
						boolean area_four=false;
						if(FishEyeActivity.misFullScreen==1){//竖屏
							area_one= x < screen_width / 2 && y > pxTopview && y < pxTopview + screen_width / 2;
							area_two = x > screen_width / 2 && x < screen_width && y < pxTopview + screen_width / 2 && y > pxTopview;
							area_there = x < screen_width / 2 && y > pxTopview + screen_width / 2 && y < screen_width + pxTopview;
							area_four = x > screen_width / 2 && y > pxTopview + screen_width / 2 && y < screen_width + pxTopview;
						}else {//横屏
							Log.i("tedu", "--横屏--》");
							// 区域 1
							area_one = x < screen_width / 2 && y < screen_height / 2;
							// 区域2
							area_two = x > screen_width / 2 && y < screen_height / 2;
							// 区域3
							area_there = x <screen_width / 2 && y > screen_height / 2;
							// 区域4
							area_four = x > screen_width / 2 && y > screen_height / 2;
						}
						if (area_one) {
							Log.i("tedu", "--area_one-->");
							ll_focus_one_live.requestFocus();
							ll_focus_one_live.startAnimation(alphaAnimation);
						} else if (area_two) {
							Log.i("tedu", "--area_two-->");
							ll_focus_two_live.requestFocus();
							ll_focus_two_live.startAnimation(alphaAnimation);
						} else if (area_there) {
							Log.i("tedu", "--area_there-->");
							ll_focus_three_live.requestFocus();
							ll_focus_three_live.startAnimation(alphaAnimation);
						} else if (area_four) {
							Log.i("tedu", "--area_four-->");
							ll_focus_four_live.requestFocus();
							ll_focus_four_live.startAnimation(alphaAnimation);
						}
					}

					action_down_X = event.getRawX();
					action_down_Y = event.getRawY();
					mMonitor.SetCruise(false);
					mHandler.removeCallbacksAndMessages(null);
					mMonitor.setTouchMove(0);
					break;
				case MotionEvent.ACTION_MOVE:
					if (mMonitor.getTouchMove() != 0) {
						break;
					}
					break;
				case MotionEvent.ACTION_UP:
					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							mMonitor.SetCruise(mIsCruise);
						}
					}, 3000);

					break;
				}

			}

		}
		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		android.widget.RelativeLayout.LayoutParams params = null;
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			misFullScreen = 1;
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			rl_top.setVisibility(View.VISIBLE);
			ll_buttom.setVisibility(View.VISIBLE);
			mIvFullScreen.setImageResource(R.drawable.full_screen);
			params = new RelativeLayout.LayoutParams(getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getWidth());
			mMonitor.SetScreenSize(getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getWidth());
		} else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			misFullScreen = 2;
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			rl_top.setVisibility(View.GONE);
			ll_buttom.setVisibility(View.GONE);
			mIvFullScreen.setImageResource(R.drawable.narrow_screen);
			params = new RelativeLayout.LayoutParams(getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getHeight());
			mMonitor.SetScreenSize(getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getHeight());
		}
		if (params != null) {
			mMonitor.setLayoutParams(params);
			ll_four_bg_live.setLayoutParams(params);
		}
	}

	private void showAlertDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(FishEyeActivity.this);
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

	@Override
	public void receiveIOCtrlData(HiCamera arg0, int arg1, byte[] arg2, int arg3) {
		// if(arg0!=mMyCamera||mMonitor==null||mMyCamera.mVideoPlayProperty==null){
		// return;
		// }
		// Bundle bundle=new Bundle();
		// bundle.putByteArray(HiDataValue.EXTRAS_KEY_DATA, arg2);
		// Message msg=Message.obtain();
		// msg.what=HiDataValue.HANDLE_MESSAGE_RECEIVE_IOCTRL;
		// msg.arg1=arg1;
		// msg.arg2=arg3;
		// msg.setData(bundle);
		// mHandler.sendMessage(msg);
	}

	@Override
	public void receiveSessionState(HiCamera arg0, int arg1) {
		if (arg0 != mMyCamera)
			return;
		Message message = Message.obtain();
		message.what = HiDataValue.HANDLE_MESSAGE_SESSION_STATE;
		message.arg1 = arg1;
		message.obj = arg0;
		mHandler.sendMessage(message);

	}

	@Override
	public void callbackPlayUTC(HiCamera arg0, int arg1) {
		if (arg0 != mMyCamera) {
			return;
		}
	}

	@Override
	public void callbackState(HiCamera arg0, int arg1, int arg2, int arg3) {
		if (mMyCamera != arg0)
			return;
		Bundle bundle = new Bundle();
		bundle.putInt("command", arg1);
		bundle.putInt("width", arg2);
		bundle.putInt("height", arg3);
		Message msg = mHandler.obtainMessage();
		msg.what = HiDataValue.HANDLE_MESSAGE_PLAY_STATE;
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 110:
				if (iv_recording.getVisibility() == View.INVISIBLE) {
					iv_recording.setVisibility(View.VISIBLE);
				} else {
					iv_recording.setVisibility(View.INVISIBLE);
				}
				mHandler.sendEmptyMessageDelayed(110, 1000);
				break;
			case HiDataValue.HANDLE_MESSAGE_SESSION_STATE:
				handSessionState(msg);
				break;
			case HiDataValue.HANDLE_MESSAGE_PLAY_STATE:
				handPalyState(msg);
				break;
			// case HiDataValue.HANDLE_MESSAGE_RECEIVE_IOCTRL:
			// switch (msg.arg1) {
			// case HiChipDefines.HI_P2P_START_LIVE:
			// mMonitor.StartFishView("abc", mMyCamera.mVideoPlayProperty.width, mMyCamera.mVideoPlayProperty.heigth);
			// break;
			// }
			// break;
			}
		}

		private void handPalyState(android.os.Message msg) {
			Bundle bundle = msg.getData();
			int command = bundle.getInt("command");
			switch (command) {
			case ICameraPlayStateCallback.PLAY_STATE_START:
				dismissLoadingView();
				Bitmap frame = mMyCamera != null ? mMyCamera.getSnapshot() : null;
				if (frame != null) {
					Bitmap bitmap = BitmapUtils.ImageCrop(frame);
					saveSnapshot(bitmap);
				}
				break;
			// 本地录像开始
			case ICameraPlayStateCallback.PLAY_STATE_RECORDING_START:
				mRecordingState = RECORDING_STATUS_ING;
				txt_recording.setVisibility(View.VISIBLE);
				iv_recording.setVisibility(View.VISIBLE);
				mHandler.sendEmptyMessage(110);
				break;
			// 本地录像结束
			case ICameraPlayStateCallback.PLAY_STATE_RECORDING_END:
			case ICameraPlayStateCallback.PLAY_STATE_RECORD_ERROR:
				mRecordingState = RECORDING_STATUS_NONE;
				txt_recording.setVisibility(View.INVISIBLE);
				iv_recording.setVisibility(View.INVISIBLE);
				mHandler.removeCallbacksAndMessages(null);
				File file = new File(recordFile);
				if (file.length() <= 1024 && file.isFile() && file.exists()) {
					file.delete();
				}
				break;
			}
		}

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
						HiTools.saveBitmap(frame, sargetFolder.getAbsolutePath() + "/" + mMyCamera.getUid());
						HiLog.v(sargetFolder.getAbsolutePath() + "/" + mMyCamera.getUid());
						mMyCamera.snapshot = frame;
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

		private void handSessionState(android.os.Message msg) {
			switch (msg.arg1) {
			case HiCamera.CAMERA_CONNECTION_STATE_DISCONNECTED:
				showLoadingView();
				if (mMyCamera != null) {
					mMyCamera.stopLiveShow();
					if (isListening) {
						isListening = false;
					}
				}
				break;
			case HiCamera.CAMERA_CONNECTION_STATE_LOGIN:
				if (mCameraVideoQuality != mMyCamera.getVideoQuality()) {
					mMyCamera.stopRecording();
					if (timer != null) {
						timer.cancel();
						timer = null;
					}
					if (timerTask != null) {
						timerTask.cancel();
						timerTask = null;
					}
					mCameraVideoQuality = mMyCamera.getVideoQuality();
				}
				mMyCamera.startLiveShow(mMyCamera.getVideoQuality(), mMonitor);
				break;
			case HiCamera.CAMERA_CONNECTION_STATE_WRONG_PASSWORD:
				break;
			case HiCamera.CAMERA_CONNECTION_STATE_CONNECTING:
				break;
			case HiCamera.CAMERA_CHANNEL_STREAM_ERROR:
				break;

			}
		};
	};

	private void showLoadingView() {
		Animation rotateAnim = AnimationUtils.loadAnimation(FishEyeActivity.this, R.anim.rotate);
		iv_loading2.setVisibility(View.VISIBLE);
		iv_loading2.startAnimation(rotateAnim);
	}

	private void dismissLoadingView() {
		iv_loading2.clearAnimation();
		iv_loading2.setVisibility(View.GONE);
	}

	public int getStatusBarHeight() {
		int result = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

}
