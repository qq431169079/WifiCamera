package com.hichip.activity.RF;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;

import com.hichip1.R;
import com.hichip.callback.ICameraIOSessionCallback;
import com.hichip.control.HiCamera;
import com.thecamhi.base.TitleView;
import com.thecamhi.base.TitleView.NavigationBarButtonListener;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;
import com.thecamhi.main.HiActivity;
import com.thecamhi.main.MainActivity;

public class AddRFActivity extends HiActivity implements OnClickListener, ICameraIOSessionCallback {
	private TitleView mTitleView;
	private RelativeLayout mRlRemote,rl_door,rl_infrared,rl_smoke,rl_gas,rl_other;
	private MyCamera mMyCamera;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_rf_new);
		String uid=getIntent().getStringExtra(HiDataValue.EXTRAS_KEY_UID);
		if(TextUtils.isEmpty(uid)){
			finish();
			return;
		}
		for(MyCamera camera:HiDataValue.CameraList){
			if(uid.equals(camera.getUid())){
				this.mMyCamera=camera;
				break;
			}
		}
		initView();
		setListerners();
	}
	@Override
	protected void onResume() {
		super.onResume();
		if(mMyCamera!=null){
			mMyCamera.registerIOSessionListener(this);
			
		}
	}
	@Override
	protected void onPause() {
		super.onPause();
		if(mMyCamera!=null){
			mMyCamera.unregisterIOSessionListener(this);
		}
	}


	private void setListerners() {
		mRlRemote.setOnClickListener(this);
		rl_door.setOnClickListener(this);
		rl_infrared.setOnClickListener(this);
		rl_smoke.setOnClickListener(this);
		rl_gas.setOnClickListener(this);
		rl_other.setOnClickListener(this);

	}

	private void initView() {
		mTitleView = (TitleView) findViewById(R.id.add_rf);
		mTitleView.setButton(TitleView.NAVIGATION_BUTTON_LEFT);
		mTitleView.setTitle("RF设备类型");
		mTitleView.setNavigationBarButtonListener(new NavigationBarButtonListener() {
			@Override
			public void OnNavigationButtonClick(int which) {
				switch (which) {
				case TitleView.NAVIGATION_BUTTON_LEFT:
					AddRFActivity.this.finish();
					break;
				}
			}
		});
		mRlRemote = (RelativeLayout) findViewById(R.id.rl_remote_control);
		rl_door=(RelativeLayout) findViewById(R.id.rl_door);
		rl_infrared=(RelativeLayout) findViewById(R.id.rl_infrared);
		rl_smoke=(RelativeLayout) findViewById(R.id.rl_smoke);
		rl_gas=(RelativeLayout) findViewById(R.id.rl_gas);
		rl_other=(RelativeLayout) findViewById(R.id.rl_other);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.rl_remote_control:
			Intent intent=new Intent(AddRFActivity.this,RemoteControlActivity.class);
			intent.putExtra(HiDataValue.EXTRAS_KEY_UID, mMyCamera.getUid());
			startActivity(intent);
			break;
		case R.id.rl_door:
			intent=new Intent(AddRFActivity.this,CheckRFCodeActivity.class);
			intent.putExtra(HiDataValue.EXTRAS_KEY_UID, mMyCamera.getUid());
			intent.putExtra(HiDataValue.EXTRAS_RF_TYPE, "door");
			startActivity(intent);
			break;
		case R.id.rl_infrared:
			intent=new Intent(AddRFActivity.this,CheckRFCodeActivity.class);
			intent.putExtra(HiDataValue.EXTRAS_KEY_UID, mMyCamera.getUid());
			intent.putExtra(HiDataValue.EXTRAS_RF_TYPE, "infra");
			startActivity(intent);
			break;
		case R.id.rl_smoke:
			intent=new Intent(AddRFActivity.this,CheckRFCodeActivity.class);
			intent.putExtra(HiDataValue.EXTRAS_KEY_UID, mMyCamera.getUid());
			intent.putExtra(HiDataValue.EXTRAS_RF_TYPE, "fire");
			startActivity(intent);
			break;
		case R.id.rl_gas:
			intent=new Intent(AddRFActivity.this,CheckRFCodeActivity.class);
			intent.putExtra(HiDataValue.EXTRAS_KEY_UID, mMyCamera.getUid());
			intent.putExtra(HiDataValue.EXTRAS_RF_TYPE, "gas");
			startActivity(intent);
			break;
		case R.id.rl_other:
			intent=new Intent(AddRFActivity.this,CheckRFCodeActivity.class);
			intent.putExtra(HiDataValue.EXTRAS_KEY_UID, mMyCamera.getUid());
			intent.putExtra(HiDataValue.EXTRAS_RF_TYPE, "beep");
			startActivity(intent);
			break;
		}

	}
	
	
	@Override
	public void receiveIOCtrlData(HiCamera arg0, int arg1, byte[] arg2, int arg3) {
		
	}
	@Override
	public void receiveSessionState(HiCamera arg0, int arg1) {
		if(arg0!=mMyCamera) return;
		Message msg=Message.obtain();
		msg.what=HiDataValue.HANDLE_MESSAGE_SESSION_STATE;
		msg.obj=arg0;
		msg.arg1=arg1;
		mHandler.sendMessage(msg);
		
		
	}
	private Handler mHandler=new Handler(){
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HiDataValue.HANDLE_MESSAGE_SESSION_STATE:
				if(msg.arg1==HiCamera.CAMERA_CONNECTION_STATE_DISCONNECTED){
					Intent intent=new Intent(AddRFActivity.this,MainActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
				}
				break;

			}
		};
	};
	

}






