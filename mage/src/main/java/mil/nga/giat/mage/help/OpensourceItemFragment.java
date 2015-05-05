package mil.nga.giat.mage.help;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import mil.nga.giat.mage.R;

public class OpensourceItemFragment extends FrameLayout {

	public OpensourceItemFragment(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView(context, attrs);
	}

	public OpensourceItemFragment(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context, attrs);
	}

	public OpensourceItemFragment(Context context) {
		super(context);
		initView(context, null);
	}

	private void initView(Context context, AttributeSet attrs) {
		View view = inflate(getContext(), R.layout.fragment_opensource_item, null);

		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OpensourceItemFragment);
			CharSequence name = a.getText(R.styleable.OpensourceItemFragment_name);
			CharSequence copyright = a.getText(R.styleable.OpensourceItemFragment_copyright);
			CharSequence license = a.getText(R.styleable.OpensourceItemFragment_license);
			a.recycle();

			TextView nameTextView = (TextView) view.findViewById(R.id.opensource_item_name);
			if(StringUtils.isBlank(name)) {
				nameTextView.setVisibility(View.GONE);
			} else {
				nameTextView.setText(name);
			}

			TextView copyrightTextView = (TextView) view.findViewById(R.id.opensource_item_copyright);
			if(StringUtils.isBlank(copyright)) {
				copyrightTextView.setVisibility(View.GONE);
			} else {
				copyrightTextView.setText(copyright);
			}

			TextView licenseTextView = (TextView) view.findViewById(R.id.opensource_item_license);
			if(StringUtils.isBlank(license)) {
				licenseTextView.setVisibility(View.GONE);
			} else {
				licenseTextView.setText(license);
			}
		}

		addView(view);
	}
}
