package com.hichip.activity.FishEye;

import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.hichip.callback.PlayLocalFileCallback;
import com.hichip.control.HiGLMonitor;
import com.hichip.sdk.PlayLocal;
import com.hichip1.R;
import com.thecamhi.activity.VideoLocalActivity;
import com.thecamhi.base.HiToast;
import com.thecamhi.base.HiTools;
import com.thecamhi.base.MyPlaybackGLMonitor;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;
import com.thecamhi.main.HiActivity;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FishEyePlaybackLocalActivity extends HiActivity implements PlayLocalFileCallback, OnClickListener, OnTouchListener, OnCheckedChangeListener {
	private String filePath;
	private MyPlaybackGLMonitor mMonitor;
	private PlayLocal playLocal;
	private ImageView mIvPausePlay, mIvFastforward, btn_return;
	private TextView mTvSpeed, tv_install;
	private int mSpeed = 0; // 0——没有倍数 2——二倍 4——四倍 8——八倍;
	private boolean mIsEnd = false;
	private LinearLayout mLlCurrPro;
	private TextView mTvCurrPro, mTvDuraTime, tv_tit, play_view_model;
	public LinearLayout mLlPlay, ll_top;
	private MyCamera mMyCamera;
	private int screen_width;
	private int screen_height;
	private String title;
	private RelativeLayout rl_view_model;
	private RadioGroup rg_view_model;
	public RadioButton rbtn_wall_overallview, rbtn_circle;
	private LinearLayout ll_focus_one, ll_four_bg, ll_focus_two, ll_focus_three, ll_focus_four;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE); // 设置无标题
		setContentView(R.layout.activity_fisheye_playback_local);
		getIntentData();
		initViewAndData();
		setListeners();
	}

	private void setListeners() {
		mMonitor.setOnTouchListener(this);
		mIvPausePlay.setOnClickListener(this);
		mIvFastforward.setOnClickListener(this);
		play_view_model.setOnClickListener(this);
		rl_view_model.setOnClickListener(this);
		rg_view_model.setOnCheckedChangeListener(this);
		btn_return.setOnClickListener(this);
		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				int count = seekBar.getProgress();
				final int pre = count / 1000;
				if (playLocal != null) {
					playLocal.PlayLocal_Speed(0, 0);
					// 只传时间,不传百分比了
					startVideoPath();
					if (mIsEnd) {
						playLocal.setLiveShowMonitor(mMonitor);
					}
					playLocal.PlayLocal_Seek(pre, false);
				}
				mIsDrag = false;
				mTvSpeed.setText(" ");
				mSpeed = 0;
				mLlCurrPro.setVisibility(View.GONE);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				mIsDrag = true;
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (mIsDrag) {
					if (mProgressTime > 0) {
						int count = seekBar.getProgress();
						int pre = count / 1000;
						if (playLocal != null) {
							playLocal.PlayLocal_Speed(0, 0);
							// 只传时间,不传百分比了
							playLocal.PlayLocal_Seek(pre, true);
						}
					}
					mLlCurrPro.setVisibility(View.VISIBLE);
					mTvCurrPro.setText(sdf.format(new Date(progress)));
					mTvDuraTime.setText(sdf.format(new Date(seekBar.getMax())));
				} else {
					mLlCurrPro.setVisibility(View.GONE);
				}
			}
		});

	}

	private void initViewAndData() {
		mMonitor = (MyPlaybackGLMonitor) findViewById(R.id.monitor_playback_local);
		mMonitor.setCamera(mMyCamera);

		mMonitor.SetViewType(mMonitor.TOP_ALL_VIEW);/* 设置为鱼眼顶装 */
		mMonitor.SetScreenSize(getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getHeight());
		rbtn_wall_overallview = (RadioButton) findViewById(R.id.rbtn_wall_overallview);
		if (mMyCamera.mInstallMode == 0) {
			mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_CIRCLE, 1);
		} else {// 壁装
			mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_SIDE, 1);
			rbtn_wall_overallview.setChecked(true);
		}

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		screen_width = dm.widthPixels;
		screen_height = dm.heightPixels;
		mTvTotalTime = (TextView) findViewById(R.id.tv_total);
		mTvCurrent = (TextView) findViewById(R.id.tv_current);
		mSeekBar = (SeekBar) findViewById(R.id.sb_playing);
		mIvPausePlay = (ImageView) findViewById(R.id.iv_pause_play);
		mIvFastforward = (ImageView) findViewById(R.id.iv_fastforward);
		mTvSpeed = (TextView) findViewById(R.id.tv_speed);
		mLlCurrPro = (LinearLayout) findViewById(R.id.ll_play_local_pro);
		mTvCurrPro = (TextView) findViewById(R.id.tv_play_loca_current_pro);
		mTvDuraTime = (TextView) findViewById(R.id.tv_play_loca_druation_tim);
		mLlPlay = (LinearLayout) findViewById(R.id.ll_playlocal);
		tv_tit = (TextView) findViewById(R.id.tv_tit);
		tv_tit.setText(title);
		ll_top = (LinearLayout) findViewById(R.id.ll_top);
		play_view_model = (TextView) findViewById(R.id.play_view_model);
		rl_view_model = (RelativeLayout) findViewById(R.id.rl_view_model);
		tv_install = (TextView) findViewById(R.id.tv_install);
		tv_install.setText(mMyCamera.mInstallMode == 0 ? getString(R.string.fish_top):getString(R.string.fish_wall));
		rg_view_model = (RadioGroup) findViewById(R.id.rg_view_model);
		hideAndShowView();
		ll_four_bg = (LinearLayout) findViewById(R.id.ll_four_bg);
		ll_focus_one = (LinearLayout) findViewById(R.id.ll_focus_one);
		ll_focus_one.requestFocus();
		ll_four_bg = (LinearLayout) findViewById(R.id.ll_four_bg);
		ll_focus_two = (LinearLayout) findViewById(R.id.ll_focus_two);
		ll_focus_three = (LinearLayout) findViewById(R.id.ll_focus_three);
		ll_focus_four = (LinearLayout) findViewById(R.id.ll_focus_four);
		btn_return = (ImageView) findViewById(R.id.btn_return);
		rbtn_circle = (RadioButton) findViewById(R.id.rbtn_circle);

		playLocal = new PlayLocal();
	}

	private void hideAndShowView() {
		if (mMyCamera.mInstallMode == 0) {// 顶装
			for (int i = 0; i < rg_view_model.getChildCount(); i++) {
				if (i == 0) {
					rg_view_model.getChildAt(0).setVisibility(View.GONE);
				} else {
					rg_view_model.getChildAt(i).setVisibility(View.VISIBLE);
				}
			}
		} else {
			for (int i = 0; i < rg_view_model.getChildCount(); i++) {
				if (i == 0 || i == 1) {
					rg_view_model.getChildAt(0).setVisibility(View.VISIBLE);
				} else {
					rg_view_model.getChildAt(i).setVisibility(View.GONE);
				}
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (playLocal != null) {
			playLocal.registerPlayLocalStateListener(this);
			// 设置显示窗口
			playLocal.setLiveShowMonitor(mMonitor);
			if (!filePath.isEmpty()) {
				startVideoPath();
			}

		}
	}

	private void startVideoPath() {
		// 1 表示打开视频失败, 0表示打开视频成功
		if (HiTools.isSDCardExist()) {
			int playstate = playLocal.StartPlayLocal(filePath);
			if (playstate != 0) {
				HiToast.showToast(FishEyePlaybackLocalActivity.this, getString(R.string.tips_open_video_fail));
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						FishEyePlaybackLocalActivity.this.finish();
					}
				}, 1000);
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (playLocal != null) {
			playLocal.unregisterPlayLocalStateListener(this);
			playLocal.StopPlayLocal();
		}
		finish();
	}

	private void getIntentData() {
		Bundle bundle = getIntent().getExtras();

		String uid = bundle.getString(HiDataValue.EXTRAS_KEY_UID);
		if (TextUtils.isEmpty(uid))
			finish();
		for (MyCamera camera : HiDataValue.CameraList) {
			if (camera.getUid().equalsIgnoreCase(uid)) {
				mMyCamera = camera;
				break;
			}
		}

		filePath = bundle.getString(VideoLocalActivity.FILE_PATH);
		title = filePath.substring(filePath.lastIndexOf("/") + 1);
	}

	/**
	 * 视频宽，高，文件时长 ，文件播放进度，音频类型，播放状态
	 */
	@Override
	public void callbackplaylocal(int width, int height, int filetime, long cursec, int audiotype, int state) {
		Message msg = Message.obtain();
		if (cursec != 0) {
			if (mFirstTime == 0) {
				mFirstTime = cursec; // 记录第一次播放进度的时间
			}
			long sub = cursec - mFirstTime;// 播放时长
			if (sub > 1000) {
				msg.what = HANDLE_MESSAGE_SEEKBAR_RUN;
				msg.arg1 = (int) sub;
				mHandler.sendMessage(msg);
			}
		}
		switch (state) {
		case PlayLocalFileCallback.PLAYLOCAL_STATE_OPEN:
			msg.what = HANDLE_MESSAGE_SEEKBAR_START;
			msg.arg1 = filetime; // 单位是秒,时间转换时要乘以1000
			mHandler.sendMessage(msg);
			break;
		case PlayLocalFileCallback.PLAY_STATE_END:
			mHandler.sendEmptyMessage(HANDLE_MESSAGE_SEEKBAR_END);
			break;
		case PlayLocalFileCallback.PLAYLOCAL_STATE_ING:
			break;
		case PlayLocalFileCallback.PLAYLOCAL_STATE_ERROR:
			HiToast.showToast(FishEyePlaybackLocalActivity.this, getString(R.string.data_parsing_error));
			break;
		}
	}

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HANDLE_MESSAGE_SEEKBAR_START:
				resetViewMonitor();
				mIsEnd = false;
				mIsPalying = true;
				mIvPausePlay.setSelected(false);
				mTvTotalTime.setText(sdf.format(new Date(msg.arg1 * 1000)));
				mSeekBar.setMax(msg.arg1 * 1000);
				mProgressTime = msg.arg1 * 1000;
				break;
			case HANDLE_MESSAGE_SEEKBAR_RUN:
				if (!mIsDrag) {
					mSeekBar.setProgress(msg.arg1);
				}
				mTvCurrent.setText(sdf.format(new Date(msg.arg1)));
				break;
			case HANDLE_MESSAGE_SEEKBAR_END:
				mIsEnd = true;
				mIsPalying = false;
				mSeekBar.setProgress(mProgressTime);
				mTvCurrent.setText(sdf.format(new Date(mProgressTime)));
				mIvPausePlay.setSelected(true);
				playLocal.StopPlayLocal();// ***
				mTvSpeed.setText(" ");
				mSpeed = 0;
				HiToast.showToast(FishEyePlaybackLocalActivity.this, getString(R.string.tips_stop_video));
				break;
			}
		};
	};

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.pb_local_exit:
			finish();
			break;
		case R.id.iv_pause_play:
			if (mIsEnd) {
				playLocal.setLiveShowMonitor(mMonitor);
				// 1 表示打开视频失败, 0表示打开视频成功
				int playstate = playLocal.StartPlayLocal(filePath);
				if (playstate != 0) {
					HiToast.showToast(FishEyePlaybackLocalActivity.this, getString(R.string.tips_open_video_fail));
					finish();
				} else {
					handSpeed();
				}
			} else {
				if (mIsPalying) {
					playLocal.PlayLocal_pause();
				} else {
					playLocal.PlayLocal_Resume();
				}
				mIvPausePlay.setSelected(mIsPalying);
				mIsPalying = !mIsPalying;
			}
			break;
		case R.id.iv_fastforward:
			handleFast();
			break;
		case R.id.play_view_model:
			rl_view_model.setVisibility(View.VISIBLE);
			ScaleAnimation scaleAnimation = new ScaleAnimation(1.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
			scaleAnimation.setDuration(400);
			rl_view_model.startAnimation(scaleAnimation);
			break;
		case R.id.rl_view_model:
			rl_view_model.setVisibility(View.GONE);
			scaleAnimation = new ScaleAnimation(1.0f, 1.0f, 1.0f, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
			scaleAnimation.setDuration(200);
			rl_view_model.startAnimation(scaleAnimation);
			break;
		case R.id.btn_return:
			FishEyePlaybackLocalActivity.this.finish();
			break;
		}

	}

	protected void resetViewMonitor() {
//		RelativeLayout.LayoutParams lp = null;
//		if (screen_width * 0.667 > screen_height) {
//			lp = new RelativeLayout.LayoutParams((int) (screen_width * 0.9),screen_height);
//		} else {
//			lp = new RelativeLayout.LayoutParams(screen_width, screen_height);
//		}
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(screen_width, screen_height);
		lp.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
		lp.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
		mMonitor.setLayoutParams(lp);

	}

	private void handSpeed() {
		switch (mSpeed) {
		case 2:
			playLocal.PlayLocal_Speed(3, 30);
			break;
		case 4:
			playLocal.PlayLocal_Speed(4, 15);
			break;
		case 8:
			playLocal.PlayLocal_Speed(12, 15);
			break;
		case 16:
			playLocal.PlayLocal_Speed(15, 15);
			break;
		case 0:
			playLocal.PlayLocal_Speed(0, 0);
			break;
		}
	}

	private void handleFast() {
		switch (mSpeed) {
		case 0:
			mSpeed = 2;
			mTvSpeed.setText("X 2");
			playLocal.PlayLocal_Speed(3, 30);
			break;
		case 2:
			mSpeed = 4;
			mTvSpeed.setText("X 4");
			playLocal.PlayLocal_Speed(4, 15);
			break;
		case 4:
			mSpeed = 8;
			mTvSpeed.setText("X 8");
			playLocal.PlayLocal_Speed(12, 15);
			break;
		case 8:
			mSpeed = 16;
			mTvSpeed.setText("X 16");
			playLocal.PlayLocal_Speed(15, 15);
			break;
		case 16:
			mSpeed = 0;
			mTvSpeed.setText(" ");
			playLocal.PlayLocal_Speed(0, 0);
			break;
		}
	}

	private long mFirstTime;
	private final static int HANDLE_MESSAGE_SEEKBAR_START = 0x90000002;
	private final static int HANDLE_MESSAGE_SEEKBAR_RUN = 0x90000003;
	private final static int HANDLE_MESSAGE_SEEKBAR_END = 0x90000004;
	private boolean mIsPalying = false;
	private TextView mTvTotalTime, mTvCurrent;
	private SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
	private boolean mIsDrag = false;
	private SeekBar mSeekBar;
	private int mProgressTime;

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
		if (v.getId() == R.id.monitor_playback_local) {
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
					mMonitor.SetZoom(true);
					mMonitor.SetZoom(true);
					mMonitor.SetZoom(true);
					mMonitor.SetZoom(true);
				} else {
					mMonitor.SetZoom(false);
					mMonitor.SetZoom(false);
					mMonitor.SetZoom(false);
					mMonitor.SetZoom(false);
				}
				xlenOld = xlen;
				ylenOld = ylen;
				nLenStart = nLenEnd;
				return true;
			} else if (nCnt == 1) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					if (mMonitor.mFrameMode == 4) {
						float x = event.getRawX();
						float y = event.getRawY();
						float screen_width = getWindowManager().getDefaultDisplay().getWidth();
						float screen_height = getWindowManager().getDefaultDisplay().getHeight();

						Animation alphaAnimation = new AlphaAnimation(1, 0.1f);
						alphaAnimation.setDuration(666);
						alphaAnimation.setInterpolator(new LinearInterpolator());
						alphaAnimation.setRepeatCount(4);
						alphaAnimation.setRepeatMode(Animation.REVERSE);

						// 区域 1
						boolean area_one = x < screen_width / 2 && y < screen_height / 2;
						// 区域2
						boolean area_two = x > screen_width / 2 && y < screen_height / 2;
						// 区域3
						boolean area_there = x < screen_width / 2 && y > screen_height / 2;
						// 区域4
						boolean area_four = x > screen_width / 2 && y > screen_height / 2;
						if (area_one) {
							ll_focus_one.requestFocus();
							ll_focus_one.startAnimation(alphaAnimation);
						} else if (area_two) {
							ll_focus_two.requestFocus();
							ll_focus_two.startAnimation(alphaAnimation);

						} else if (area_there) {
							ll_focus_three.requestFocus();
							ll_focus_three.startAnimation(alphaAnimation);
						} else if (area_four) {
							ll_focus_four.requestFocus();
							ll_focus_four.startAnimation(alphaAnimation);
						}
					}
					mMonitor.setTouchMove(0);
					break;
				case MotionEvent.ACTION_MOVE:
					if (mMonitor.getTouchMove() != 0)
						break;
					if (Math.abs(move_x - action_down_x) > 40 || Math.abs(move_y - action_down_y) > 40) {
						mMonitor.setTouchMove(1);
					}
					break;
				}
			}
		}
		return true;
	}

	int moveX;
	int moveY;

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		// 1.圆 2.圆柱 3.二画面 4.四画面 5.碗
		switch (checkedId) {
		case R.id.rbtn_wall_overallview: // 壁装全景
			rl_view_model.setVisibility(View.GONE);
			ll_four_bg.setVisibility(View.GONE);
			mMonitor.mWallMode = 1;
			mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_SIDE, 1);
			break;
		case R.id.rbtn_circle:// 圆
			rl_view_model.setVisibility(View.GONE);
			ll_four_bg.setVisibility(View.GONE);
			if (mMyCamera.mInstallMode == 0) {
				mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_CIRCLE, 1);
				mMonitor.setmFrameMode(1);
			} else {// 壁装(圆)
				mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_SIDE, 0);
				mMonitor.mWallMode = 0;
			}
			break;
		case R.id.rbtn_bowl: // 碗
			rl_view_model.setVisibility(View.GONE);
			ll_four_bg.setVisibility(View.GONE);
			mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_CARTOON, 0);// 立体碗
			mMonitor.setmFrameMode(5);
			break;
		case R.id.rbtn_two: // 二画面
			rl_view_model.setVisibility(View.GONE);
			ll_four_bg.setVisibility(View.GONE);
			mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_COLUMN, 2);
			mMonitor.setmFrameMode(3);
			break;
		case R.id.rbtn_four: // 四画面
			rl_view_model.setVisibility(View.GONE);
			mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_CIRCLE, 4);
			ll_four_bg.setVisibility(View.VISIBLE);
			mMonitor.setmFrameMode(4);
			break;
		case R.id.rbtn_cylinder:// 圆柱
			rl_view_model.setVisibility(View.GONE);
			ll_four_bg.setVisibility(View.GONE);
			mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_CARTOON, 1);
			mMonitor.setmFrameMode(2);
			break;
		}
	}

}
