package com.thecamhi.zxing;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import com.customview.dialog.NiftyDialogBuilder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.hichip1.R;
import com.hichip.sdk.HiChipSDK;
import com.tencent.android.tpush.common.s;
import com.thecamhi.base.HiToast;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;
import com.thecamhi.main.MainActivity;
import com.thecamhi.zxing.utils.UriUtils;

/**
 * Initial the camera
 */
public class CaptureActivity extends Activity implements Callback {

	private CaptureActivityHandler handler;
	private ViewfinderView viewfinderView;
	private boolean hasSurface;
	private Vector<BarcodeFormat> decodeFormats;
	private String characterSet;
	private InactivityTimer inactivityTimer;
	private MediaPlayer mediaPlayer;
	private boolean playBeep;
	private static final float BEEP_VOLUME = 0.10f;
	private boolean vibrate;
	private Button cancelScanButton;

	int ifOpenLight = 0; // 判断是否开启闪光灯

	private ArrayList<MyCamera> mAnalyCameraList = new ArrayList<>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camera);
		CameraManager.init(getApplication());
		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		cancelScanButton = (Button) this.findViewById(R.id.btn_cancel_scan);
		hasSurface = false;
		inactivityTimer = new InactivityTimer(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			initCamera(surfaceHolder);
		} else {
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		decodeFormats = null;
		characterSet = null;

		playBeep = true;
		AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
		if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
			playBeep = false;
		}
		initBeepSound();
		vibrate = true;

		// quit the scan view
		cancelScanButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				CaptureActivity.this.finish();
			}
		});
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		CameraManager.get().closeDriver();
	}

	@Override
	protected void onDestroy() {
		inactivityTimer.shutdown();
		super.onDestroy();
	}

	/**
	 * Handler scan result
	 * 
	 * @param result
	 * @param barcode
	 *            获取结果
	 */
	public void handleDecode(Result result, Bitmap barcode) {
		inactivityTimer.onActivity();
		playBeepSoundAndVibrate();
		String resultString = result.getText();
		if (resultString.equals("")) {
			Toast.makeText(CaptureActivity.this, getString(R.string.toast_scan_fail), Toast.LENGTH_SHORT).show();
		} else {
			if (!TextUtils.isEmpty(resultString) && resultString.length() > 8) {
				String sub = resultString.substring(0, 8);
				if (sub.equalsIgnoreCase(getString(R.string.app_name) + "_AC")) {// 二维码是加密分享的UID
					handData(resultString);
				} else {// 二维码是UID
					Intent resultIntent = new Intent();
					Bundle bundle = new Bundle();
					bundle.putString(HiDataValue.EXTRAS_KEY_UID, resultString);
					resultIntent.putExtras(bundle);
					CaptureActivity.this.setResult(RESULT_OK, resultIntent);
					CaptureActivity.this.finish();
				}
			}
		}
	}

	private void handData(String resultString) {
		String string = resultString.substring(8, resultString.length());
		byte[] buff = new byte[resultString.getBytes().length];
		byte[] datas = string.getBytes();
		System.arraycopy(datas, 0, buff, 0, datas.length);
		HiChipSDK.Aes_Decrypt(buff, datas.length);
		String decryptStr = new String(buff).trim();
		// Log.i("tedu", "--扫出来的结果 解密后:-->" + decryptStr);
		// 解析数据
		analyData(decryptStr);
	}

	private StringBuffer sbAddCamerUid = new StringBuffer();

	private void analyData(String string) {
		try {
			JSONArray jsonArray = new JSONArray(string);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				String uid = jsonObject.getString("U").substring(0, jsonObject.getString("U").length() - 2);
				String username = jsonObject.getString("A").substring(0, jsonObject.getString("A").length() - 2);
				String password = jsonObject.getString("P").substring(0, jsonObject.getString("P").length() - 2);
				MyCamera camera = new MyCamera(CaptureActivity.this, getString(R.string.title_camera_fragment), uid, username, password);
				mAnalyCameraList.add(camera);
			}
			if (mAnalyCameraList != null && mAnalyCameraList.size() > 0) {
				for (MyCamera camera : HiDataValue.CameraList) {
					for (int i = 0; i < mAnalyCameraList.size(); i++) {
						if (camera.getUid().equalsIgnoreCase(mAnalyCameraList.get(i).getUid())) {
							mAnalyCameraList.remove(i);
						}
					}
				}
				if (mAnalyCameraList.size() < 1) {
					HiToast.showToast(CaptureActivity.this, getString(R.string.toast_device_added));
					CaptureActivity.this.finish();
				} else {
					for (int i = 0; i < mAnalyCameraList.size(); i++) {
						MyCamera camera = mAnalyCameraList.get(i);
						if (i < mAnalyCameraList.size() - 1) {
							sbAddCamerUid.append(camera.getUid() + "\n");
						} else {
							sbAddCamerUid.append(camera.getUid());
						}
					}
					final NiftyDialogBuilder dialog = NiftyDialogBuilder.getInstance(CaptureActivity.this);
					if (mAnalyCameraList.size() > 3) {
						dialog.withMessageLayoutWrap();
					}
					dialog.withTitle(getString(R.string.add_camera)).withMessage(sbAddCamerUid.toString()).withButton1Text(getString(R.string.cancel)).withButton2Text(getString(R.string.toast_confirm_add));
					dialog.setButton1Click(new OnClickListener() {
						@Override
						public void onClick(View v) {
							dialog.dismiss();
							CaptureActivity.this.finish();
						}
					});
					dialog.setButton2Click(new OnClickListener() {
						@Override
						public void onClick(View v) {
							for (MyCamera camera : mAnalyCameraList) {
								camera.saveInDatabase(CaptureActivity.this);
								camera.saveInCameraList();
							}
							dialog.dismiss();
							Intent intent = new Intent();
							intent.setAction(HiDataValue.ACTION_CAMERA_INIT_END);
							sendBroadcast(intent);

							intent = new Intent(CaptureActivity.this, MainActivity.class);
							intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(intent);
						}
					});
					dialog.isCancelable(false);
					dialog.show();
				}
			}
		} catch (JSONException e) {
			HiToast.showToast(CaptureActivity.this, getString(R.string.toast_scan_fail));
			e.printStackTrace();
		}

	}

	/*
	 * 获取带二维码的相片进行扫描
	 */
	@SuppressLint("InlinedApi")
	public void pickPictureFromAblum(View v) {
		// 打开手机中的相册
		Intent innerIntent = new Intent(Intent.ACTION_GET_CONTENT);
		innerIntent.setType("image/*");
		startActivityForResult(innerIntent, 0X22);
	}

	String photo_path;
	ProgressDialog mProgress;
	Bitmap scanBitmap;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case 0X22:
				handleAlbumPic(data);
				break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * 处理选择的图片
	 * 
	 * @param data
	 */
	private void handleAlbumPic(Intent data) {
		// 获取选中图片的路径
		photo_path = UriUtils.getRealPathFromUri(CaptureActivity.this, data.getData());
		mProgress = new ProgressDialog(CaptureActivity.this);
		mProgress.setMessage(getString(R.string.toast_scanning));
		mProgress.setCancelable(false);
		mProgress.show();
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mProgress.dismiss();
				Result result = scanningImage(photo_path);
				if (result != null) {
					mProgress.dismiss();
					String resultString = result.getText();
					if (resultString.equals("")) {
						Toast.makeText(CaptureActivity.this, getString(R.string.toast_scan_fail), Toast.LENGTH_SHORT).show();
						CaptureActivity.this.finish();
					} else {
						handData(resultString);
					}
				} else {
					mProgress.dismiss();
					Toast.makeText(CaptureActivity.this, getString(R.string.toast_scan_fail), Toast.LENGTH_SHORT).show();
					CaptureActivity.this.finish();
				}
			}
		});
	}

	/**
	 * 扫描二维码图片的方法
	 * 
	 * @param path
	 * @return
	 */
	public Result scanningImage(String path) {
		if (TextUtils.isEmpty(path)) {
			return null;
		}
		Map<DecodeHintType, Object> hints = new LinkedHashMap<DecodeHintType, Object>();
		hints.put(DecodeHintType.CHARACTER_SET, "UTF8"); // 设置二维码内容的编码
		// 优化精度
		hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
		// 复杂模式，开启PURE_BARCODE模式
		hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
		//这里不能对scanBitmap进行压缩处理,处理之后就会有bug
		scanBitmap = BitmapFactory.decodeFile(path);
		RGBLuminanceSource source = new RGBLuminanceSource(scanBitmap);
		BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
		QRCodeReader reader = new QRCodeReader();
		try {
			Result result = reader.decode(bitmap1, hints);
			return result;
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (ChecksumException e) {
			e.printStackTrace();
		} catch (FormatException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 从二维码 解析的中文乱码 解决方法
	 * 
	 * @param str
	 * @return
	 */
	private String recode(String str) {
		String formart = "";
		try {
			boolean ISO = Charset.forName("ISO-8859-1").newEncoder().canEncode(str);
			if (ISO) {
				formart = new String(str.getBytes("ISO-8859-1"), "GB2312");
				Log.i("1234      ISO8859-1", formart);
			} else {
				formart = str;
				Log.i("1234      stringExtra", str);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return formart;
	}

	// 是否开启闪光灯
	public void IfOpenLight(View v) {
		ifOpenLight++;

		switch (ifOpenLight % 2) {
		case 0:
			// 关闭
			CameraManager.get().closeLight();
			break;

		case 1:
			// 打开
			CameraManager.get().openLight(); // 开闪光灯
			break;
		default:
			break;
		}
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder);
		} catch (IOException ioe) {
			return;
		} catch (RuntimeException e) {
			return;
		}
		if (handler == null) {
			handler = new CaptureActivityHandler(this, decodeFormats, characterSet);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;

	}

	public ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	public Handler getHandler() {
		return handler;
	}

	public void drawViewfinder() {
		viewfinderView.drawViewfinder();

	}

	private void initBeepSound() {
		if (playBeep && mediaPlayer == null) {
			// The volume on STREAM_SYSTEM is not adjustable, and users found it
			// too loud,
			// so we now play on the music stream.
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setOnCompletionListener(beepListener);

			AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.beep);
			try {
				mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
				file.close();
				mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
				mediaPlayer.prepare();
			} catch (IOException e) {
				mediaPlayer = null;
			}
		}
	}

	private static final long VIBRATE_DURATION = 200L;

	private void playBeepSoundAndVibrate() {
		if (playBeep && mediaPlayer != null) {
			mediaPlayer.start();
		}
		if (vibrate) {
			Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_DURATION);
		}
	}

	/**
	 * When the beep has finished playing, rewind to queue up another one.
	 */
	private final OnCompletionListener beepListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mediaPlayer) {
			mediaPlayer.seekTo(0);
		}
	};

}