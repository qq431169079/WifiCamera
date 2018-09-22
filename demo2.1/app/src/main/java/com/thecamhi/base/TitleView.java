package com.thecamhi.base;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hichip1.R;

public class TitleView extends RelativeLayout implements OnClickListener {
	public static final int NAVIGATION_BUTTON_LEFT = 0;
	public static final int NAVIGATION_BUTTON_RIGHT = 1;
	public static final int NAVIGATION_IMAGEVIEW_RIGHT = 2;
	private NavigationBarButtonListener btnListener;

	public TitleView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context);
	}

	private void initView(Context context) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.title_view_in_all_main_view, this, true);
	}

	public void setTitle(String txt) {
		TextView tv = (TextView) findViewById(R.id.title_middle);
		tv.setVisibility(View.VISIBLE);
		tv.setText(txt);
	}

	public void setRightBtnTextBackround(int resId) {
		ImageView btn_finish = (ImageView) findViewById(R.id.btn_finish);
		btn_finish.setImageResource(resId);

	}
	public void setLeftBtnTextBackround(int resId) {
		ImageView btn_return = (ImageView) findViewById(R.id.btn_return);
		btn_return.setImageResource(resId);
		
	}


	public void setRightBackroundPadding(int left,int top,int right,int bottom){
		ImageView btn_finish = (ImageView) findViewById(R.id.btn_finish);
		btn_finish.setPadding(left, top, right, bottom);
	}
	public void setLeftBackroundPadding(int left,int top,int right,int bottom){
		ImageView btn_return = (ImageView) findViewById(R.id.btn_return);
		btn_return.setPadding(left, top, right, bottom);
	}

	public void setButton(int which) {
		switch (which) {
		case NAVIGATION_BUTTON_LEFT: {
			ImageView btn = (ImageView) findViewById(R.id.btn_return);
			btn.setTag(which);
			btn.setVisibility(View.VISIBLE);
			btn.setOnClickListener(this);
		}
			break;

		case NAVIGATION_BUTTON_RIGHT:
			ImageView btn = (ImageView) findViewById(R.id.btn_finish);
			btn.setTag(which);
			btn.setVisibility(View.VISIBLE);
			btn.setOnClickListener(this);
			break;
		}

	}

	@Override
	public void onClick(View v) {
		int which = ((Integer) v.getTag()).intValue();
		if (btnListener != null) {
			btnListener.OnNavigationButtonClick(which);
		}

	}

	public interface NavigationBarButtonListener {

		 void OnNavigationButtonClick(int which);
	}

	public void setNavigationBarButtonListener(NavigationBarButtonListener listener) {
		btnListener = listener;
	}

}
