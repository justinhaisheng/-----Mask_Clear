package com.android.server.power;

import android.app.Dialog;
import android.content.Context;
import android.widget.TextView;

import com.android.internal.R;



public class CustomWaitDialog extends Dialog {
	
	private TextView nTextView;
	private CharSequence nText;

	private CustomWaitDialog(Context context) {
		this(context, 0);
	}

	private CustomWaitDialog(Context context, int theme) {
		super(context, R.style.CustomDialogHolo);

		setContentView(R.layout.custom_wait_dialog_layout);
		nTextView = (TextView) findViewById(R.id.text_off_or_reboot);

		setCancelable(false);
		setCanceledOnTouchOutside(false);
	}

	public void setText(CharSequence text) {
		nText = text;
	}

	@Override
	public void show() {

		nTextView.setText(nText);

		super.show();

	}

	public static class Builder {
		private CustomWaitDialog mDialog;

		public Builder(Context context) {
			mDialog = new CustomWaitDialog(context);
		}

		public Builder setText(CharSequence text) {
			mDialog.setText(text);
			return this;
		}

		public CustomWaitDialog build() {
			return mDialog;
		}

		public void setCanceledOnTouchOutside(boolean flag) {
			mDialog.setCanceledOnTouchOutside(flag);
		}
	}

}
