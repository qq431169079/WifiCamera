package com.thecamhi.activity.setting;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import com.hichip1.R;
import com.hichip.callback.ICameraIOSessionCallback;
import com.hichip.content.HiChipDefines;
import com.hichip.control.HiCamera;
import com.hichip.widget.SwitchButton;
import com.thecamhi.base.HiToast;
import com.thecamhi.base.TitleView;
import com.thecamhi.base.TitleView.NavigationBarButtonListener;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;
import com.thecamhi.main.HiActivity;

public class EmailSettingActivity extends HiActivity implements ICameraIOSessionCallback{
	private MyCamera mCamera;
	private EditText mailbox_setting_server_edt, mailbox_setting_port_edt, mailbox_setting_username_edt,
			mailbox_setting_psw_edt, mailbox_setting_receive_address_edt, mailbox_setting_sending_address_edt,
			mailbox_setting_theme_edt, mailbox_setting_message_edt;
	private Spinner mailbox_setting_safety_spn;
	private SwitchButton mailbox_setting_check_tgbtn;
	HiChipDefines.HI_P2P_S_EMAIL_PARAM param;
	private boolean isCheck = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mailbox_setting);

		String uid = getIntent().getStringExtra(HiDataValue.EXTRAS_KEY_UID);

		for (MyCamera camera : HiDataValue.CameraList) {
			if (uid.equals(camera.getUid())) {
				mCamera = camera;
				mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_EMAIL_PARAM, null);

				break;

			}
		}

		initView();
	}

	private void initView() {
		TitleView title = (TitleView) findViewById(R.id.title_top);
		title.setTitle(getResources().getString(R.string.title_mailbox_settings));
		title.setButton(TitleView.NAVIGATION_BUTTON_LEFT);
		title.setNavigationBarButtonListener(new NavigationBarButtonListener() {

			@Override
			public void OnNavigationButtonClick(int which) {
				switch (which) {
				case TitleView.NAVIGATION_BUTTON_LEFT:
					EmailSettingActivity.this.finish();
					break;
				}
			}
		});

		mailbox_setting_server_edt = (EditText) findViewById(R.id.mailbox_setting_server_edt);
		mailbox_setting_port_edt = (EditText) findViewById(R.id.mailbox_setting_port_edt);
		mailbox_setting_username_edt = (EditText) findViewById(R.id.mailbox_setting_username_edt);
		mailbox_setting_psw_edt = (EditText) findViewById(R.id.mailbox_setting_psw_edt);
		mailbox_setting_receive_address_edt = (EditText) findViewById(R.id.mailbox_setting_receive_address_edt);
		mailbox_setting_sending_address_edt = (EditText) findViewById(R.id.mailbox_setting_sending_address_edt);
		mailbox_setting_theme_edt = (EditText) findViewById(R.id.mailbox_setting_theme_edt);
		mailbox_setting_message_edt = (EditText) findViewById(R.id.mailbox_setting_message_edt);
		mailbox_setting_safety_spn = (Spinner) findViewById(R.id.mailbox_setting_safety_spn);
		ArrayAdapter<CharSequence> adapter_frequency = ArrayAdapter.createFromResource(this, R.array.safety_connection,
				android.R.layout.simple_spinner_item);
		adapter_frequency.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mailbox_setting_safety_spn.setAdapter(adapter_frequency);

		mailbox_setting_check_tgbtn = (SwitchButton) findViewById(R.id.mailbox_setting_check_tgbtn);

		Button testBtn = (Button) findViewById(R.id.mailbox_setting_test_btn);

		testBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (param == null) {
					return;
				}
				isCheck = true;
				sendMailSetting(isCheck);
			}
		});

		Button mailbox_setting_application_btn = (Button) findViewById(R.id.mailbox_setting_application_btn);
		mailbox_setting_application_btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (param == null) {
					return;
				}
				isCheck = false;
				sendMailSetting(isCheck);

			}
		});
	}

	protected void sendMailSetting(boolean check) {

		String serverStr = mailbox_setting_server_edt.getText().toString().trim();
		String portStr = mailbox_setting_port_edt.getText().toString().trim();
		String usernameStr = mailbox_setting_username_edt.getText().toString().trim();
		String pswStr = mailbox_setting_psw_edt.getText().toString().trim();

		String sendingStr = mailbox_setting_receive_address_edt.getText().toString().trim();
		String receiveStr = mailbox_setting_sending_address_edt.getText().toString().trim();
		String themeStr = mailbox_setting_theme_edt.getText().toString().trim();
		String messageStr = mailbox_setting_message_edt.getText().toString().trim();

		param.setStrSvr(serverStr);
		if (!TextUtils.isEmpty(portStr)) {
			if(Integer.parseInt(portStr)>65535||Integer.parseInt(portStr)<=0){
				mailbox_setting_port_edt.setText(String.valueOf(param.u32Port));
				HiToast.showToast(EmailSettingActivity.this, getString(R.string.port_limit));
				return;
			}else {
				param.u32Port = Integer.valueOf(portStr);
			}
		} else {
			HiToast.showToast(EmailSettingActivity.this, getString(R.string.tips_port_notnull));
			return;
		}
		param.setStrUsernm(usernameStr);
		param.setStrPasswd(pswStr);
		param.setStrFrom(receiveStr);
		param.setStrTo(sendingStr);
		param.setStrSubject(themeStr);
		param.setStrText(messageStr);
		if (messageStr.trim().getBytes().length > 128) {
			HiToast.showToast(EmailSettingActivity.this, getString(R.string.tips_email_tolong));
			return;
		}
		param.u32LoginType = mailbox_setting_check_tgbtn.isChecked() ? 1 : 3;
		param.u32Auth = mailbox_setting_safety_spn.getSelectedItemPosition();
		byte[] sendParam = HiChipDefines.HI_P2P_S_EMAIL_PARAM_EXT.parseContent(param, check ? 1 : 0);
		showLoadingProgress();
		mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_EMAIL_PARAM_EXT, sendParam);
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

					case HiChipDefines.HI_P2P_GET_EMAIL_PARAM:
						param = new HiChipDefines.HI_P2P_S_EMAIL_PARAM(data);
						mailbox_setting_server_edt.setText(new String(param.strSvr).trim());
						mailbox_setting_port_edt.setText(String.valueOf(param.u32Port).trim());
						mailbox_setting_username_edt.setText(new String(param.strUsernm).trim());
						mailbox_setting_psw_edt.setText(new String(param.strPasswd).trim());
						mailbox_setting_receive_address_edt.setText(new String(param.strTo[0]).trim());
						mailbox_setting_sending_address_edt.setText(new String(param.strFrom).trim());
						mailbox_setting_theme_edt.setText(new String(param.strSubject).trim());
						// mailbox_setting_message_edt.setText(Packet.getString(param.strText));
						mailbox_setting_message_edt.setText(new String(param.strText).trim());
						if (param.u32LoginType == 1) {
							mailbox_setting_check_tgbtn.setChecked(true);
						} else if (param.u32LoginType == 3) {
							mailbox_setting_check_tgbtn.setChecked(false);
						}

						mailbox_setting_safety_spn.setSelection(param.u32Auth);

						break;

					case HiChipDefines.HI_P2P_SET_EMAIL_PARAM_EXT:
						if (!isCheck) {
							HiToast.showToast(EmailSettingActivity.this,
									getResources().getString(R.string.mailbox_setting_save_success));
							// finish();
						} else {
							HiToast.showToast(EmailSettingActivity.this,
									getResources().getString(R.string.mailbox_setting_check_success));
						}

						dismissLoadingProgress();

						break;

					}
				} else {
					switch (msg.arg1) {
					case HiChipDefines.HI_P2P_SET_EMAIL_PARAM_EXT:
						if (!isCheck) {
							HiToast.showToast(EmailSettingActivity.this,
									getResources().getString(R.string.mailbox_setting_save_failed));
						} else {
							HiToast.showToast(EmailSettingActivity.this,
									getResources().getString(R.string.mailbox_setting_check_failed));
						}
						dismissLoadingProgress();
						break;

					}

				}
			}
				break;

			}
		}
	};

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

}
