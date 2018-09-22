package com.thecamhi.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;

import com.hichip.activity.FishEye.FishEyeActivity;
import com.hichip.base.HiLog;
import com.hichip.content.HiChipDefines;
import com.hichip.control.HiGLMonitor;
import com.hichip.data.HiDeviceInfo;
import com.hichip.sdk.HiChipP2P;
import com.thecamhi.bean.MyCamera;

import java.util.Timer;
import java.util.TimerTask;

public class MyLiveViewGLMonitor extends HiGLMonitor implements OnTouchListener, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
	private GestureDetector mGestureDetector;
	private static final int FLING_MIN_DISTANCE = 50;
	public static int PTZ_STEP = 50; // 云台步长
	private MyCamera mCamera = null;
	Matrix matrix = new Matrix();
	private OnTouchListener mOnTouchListener;
	private Activity context;
	private int state = 0; // normal=0, larger=1,two finger touch=3
	private int touchMoved; // not move=0, move=1, two point=2
	public int left;
	public int width;
	public int height;
	public int bottom;
	public int screen_width;
	public int screen_height;
	// public int mOritation; // 1竖屏 0横屏
	public static int centerPoint;
	private float pxTopview;

	public MyLiveViewGLMonitor(Context context, AttributeSet attrs) {
		super(context, attrs);
		mGestureDetector = new GestureDetector(context, this);
		super.setOnTouchListener(this);
		setOnTouchListener(this);
		setFocusable(true);
		setClickable(true);
		setLongClickable(true);
		this.context = (Activity) context;

		DisplayMetrics dm = new DisplayMetrics();
		((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(dm);
		screen_width = dm.widthPixels;
		screen_height = dm.heightPixels;
		pxTopview = HiTools.dip2px(getContext(), 45) + getStatusBarHeight();
	}

	public int getStatusBarHeight() {
		int result = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	public int getTouchMove() {
		return this.touchMoved;
	}

	public void setTouchMove(int touchMoved) {
		this.touchMoved = touchMoved;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	// View当前的位置
	private float rawX = 0;
	private float rawY = 0;
	// View之前的位置
	private float lastX = 0;
	private float lastY = 0;

	int xlenOld;
	int ylenOld;

	private int pyl = 20;
	double nLenStart = 0;

	@SuppressLint("WrongCall")
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (mOnTouchListener != null) {
			mOnTouchListener.onTouch(v, event);// 必须要回调非当前的OnTouch方法,不然会栈溢出崩溃
		}
		int nCnt = event.getPointerCount();
		if (state == 1) {// 放大就是1
			if (nCnt == 2) {
				return false;
			}
			// 处理放大后,移动界面(类似移动云台,只不过云台没有动)
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				// 获取手指落下的坐标并保存
				rawX = (event.getRawX());
				rawY = (event.getRawY());
				lastX = rawX;
				lastY = rawY;
				break;
			case MotionEvent.ACTION_MOVE:
				if (touchMoved == 2) {
					break;
				}
				HiLog.e("mMonitor.ACTION_MOVE");
				// 手指拖动时，获得当前位置
				rawX = event.getRawX();
				rawY = event.getRawY();
				// 手指移动的x轴和y轴偏移量分别为当前坐标-上次坐标
				float offsetX = rawX - lastX;
				float offsetY = rawY - lastY;
				// 通过View.layout来设置左上右下坐标位置
				// 获得当前的left等坐标并加上相应偏移量
				if (Math.abs(offsetX) < pyl && Math.abs(offsetY) < pyl) {
					return false;
				}
				left += offsetX;
				bottom -= offsetY;
				if (left > 0) {
					left = 0;
				}
				if (bottom > 0) {
					bottom = 0;
				}
				if ((left + width < (screen_width))) {
					left = (int) (screen_width - width);
				}
				if (bottom + height < screen_height) {
					bottom = (int) (screen_height - height);
				}
				if (left <= (-width)) {
					left = (-width);
				}
				if (bottom <= (-height)) {
					bottom = (-height);
				}
				setMatrix(left, bottom, width, height);
				// 移动过后，更新lastX与lastY
				lastX = rawX;
				lastY = rawY;
				break;
			}
			return true;
		} else if (state == 0 && nCnt == 1) {
			return mGestureDetector.onTouchEvent(event);
		}
		return true;
	}

	public void saveMatrix(int left, int bottom, int width, int height) {
		this.left = left;
		this.bottom = bottom;
		this.width = width;
		this.height = height;
	}

	float resetWidth;
	float resetHeight;
	private int centerPointX;
	private int centerPointY;

	public void setView() {
		WindowManager.LayoutParams wlp = context.getWindow().getAttributes();
		wlp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;

		context.getWindow().setAttributes(wlp);
		context.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

		DisplayMetrics dm = new DisplayMetrics();
		context.getWindowManager().getDefaultDisplay().getMetrics(dm);
		float screen_width = dm.widthPixels;
		float screen_height = dm.heightPixels;

		if (resetWidth == 0) {
			resetWidth = screen_width;
			resetHeight = screen_height;
		}
		resetWidth += 100;
		resetHeight += 100;
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	private float down_x = 0;
	private float down_y = 0;
	private float move_X = 0;
	private float move_Y = 0;
	private int[] flags = { 0, 0, 0 };

	// distanceX 为e1-e2的偏移量 e1为起始点的坐标 e2为移动变化点的坐标
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		if (mCamera.isFishEye()) {
			if (FishEyeActivity.mFrameMode == 4&&mCamera.mInstallMode==0) {
				if (Math.abs(distanceX) < 5) {
					return true;
				}
				handlerFrameMode_4(distanceX, e1.getRawX(), e1.getRawY());
			} else if (FishEyeActivity.mFrameMode == 3&&mCamera.mInstallMode==0) {// 二画面
				handerFrameMode_3(e1, e2, distanceX);
			} else if (FishEyeActivity.mFrameMode == 1 || FishEyeActivity.mFrameMode == 2 || FishEyeActivity.mFrameMode == 5||mCamera.mInstallMode==0||mCamera.mInstallMode==1) {
				down_x = e1.getRawX();
				down_y = e1.getRawY();
				move_X = e2.getRawX();
				move_Y = e2.getRawY();
				float disX = move_X - down_x;
				float disY = move_Y - down_y;
				float K = disY / disX;

				boolean area_one = false;
				boolean area_two = false;
				boolean area_there = false;
				boolean area_four = false;
				float x = e1.getRawX();
				float y = e1.getRawY();
				float pxMoniter = screen_width;
				// 区域 1
				area_one = x < screen_width / 2 && y > pxTopview && y < pxTopview + pxMoniter / 2;
				// 区域2
				area_two = x > screen_width / 2 && x < screen_width && y < pxTopview + pxMoniter / 2 && y > pxTopview;
				// 区域3
				area_there = x < screen_width / 2 && y > pxTopview + pxMoniter / 2 && y < pxMoniter + pxTopview;
				// 区域4
				area_four = x > screen_width / 2 && y > pxTopview + pxMoniter / 2 && y < pxMoniter + pxTopview;

				int numX = (int) Math.ceil(Math.abs(distanceX) / 10) + 1;
				// Log.i("tedu", "--distanceX-->"+distanceX+":::"+"--distanceY-->"+distanceY);
				if (distanceX < -2) {// 右滑
					if (K < 4 && K > 0.4) {// 1.右下滑
						int index = 0;
						for (int i = 0; i < flags.length; i++) {
							if (flags[i] != 1) {
								flags[i] = 1;
								break;
							} else {
								index++;
							}
						}
						if (index == 3) {
							Log.i("tedu", "---回调了: onScroll---右下滑--->" + numX + "-K->" + K);
							if (this.GetFishLager() == 0.0) {
								if (area_one || area_there) {
									setGesture(HiGLMonitor.GESTURE_LEFT, numX);
								}
								if (area_two || area_four) {
									setGesture(HiGLMonitor.GESTURE_RIGHT, numX);
								}
							} else {
								this.SetGesture(7);
								this.SetGesture(7);
								this.SetGesture(7);
							}
						}
						down_x = move_X;
						down_y = move_Y;
						return true;
					} else if (K > -4 && K < -0.4) {// 2.右上滑
						int index = 0;
						for (int i = 0; i < flags.length; i++) {
							if (flags[i] != 2) {
								flags[i] = 2;
								break;
							} else {
								index++;
							}
						}
						if (index == 3) {
							Log.i("tedu", "------回调了: onScroll---右上滑---->" + numX + "-K->" + K);
							if (this.GetFishLager() == 0.0) {
								if (area_one || area_there) {
									setGesture(HiGLMonitor.GESTURE_RIGHT, numX);
								} else {
									setGesture(HiGLMonitor.GESTURE_LEFT, numX);
								}
							} else {
								this.SetGesture(6);
								this.SetGesture(6);
								this.SetGesture(6);
							}
						}
						down_x = move_X;
						down_y = move_Y;
						return true;
					} else if (K > 3 || K < -3) {
						Log.i("tedu", "--K > 3 || K < --->" + K);
					} else {
						int index = 0;
						for (int i = 0; i < flags.length; i++) {
							if (flags[i] != 3) {
								flags[i] = 3;
								break;
							} else {
								index++;
							}
						}
						if (index == 3) {
							Log.i("tedu", "------回调了: onScroll---右滑---->" + numX);
							if (e1.getRawY() > pxTopview && e1.getRawY() < pxTopview + screen_width / 2&&mCamera.mInstallMode==0) {// Y轴的上半轴
								if (this.GetFishLager() >= 0.0 && this.GetFishLager() < 8.0) {
									setGesture(HiGLMonitor.GESTURE_RIGHT, numX);
								} else {
									setGesture(HiGLMonitor.GESTURE_RIGHT, numX / 2);
								}
							} else if (e1.getRawY() > pxTopview + screen_width / 2 && e1.getRawY() < pxTopview + screen_width&&mCamera.mInstallMode==0) {
								if (this.GetFishLager() >= 0.0 && this.GetFishLager() < 8.0) {
									setGesture(HiGLMonitor.GESTURE_LEFT, numX);
								} else {
									setGesture(HiGLMonitor.GESTURE_LEFT, numX / 2);
								}
							}else {
								setGesture(HiGLMonitor.GESTURE_RIGHT, numX / 2);
							}
						}
						down_x = move_X;
						down_y = move_Y;
						return true;
					}
				}
				if (distanceX > 2) {// 左滑
					if (K < 4 && K > 1) {// 3.左上滑
						int index = 0;
						for (int i = 0; i < flags.length; i++) {
							if (flags[i] != 4) {
								flags[i] = 4;
								break;
							} else {
								index++;
							}
						}
						if (index == 3) {
							Log.i("tedu", "------回调了: onScroll---左上滑---->" + numX + "-K->" + K);
							if (this.GetFishLager() == 0.0) {
								if (area_one || area_there) {
									setGesture(HiGLMonitor.GESTURE_RIGHT, numX);
								}
								if (area_two || area_four) {
									setGesture(HiGLMonitor.GESTURE_LEFT, numX);
								}
							} else {
								this.SetGesture(4);
								this.SetGesture(4);
								this.SetGesture(4);
							}
						}
						down_x = move_X;
						down_y = move_Y;
						return true;
					} else if (K > -4 && K < -1) {// 4.左下滑
						int index = 0;
						for (int i = 0; i < flags.length; i++) {
							if (flags[i] != 5) {
								flags[i] = 5;
								break;
							} else {
								index++;
							}
						}
						if (index == 3) {
							Log.i("tedu", "------回调了: onScroll---左下滑---->" + numX + "-K->" + K);
							if (this.GetFishLager() == 0.0) {
								if (area_one || area_there) {
									setGesture(HiGLMonitor.GESTURE_LEFT, numX);
								}
								if (area_two || area_four) {
									setGesture(HiGLMonitor.GESTURE_RIGHT, numX);
								}
							} else {
								this.SetGesture(5);
								this.SetGesture(5);
								this.SetGesture(5);
							}
						}
						down_x = move_X;
						down_y = move_Y;
						return true;
					} else if (K > 3 || K < -3) {
					} else {
						int index = 0;
						for (int i = 0; i < flags.length; i++) {
							if (flags[i] != 6) {
								flags[i] = 6;
								break;
							} else {
								index++;
							}
						}
						if (index == 3) {
							Log.i("tedu", "------回调了: onScroll---左滑---->" + numX + "-K->" + K);
							if (e1.getRawY() > pxTopview && e1.getRawY() < pxTopview + screen_width / 2&&mCamera.mInstallMode==0) {// Y轴的上半轴
								if (this.GetFishLager() >= 0.0 && this.GetFishLager() < 8.0) {
									setGesture(HiGLMonitor.GESTURE_LEFT, numX);
								} else {
									setGesture(HiGLMonitor.GESTURE_LEFT, numX / 2);
								}
							} else if (e1.getRawY() > pxTopview + screen_width / 2 && e1.getRawY() < pxTopview + screen_width&&mCamera.mInstallMode==0) {
								if (this.GetFishLager() >= 0.0 && this.GetFishLager() < 8.0) {
									setGesture(HiGLMonitor.GESTURE_RIGHT, numX);
								} else {
									setGesture(HiGLMonitor.GESTURE_RIGHT, numX / 2);
								}
							}else {
								setGesture(HiGLMonitor.GESTURE_LEFT, numX / 2);
							}
						}

						down_x = move_X;
						down_y = move_Y;
						return true;
					}
				}
				int num = (int) Math.ceil(Math.abs(distanceY) / 40) + 1;
				if (distanceY < 0 && Math.abs(K) > 1.0) {// 5.下滑
					int index = 0;
					for (int i = 0; i < flags.length - 1; i++) {
						if (flags[i] != 7) {
							flags[i] = 7;
							break;
						} else {
							index++;
						}
					}
					if (index == 2) {
						Log.i("tedu", "---11---回调了: onScroll---下滑---->" + num + "--K-->" + K);
						if (this.GetFishLager() != 0.0) {
							Log.i("tedu", "--down-->" + this.GetFishLager());
							if (this.GetFishLager() < 8.0 && this.GetFishLager() >= 0) {
								setGesture(HiGLMonitor.GESTURE_DOWN, num);
							} else {
								setGesture(HiGLMonitor.GESTURE_DOWN, num - 1);
							}
						}
						if (this.GetFishLager() == 0.0) {
							if (area_one || area_there) {
								setGesture(HiGLMonitor.GESTURE_LEFT, num);
							}
							if (area_two || area_four) {
								setGesture(HiGLMonitor.GESTURE_RIGHT, num);
							}
						}
					}
					down_x = move_X;
					down_y = move_Y;
					return true;
				}
				if (distanceY > 0 && Math.abs(K) > 1.0) {// 6.上滑
					int index = 0;
					for (int i = 0; i < flags.length - 1; i++) {
						if (flags[i] != 7) {
							flags[i] = 7;
							break;
						} else {
							index++;
						}
					}
					if (index == 2) {
						Log.i("tedu", "--11----回调了: onScroll---上滑---->" + num + "--K-->" + K);
						if (this.GetFishLager() != 0.0) {
							if (this.GetFishLager() < 8.0 && this.GetFishLager() >= 0.0) {
								setGesture(HiGLMonitor.GESTURE_UP, num);
							} else {
								setGesture(HiGLMonitor.GESTURE_UP, num - 1);
							}
						}
						if (this.GetFishLager() == 0.0) {
							if (area_one || area_there) {
								setGesture(HiGLMonitor.GESTURE_RIGHT, num);
							}
							if (area_two || area_four) {
								setGesture(HiGLMonitor.GESTURE_LEFT, num);
							}
						}
					}
					down_x = move_X;
					down_y = move_Y;
					return true;
				}
			}
		}
		down_x = move_X;
		down_y = move_Y;
		return true;
	}

	private void setGesture(int gesture, int num) {
		for (int i = 0; i < num; i++) {
			this.SetGesture(gesture);
			this.SetGesture(gesture);
		}
	}

	private void handerFrameMode_3(MotionEvent e1, MotionEvent e2, float distanceX) {
		boolean area_top = false;
		boolean area_bottom = false;
		float y = e1.getRawY();
		if (FishEyeActivity.misFullScreen == 1) { // 竖
			area_top = y > pxTopview && y < pxTopview + screen_width / 2;
			area_bottom = y > pxTopview + screen_width / 2 && y < pxTopview + screen_width;
		} else {// 横屏
			area_top = y < screen_width / 2;
			area_bottom = y > screen_width / 2 && y < screen_width;
		}

		move_X = e2.getRawX();
		move_Y = e2.getRawY();
		float disX = move_X - down_x;
		float disY = move_Y - down_y;
		float K = disY / disX;
		if (area_top) {
			if (distanceX < 0 && Math.abs(K) < 1) {// 右滑
				this.SetGesture(HiGLMonitor.GESTURE_RIGHT, 0);
				this.SetGesture(HiGLMonitor.GESTURE_RIGHT, 0);
			} else if (distanceX > 0 && Math.abs(K) < 1) {// 左滑
				this.SetGesture(HiGLMonitor.GESTURE_LEFT, 0);
				this.SetGesture(HiGLMonitor.GESTURE_LEFT, 0);
			}
		} else if (area_bottom) {
			if (distanceX < 0 && Math.abs(K) < 1) {// 右滑
				this.SetGesture(HiGLMonitor.GESTURE_RIGHT, 1);
				this.SetGesture(HiGLMonitor.GESTURE_RIGHT, 1);
			} else if (distanceX > 0 && Math.abs(K) < 1) {// 左滑
				this.SetGesture(HiGLMonitor.GESTURE_LEFT, 1);
				this.SetGesture(HiGLMonitor.GESTURE_LEFT, 1);
			}
		}
	}

	private void handlerFrameMode_4(float distanceX, float x, float y) {
		float pxMoniter = screen_width;
		boolean area_one = false;
		boolean area_two = false;
		boolean area_there = false;
		boolean area_four = false;
		if (FishEyeActivity.misFullScreen == 1) { // 竖屏
			// 区域 1
			area_one = x < screen_width / 2 && y > pxTopview && y < pxTopview + pxMoniter / 2;
			// 区域2
			area_two = x > screen_width / 2 && x < screen_width && y < pxTopview + pxMoniter / 2 && y > pxTopview;
			// 区域3
			area_there = x < screen_width / 2 && y > pxTopview + pxMoniter / 2 && y < pxMoniter + pxTopview;
			// 区域4
			area_four = x > screen_width / 2 && y > pxTopview + pxMoniter / 2 && y < pxMoniter + pxTopview;
		} else { // 横屏
			// 区域 1
			area_one = x < screen_height / 2 && y < screen_width / 2;
			// 区域2
			area_two = x > screen_height / 2 && y < screen_width / 2;
			// 区域3
			area_there = x < screen_height / 2 && y > screen_width / 2;
			// 区域4
			area_four = x > screen_height / 2 && y > screen_width / 2;
		}
		int num = 1;
		if (area_one) {
			for (int i = 0; i <= num; i++) {
				if (distanceX > 0) {
					this.SetGesture(HiGLMonitor.GESTURE_LEFT, 2);
				} else {
					this.SetGesture(HiGLMonitor.GESTURE_RIGHT, 2);
				}
			}
		} else if (area_two) {
			for (int i = 0; i <= num; i++) {
				if (distanceX > 0) {
					this.SetGesture(HiGLMonitor.GESTURE_LEFT, 0);
				} else {
					this.SetGesture(HiGLMonitor.GESTURE_RIGHT, 0);
				}
			}
		} else if (area_there) {
			for (int i = 0; i <= num; i++) {
				if (distanceX > 0) {
					this.SetGesture(HiGLMonitor.GESTURE_LEFT, 3);
				} else {
					this.SetGesture(HiGLMonitor.GESTURE_RIGHT, 3);
				}
			}
		} else if (area_four) {
			for (int i = 0; i <= num; i++) {
				if (distanceX > 0) {
					this.SetGesture(HiGLMonitor.GESTURE_LEFT, 1);
				} else {
					this.SetGesture(HiGLMonitor.GESTURE_RIGHT, 1);
				}
			}
		}
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}

	// velocityX——每秒x轴方向移动的像素 速率; velocityY——每秒y轴方向移动的像素 速率;
	// e1 起点移动时间
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if (mCamera == null)
			return false;
		PTZ_STEP = mCamera.getChipVersion() == HiDeviceInfo.CHIP_VERSION_GOKE ? 25 : 50;
		if (state == 0) {
			if (!mCamera.isFishEye()) {
				if (e1.getX() - e2.getX() > FLING_MIN_DISTANCE && Math.abs(velocityX) > Math.abs(velocityY)) {
					mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_PTZ_CTRL, HiChipDefines.HI_P2P_S_PTZ_CTRL.parseContent(HiChipP2P.HI_P2P_SE_CMD_CHN, HiChipDefines.HI_P2P_PTZ_CTRL_LEFT, HiChipDefines.HI_P2P_PTZ_MODE_STEP, (short) PTZ_STEP, (short) PTZ_STEP));
				} else if (e2.getX() - e1.getX() > FLING_MIN_DISTANCE && Math.abs(velocityX) > Math.abs(velocityY)) {
					mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_PTZ_CTRL, HiChipDefines.HI_P2P_S_PTZ_CTRL.parseContent(HiChipP2P.HI_P2P_SE_CMD_CHN, HiChipDefines.HI_P2P_PTZ_CTRL_RIGHT, HiChipDefines.HI_P2P_PTZ_MODE_STEP, (short) PTZ_STEP, (short) PTZ_STEP));
				} else if (e1.getY() - e2.getY() > FLING_MIN_DISTANCE && Math.abs(velocityY) > Math.abs(velocityX)) {
					mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_PTZ_CTRL, HiChipDefines.HI_P2P_S_PTZ_CTRL.parseContent(HiChipP2P.HI_P2P_SE_CMD_CHN, HiChipDefines.HI_P2P_PTZ_CTRL_UP, HiChipDefines.HI_P2P_PTZ_MODE_STEP, (short) PTZ_STEP, (short) PTZ_STEP));
				} else if (e2.getY() - e1.getY() > FLING_MIN_DISTANCE && Math.abs(velocityY) > Math.abs(velocityX)) {
					mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_PTZ_CTRL, HiChipDefines.HI_P2P_S_PTZ_CTRL.parseContent(HiChipP2P.HI_P2P_SE_CMD_CHN, HiChipDefines.HI_P2P_PTZ_CTRL_DOWN, HiChipDefines.HI_P2P_PTZ_MODE_STEP, (short) PTZ_STEP, (short) PTZ_STEP));
				}
			} else {
				if (getTouchMove() == 2) {
					return false;
				}
				if (FishEyeActivity.mFrameMode == 4 && Math.abs(velocityX) >= 1000&&mCamera.mInstallMode == 0) {
					float distanceX = e1.getRawX() - e2.getRawX();
					handlerFrameMode_4_Rotate(distanceX, e1.getRawX(), e1.getRawY());
					return true;
				}
				
				if (FishEyeActivity.mFrameMode == 3||mCamera.mInstallMode == 0) {
					boolean area_top = false;
					boolean area_bottom = false;
					float y = e1.getRawY();
					if (FishEyeActivity.misFullScreen == 1) { // 竖
						area_top = y > pxTopview && y < pxTopview + screen_width / 2;
						area_bottom = y > pxTopview + screen_width / 2 && y < pxTopview + screen_width;
					} else {// 横屏
						area_top = y < screen_width / 2;
						area_bottom = y > screen_width / 2 && y < screen_width;
					}
					float disX = e2.getRawX() - e1.getRawX();
					if (area_top) {
						Log.i("tedu", "--area_top-->"+area_top);
						if (disX > 0 && Math.abs(velocityX) >= 1600) {// 右滑
							startRotateMode4(HiGLMonitor.GESTURE_RIGHT, 100, 0);
						} else if (disX < 0 && Math.abs(velocityX) >= 1600) {// 左滑
							startRotateMode4(HiGLMonitor.GESTURE_LEFT, 100, 0);
						}
					} else if (area_bottom) {
						Log.i("tedu", "--area_bottom-->"+area_bottom);
						if (disX > 0 && Math.abs(velocityX) >= 1600) {// 右滑
							startRotateMode4(HiGLMonitor.GESTURE_RIGHT, 100, 1);
						} else if (disX < 0 && Math.abs(velocityX) >= 1600) {// 左滑
							startRotateMode4(HiGLMonitor.GESTURE_LEFT, 100, 1);
						}
					}
				}
				if ((FishEyeActivity.mFrameMode == 1) || FishEyeActivity.mFrameMode == 2 || FishEyeActivity.mFrameMode == 5||mCamera.mInstallMode == 0||mCamera.mInstallMode == 1) {
					float m_x = e2.getRawX();
					float m_Y = e2.getRawY();
					float d_x = e1.getRawX();
					float d_y = e1.getRawY();
					float disX = m_x - d_x;
					float disY = m_Y - d_y;
					float distanceX = e1.getRawX() - e2.getRawX();
					float distanceY = e1.getRawY() - e2.getRawY();
					float K = disY / disX;
					int numX = (int) (Math.abs(disX) / 3) / 2;

					boolean area_one = false;
					boolean area_two = false;
					boolean area_there = false;
					boolean area_four = false;
					float x = e1.getRawX();
					float y = e1.getRawY();
					float pxMoniter = screen_width;

					// 区域 1
					area_one = x < screen_width / 2 && y > pxTopview && y < pxTopview + pxMoniter / 2;
					// 区域2
					area_two = x > screen_width / 2 && x < screen_width && y < pxTopview + pxMoniter / 2 && y > pxTopview;
					// 区域3
					area_there = x < screen_width / 2 && y > pxTopview + pxMoniter / 2 && y < pxMoniter + pxTopview;
					// 区域4
					area_four = x > screen_width / 2 && y > pxTopview + pxMoniter / 2 && y < pxMoniter + pxTopview;

					if (distanceX < 0 && velocityX > 1000) {// 右滑
						if (K < 5 && K > 0.5) {// 1.右下滑
							Log.i("tedu", "------回调了: onFling----右下滑--->");
							if (this.GetFishLager() == 0.0 || FishEyeActivity.mFrameMode == 2 || FishEyeActivity.mFrameMode == 5) {
								if (area_one || area_there) {
									startRotate(HiGLMonitor.GESTURE_LEFT, numX);
								}
								if (area_two || area_four) {
									startRotate(HiGLMonitor.GESTURE_RIGHT, numX);
								}
							} else {
								startRotate(7, numX);
							}
							return true;
						} else if (K > -5 && K < -0.5) {// 2.右上滑
							Log.i("tedu", "------回调了: onFling----右上滑--->" + numX + "--Lager-->" + this.GetFishLager());
							if (this.GetFishLager() == 0.0 || FishEyeActivity.mFrameMode == 2 || FishEyeActivity.mFrameMode == 5) {
								if (area_one || area_there) {
									startRotate(HiGLMonitor.GESTURE_RIGHT, numX);
								}
								if (area_two || area_four) {
									startRotate(HiGLMonitor.GESTURE_LEFT, numX);
								}
							} else {
								startRotate(6, numX);
							}
							return true;
						} else if (K > 3 || K < -3) {
						} else {
							Log.i("tedu", "------回调了: onFling----右滑--->" + velocityX);
							if (e1.getRawY() > pxTopview && e1.getRawY() < pxTopview + screen_width / 2&&mCamera.mInstallMode==0) {// Y轴的上半轴
								if (this.GetFishLager() >= 8.0) {
									startRotate(HiGLMonitor.GESTURE_RIGHT, numX / 2);
								} else {
									startRotate(HiGLMonitor.GESTURE_RIGHT, numX);
								}
							} else if (e1.getRawY() > pxTopview + screen_width / 2 && e1.getRawY() < pxTopview + screen_width&&mCamera.mInstallMode==0) {
								if (this.GetFishLager() >= 8.0) {
									startRotate(HiGLMonitor.GESTURE_LEFT, numX / 2);
								} else {
									startRotate(HiGLMonitor.GESTURE_LEFT, numX);
								}
							}else {
								startRotate(HiGLMonitor.GESTURE_RIGHT, numX / 2);
							}
							return true;
						}
					}
					if (distanceX > 0 && velocityX < -1000) {// 左滑
						if (K < 5 && K > 0.5) {// 3.左上滑
							Log.i("tedu", "------回调了: onFling----左上滑--->");
							if (this.GetFishLager() == 0.0 || FishEyeActivity.mFrameMode == 2 || FishEyeActivity.mFrameMode == 5) {
								if (area_two || area_four || area_one) {
									startRotate(HiGLMonitor.GESTURE_LEFT, numX);
								} else {
									startRotate(HiGLMonitor.GESTURE_RIGHT, numX);
								}
							} else {
								startRotate(4, numX);
							}
							return true;
						} else if (K > -5 && K < -0.5) {// 4.左下滑
							Log.i("tedu", "------回调了: onFling----左下滑--->");
							if (this.GetFishLager() == 0.0 || FishEyeActivity.mFrameMode == 2 || FishEyeActivity.mFrameMode == 5) {
								if (area_four) {
									startRotate(HiGLMonitor.GESTURE_RIGHT, numX);
								} else {
									startRotate(HiGLMonitor.GESTURE_LEFT, numX);
								}
							} else {
								startRotate(5, numX);
							}
							return true;
						} else if (K > 3 || K < -3) {
						} else {
							Log.i("tedu", "------回调了: onFling----左滑--->" + velocityX);
							if (e1.getRawY() > pxTopview && e1.getRawY() < pxTopview + screen_width / 2&&mCamera.mInstallMode==0) {// Y轴的上半轴
								if (this.GetFishLager() >= 8.0) {
									startRotate(HiGLMonitor.GESTURE_LEFT, numX / 2);
								} else {
									startRotate(HiGLMonitor.GESTURE_LEFT, numX);
								}
							} else if (e1.getRawY() > pxTopview + screen_width / 2 && e1.getRawY() < pxTopview + screen_width&&mCamera.mInstallMode==0) {
								if (this.GetFishLager() >= 8.0) {
									startRotate(HiGLMonitor.GESTURE_RIGHT, numX / 2);
								} else {
									startRotate(HiGLMonitor.GESTURE_RIGHT, numX);
								}
							}else {
								startRotate(HiGLMonitor.GESTURE_LEFT, numX);
							}
							return true;
						}
					}
					if (distanceY < 0 && velocityY > 5000) { // 向下滑
						Log.i("tedu", "------回调了: onFling----下滑--->" + velocityY);
						if (this.GetFishLager() == 0.0 || FishEyeActivity.mFrameMode == 2 || FishEyeActivity.mFrameMode == 5) {
							if (area_one || area_there) {
								startRotate(HiGLMonitor.GESTURE_LEFT, 60);
							}
							if (area_two || area_four) {
								startRotate(HiGLMonitor.GESTURE_RIGHT, 60);
							}
						} else {
							startRotate(HiGLMonitor.GESTURE_DOWN, 40);
						}
						return true;
					} else if (distanceY > 0 && velocityY < -5000) {// 向上滑
						Log.i("tedu", "------回调了: onFling----上滑--->" + velocityY);
						if (this.GetFishLager() == 0.0 || FishEyeActivity.mFrameMode == 2 || FishEyeActivity.mFrameMode == 5) {
							if (area_one || area_there) {
								startRotate(HiGLMonitor.GESTURE_RIGHT, 60);
							}
							if (area_two || area_four) {
								startRotate(HiGLMonitor.GESTURE_LEFT, 60);
							}
						} else {
							startRotate(HiGLMonitor.GESTURE_UP, 40);
						}
						return true;
					}
				}
			}

		}
		return true;
	}

	private void startRotateMode4(final int gesture, final int num, final int no) {
		new Thread() {
			public void run() {
				for (int i = 0; i <= num;) {
					if (i < num / 5)
						i++;
					else if (i < num * 2 / 5)
						i += 5;
					else if (i < num * 3 / 5)
						i += 3;
					else if (i < num * 4 / 5)
						i += 1;
					else
						i++;

					try {
						Thread.sleep(20);
						int b = (num * 2 / 3 - i);
						if (b < 4)
							b = 4;
						int j = b / 4;
						for (int a = j; a >= 0; a--) {
							MyLiveViewGLMonitor.this.SetGesture(gesture, no);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
		}.start();
	}

	private void handlerFrameMode_4_Rotate(float distanceX, float x, float y) {
		float pxMoniter = screen_width;
		boolean area_one = false;
		boolean area_two = false;
		boolean area_there = false;
		boolean area_four = false;
		if (FishEyeActivity.misFullScreen == 1) { // 竖屏
			// 区域 1
			area_one = x < screen_width / 2 && y > pxTopview && y < pxTopview + pxMoniter / 2;
			// 区域2
			area_two = x > screen_width / 2 && x < screen_width && y < pxTopview + pxMoniter / 2 && y > pxTopview;
			// 区域3
			area_there = x < screen_width / 2 && y > pxTopview + pxMoniter / 2 && y < pxMoniter + pxTopview;
			// 区域4
			area_four = x > screen_width / 2 && y > pxTopview + pxMoniter / 2 && y < pxMoniter + pxTopview;
		} else { // 横屏
			// 区域 1
			area_one = x < screen_height / 2 && y < screen_width / 2;
			// 区域2
			area_two = x > screen_height / 2 && y < screen_width / 2;
			// 区域3
			area_there = x < screen_height / 2 && y > screen_width / 2;
			// 区域4
			area_four = x > screen_height / 2 && y > screen_width / 2;
		}
		int num = (int) Math.abs(distanceX) / 5;
		if (area_one) {
			if (distanceX > 0) {
				startRotateMode4(HiGLMonitor.GESTURE_LEFT, num, 2);
			} else {
				startRotateMode4(HiGLMonitor.GESTURE_RIGHT, num, 2);
			}
		} else if (area_two) {
			if (distanceX > 0) {
				startRotateMode4(HiGLMonitor.GESTURE_LEFT, num, 0);
			} else {
				startRotateMode4(HiGLMonitor.GESTURE_RIGHT, num, 0);
			}
		} else if (area_there) {
			if (distanceX > 0) {
				startRotateMode4(HiGLMonitor.GESTURE_LEFT, num, 3);
			} else {
				startRotateMode4(HiGLMonitor.GESTURE_RIGHT, num, 3);
			}
		} else if (area_four) {
			if (distanceX > 0) {
				startRotateMode4(HiGLMonitor.GESTURE_LEFT, num, 1);
			} else {
				startRotateMode4(HiGLMonitor.GESTURE_RIGHT, num, 1);
			}
		}

	}

	public void setCamera(MyCamera mCamera) {
		this.mCamera = mCamera;
	}

	public void setOnTouchListener(OnTouchListener mOnTouchListener) {
		this.mOnTouchListener = mOnTouchListener;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return false;
	}

	public int mSetPosition = 0;

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		if (mCamera.isFishEye()) {
			float x = e.getRawX();
			float y = e.getRawY();
			int pxMoniter = screen_width;
			centerPoint = screen_width / 2 / 3;// 测试中心区域的值,先随便写一个值
			centerPointX = screen_width / 2;
			centerPointY = (int) (screen_width / 2 + pxTopview);
			if (context instanceof FishEyeActivity) {// 壁装双击切换下标
				FishEyeActivity act = (FishEyeActivity) context;
				if (mCamera.mInstallMode == 1) {
					if (act.mWallMode == 1) {// 1-壁装全景
						this.SetShowScreenMode(HiGLMonitor.VIEW_MODE_SIDE, 0);
						act.rbtn_circle.setChecked(true);
						act.mWallMode = 0;
					} else {
						this.SetShowScreenMode(HiGLMonitor.VIEW_MODE_SIDE, 1);
						act.rbtn_wall_overallview.setChecked(true);
						act.mWallMode = 1;
					}
					return true;
				}
			}

			// 区域 1
			boolean area_one = x < screen_width / 2 && y > pxTopview && y < pxTopview + pxMoniter / 2;
			// 区域2
			boolean area_two = x > screen_width / 2 && x < screen_width && y < pxTopview + pxMoniter / 2 && y > pxTopview;
			// 区域3
			boolean area_there = x < screen_width / 2 && y > pxTopview + pxMoniter / 2 && y < pxMoniter + pxTopview;
			// 区域4
			boolean area_four = x > screen_width / 2 && y > pxTopview + pxMoniter / 2 && y < pxMoniter + pxTopview;
			if (FishEyeActivity.mFrameMode == 1) {
				if (mIsZoom == true && GetFishLager() == 0.0) {// 解决圆双击放大,然后两个手指缩小至最小,然后再双击放大的bug
					mIsZoom = false;
				}
				if (GetFishLager() > 0.0 && !mIsZoom) {// 解决两个手指放大后,双击必须要缩小为原图的bug
					this.SetPosition(false, 8);
					return true;
				}
				if (mIsZoom) {
					this.SetPosition(false, mSetPosition);
					mIsZoom = !mIsZoom;
					return true;
				}
				handlerFrameMode1(x, y, pxTopview, centerPoint, area_one, area_two, area_there, area_four);
			} else if (FishEyeActivity.mFrameMode == 4) {
				// handlerFrameMode4(area_one, area_two, area_there, area_four);
			}
			// else if (FishEyeActivity.mFrameMode == 2) {
			// if (mIsZoom) {
			// this.SetShowScreenMode(HiGLMonitor.VIEW_MODE_CARTOON, 1);// 圆柱体
			// } else {
			// this.SetShowScreenMode(HiGLMonitor.VIEW_MODE_CARTOON, 0);// 立体碗
			// }
			// mIsZoom = !mIsZoom;
			// }
			return true;
		}
		return true;
	}

	private void handlerFrameMode1(float x, float y, float pxTopview, float centerPoint, boolean area_one, boolean area_two, boolean area_there, boolean area_four) {
		if (area_one) {
			float distanceX = screen_width / 2 - x;
			float distanceY = screen_width / 2 + pxTopview - y;
			double distance = Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));
			if (distance < screen_width / 2 && distance > centerPoint) {// 根据勾股定理算出来的距离如果小于半径就在区域1里面
				float absX = Math.abs(x - centerPointX);
				float absY = Math.abs(y - centerPointY);
				if (absX > absY && distance < screen_width / 2 && distance > centerPoint) {// 我是真正的6模块
					setPostion(6);
				} else if (absX < absY && distance < screen_width / 2 && distance > centerPoint) {// 我是真正的6模块
					setPostion(7);
				}
			} else if (distance < centerPoint) {
				setPostion(8);
			}
		} else if (area_two) {
			float distanceX = x - screen_width / 2;
			float distanceY = screen_width / 2 + pxTopview - y;
			double distance = Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));
			if (distance < screen_width / 2 && distance > centerPoint) {
				float absX = Math.abs(x - centerPointX);
				float absY = Math.abs(y - centerPointY);
				if (absX > absY && distance < screen_width / 2 && distance > centerPoint) {// 我是真正的1模块
					setPostion(1);
				} else if (absX < absY && distance < screen_width / 2 && distance > centerPoint) {// 我是真正的0模块
					setPostion(0);
				}
			} else if (distance < centerPoint) {
				setPostion(8);
			}
		} else if (area_there) {
			float distanceX = screen_width / 2 - x;
			float distanceY = y - (screen_width / 2 + pxTopview);
			double distance = Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));
			if (distance < screen_width / 2 && distance > centerPoint) {
				float absX = Math.abs(x - centerPointX);
				float absY = Math.abs(y - centerPointY);
				if (absX > absY && distance < screen_width / 2 && distance > centerPoint) {// 我是真正的5模块
					setPostion(5);
				} else if (absX < absY && distance < screen_width / 2 && distance > centerPoint) {// 我是真正的4模块
					setPostion(4);
				}
			} else if (distance < centerPoint) {
				setPostion(8);
			}
		} else if (area_four) {
			float distanceX = x - screen_width / 2;
			float distanceY = y - screen_width / 2 - pxTopview;
			double distance = Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));
			if (distance < screen_width / 2 && distance > centerPoint) {
				float absX = Math.abs(x - centerPointX);
				float absY = Math.abs(y - centerPointY);
				if (absX > absY && distance < screen_width / 2 && distance > centerPoint) {// 我是真正的2模块
					setPostion(2);
				} else if (absX < absY && distance < screen_width / 2 && distance > centerPoint) {// 我是真正的3模块
					setPostion(3);
				}
			} else if (distance < centerPoint) {
				setPostion(8);
			}
		}
	}

	private void setPostion(int position) {
		if (!mIsZoom) {
			this.SetPosition(true, position);
			mIsZoom = !mIsZoom;
			mSetPosition = position;
		}
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		return false;
	}

	private void startRotate(final int gesture, final int num) {
		Log.i("tedu", "startRotate" + gesture + "::::" + num);
		new Thread() {
			public void run() {
				for (int i = 0; i <= num;) {
					if (i < num / 5)
						i++;
					else if (i < num * 2 / 5)
						i += 5;
					else if (i < num * 3 / 5)
						i += 3;
					else if (i < num * 4 / 5)
						i += 1;
					else
						i++;

					try {
						Thread.sleep(20);
						int b = (num * 2 / 3 - i);
						if (b < 4)
							b = 4;
						int j = b / 4;
						for (int a = j; a >= 0; a--) {
							MyLiveViewGLMonitor.this.SetGesture(gesture);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
		}.start();
	}

	public boolean mIsZoom = false;

	public void showMyToast(final Toast toast, final int cnt) {
		final Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				toast.show();
			}
		}, 0, 3000);
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				toast.cancel();
				timer.cancel();
			}
		}, cnt);
	}

}
