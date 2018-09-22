package com.hichip.activity.FishEye;

import android.app.ActionBar.LayoutParams;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.customview.dialog.NiftyDialogBuilder;
import com.hichip.control.HiGLMonitor;
import com.hichip.pictureviewer.ImagePagerActivity;
import com.hichip1.R;
import com.thecamhi.base.HiToast;
import com.thecamhi.base.HiTools;
import com.thecamhi.base.MyPlaybackGLMonitor;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;
import com.thecamhi.main.HiActivity;

import java.io.File;

public class FishEyePhotoActivity extends HiActivity implements OnTouchListener, OnClickListener, OnCheckedChangeListener {
	private MyPlaybackGLMonitor mMonitor;
	private MyCamera mMyCamera;
	private String pathPhoto;
	public  RelativeLayout ll_top,rl_view_model;
	private short model;// PLAY=1,STOP=2,PAUSE=3,SETPOS=4,GETPOS=5
	private ImageView btn_return,iv_more;
	private TextView tv_tit,play_view_model,tv_install;
	private RadioGroup rg_view_model;
	public  RadioButton rbtn_wall_overallview,rbtn_circle;
	private LinearLayout ll_four_bg;
	private LinearLayout ll_focus_one,ll_focus_two,ll_focus_three,ll_focus_four;
	private int  mListPosition;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_fisheye_photo);
		getIntentData();
		initVeiw();
		setOnListerners();
	}
	@Override
	protected void onResume() {
		super.onResume();
		new Thread() {
			public void run() {
				int result = mMyCamera.ShowPic(pathPhoto);
				if (result == -1) {
					mHandler.obtainMessage(110).sendToTarget();
				}
			};
		}.start();
	}

	private void setOnListerners() {
		mMonitor.setOnTouchListener(this);
		btn_return.setOnClickListener(this);
		play_view_model.setOnClickListener(this);
		rl_view_model.setOnClickListener(this);
		rg_view_model.setOnCheckedChangeListener(this);
		iv_more.setOnClickListener(this);
	}


	private void getIntentData() {
		String uid = getIntent().getStringExtra(HiDataValue.EXTRAS_KEY_UID);
		for (MyCamera camera : HiDataValue.CameraList) {
			if (uid.equalsIgnoreCase(camera.getUid())) {
				this.mMyCamera = camera;
				break;
			}
		}
		pathPhoto = getIntent().getStringExtra("photo_path");
		mListPosition=getIntent().getIntExtra("position", -1);

	}

	private void initVeiw() {
		mMonitor = (MyPlaybackGLMonitor) findViewById(R.id.monitor_photo);
		mMonitor.setCamera(mMyCamera);
		mMonitor.SetViewType(mMonitor.TOP_ALL_VIEW);/* 设置为鱼眼顶装 */
		mMonitor.SetScreenSize(getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getHeight());
		rbtn_wall_overallview=(RadioButton) findViewById(R.id.rbtn_wall_overallview);
		rbtn_circle=(RadioButton) findViewById(R.id.rbtn_circle);
		if (mMyCamera.mInstallMode == 0) {
			mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_CIRCLE, 1);
		} else {// 壁装
			mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_SIDE, 1);
		    rbtn_wall_overallview.setChecked(true);
		}
		mMyCamera.setLiveShowMonitor(mMonitor);
		ll_top = (RelativeLayout) findViewById(R.id.ll_top);
		btn_return = (ImageView) findViewById(R.id.btn_return);
		tv_tit=(TextView) findViewById(R.id.tv_tit);
		String[] strs=pathPhoto.split("/");
		String name=strs[strs.length-1];
		tv_tit.setText(name.substring(4, 8)+"-"+name.substring(8,10)+"-"+name.substring(10, 12)+"  "+name.substring(13,15)+":"+name.substring(15, 17)+":"+
		name.substring(17,19));
		play_view_model=(TextView) findViewById(R.id.play_view_model);
		rl_view_model=(RelativeLayout) findViewById(R.id.rl_view_model);
		tv_install=(TextView) findViewById(R.id.tv_install);
		tv_install.setText(mMyCamera.mInstallMode == 0 ?getString(R.string.fish_top):getString(R.string.fish_wall));
		rg_view_model=(RadioGroup) findViewById(R.id.rg_view_model);
		ll_four_bg=(LinearLayout) findViewById(R.id.ll_four_bg);
		ll_focus_one=(LinearLayout) findViewById(R.id.ll_focus_one_photo);
		ll_focus_one.requestFocus();
		ll_focus_two=(LinearLayout) findViewById(R.id.ll_focus_two_photo);
		ll_focus_three=(LinearLayout) findViewById(R.id.ll_focus_three_photo);
		ll_focus_four=(LinearLayout) findViewById(R.id.ll_focus_four_photo);
		iv_more = (ImageView) findViewById(R.id.iv_more);
		hideAndShowView();
		//resetMonitorSize();
		
		
	}
	private void resetMonitorSize() {
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int screen_width = dm.widthPixels;
		int screen_height = dm.heightPixels;
		RelativeLayout.LayoutParams lp = null;
		if (screen_width * 0.667 > screen_height) {
			lp = new RelativeLayout.LayoutParams((int) (screen_width * 0.9),screen_height);
		} else {
			lp = new RelativeLayout.LayoutParams(screen_width, screen_height);
		}
		// RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(screen_width, screen_height);
		lp.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
		lp.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);

		mMonitor.setLayoutParams(lp);
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

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 110:
				HiToast.showToast(FishEyePhotoActivity.this, "error!");
				FishEyePhotoActivity.this.finish();
				break;

			}

		};
	};

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
		if (v.getId() == R.id.monitor_photo) {
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
					if (model == 0) {
						return false;
					}
					break;
				case MotionEvent.ACTION_MOVE:
					if (mMonitor.getTouchMove() != 0)
						break;
					if (Math.abs(move_x - action_down_x) > 40 || Math.abs(move_y - action_down_y) > 40) {
						mMonitor.setTouchMove(1);
					}
					break;
				case MotionEvent.ACTION_UP:
					break;
				}
			}
		}
		return false;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_return:
			FishEyePhotoActivity.this.finish();
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
		case R.id.iv_more:
			View customView = View.inflate(FishEyePhotoActivity.this, R.layout.pup_photo_dele_share, null);
			PopupWindow pup = new PopupWindow(customView);
			ColorDrawable cd = new ColorDrawable(-0000);
			pup.setBackgroundDrawable(cd);
			pup.setOutsideTouchable(true);
			pup.setFocusable(true);
			pup.setWidth(HiTools.dip2px(this, 90));
			pup.setHeight(LayoutParams.WRAP_CONTENT);
			int disX = HiTools.dip2px(this, 47);
			pup.showAsDropDown(iv_more, -disX, 0);
			LinearLayout ll_share = (LinearLayout) customView.findViewById(R.id.ll_share);
			ll_share.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					File file = new File(pathPhoto);
					if (file.isFile() && file.exists()) {
						Uri imageUri=Uri.fromFile(file);
						Intent shareIntent = new Intent();
						shareIntent.setAction(Intent.ACTION_SEND);
						shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
						shareIntent.setType("image/*");
						startActivity(Intent.createChooser(shareIntent, "分享到"));
					}

				}
			});
			LinearLayout ll_delete = (LinearLayout) customView.findViewById(R.id.ll_delete);
			ll_delete.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showDeleteDialog();
				}
			});
			break;
		}

	}
	
	protected void showDeleteDialog() {
		final NiftyDialogBuilder dialog = NiftyDialogBuilder.getInstance(this);
		dialog.withMessage(getString(R.string.tips_msg_delete_snapshot)).withButton1Text(getString(R.string.btn_no)).withButton2Text(getString(R.string.btn_ok)).setButton1Click(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		}).setButton2Click(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
				File file=new File(pathPhoto);
				if(file.exists()&&file.isFile()){
					file.delete();
					FishEyePhotoActivity.this.finish();
					Intent intent=new Intent();
					intent.setAction(ImagePagerActivity.BROAD_ACTION);
					intent.putExtra(ImagePagerActivity.INDEX, mListPosition);
					sendBroadcast(intent);
				}
			}
		}).show();
	}

	
	
	

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		// 1.圆 2.圆柱 3.二画面 4.四画面 5.碗
		switch (checkedId) {
		case R.id.rbtn_wall_overallview: // 壁装全景
			rl_view_model.setVisibility(View.GONE);
			ll_four_bg.setVisibility(View.GONE);
			mMonitor.mWallMode=1;
			mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_SIDE, 1);
			break;
		case R.id.rbtn_circle:// 圆
			rl_view_model.setVisibility(View.GONE);
			ll_four_bg.setVisibility(View.GONE);
			if (mMyCamera.mInstallMode == 0) {
				mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_CIRCLE, 1);
				mMonitor.setmFrameMode(1);
			} else {//壁装(圆)
				mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_SIDE, 0);
				mMonitor.mWallMode=0;
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
