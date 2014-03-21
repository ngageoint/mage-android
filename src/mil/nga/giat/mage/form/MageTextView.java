package mil.nga.giat.mage.form;

import mil.nga.giat.mage.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

public class MageTextView extends TextView {

	private String propertyKey;
	private int propertyType;

	public MageTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	    TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MageFormElement);
	    String propertyKey = typedArray.getString(R.styleable.MageFormElement_propertyKey);
	    setPropertyKey(propertyKey);
	    setPropertyType(typedArray.getInt(R.styleable.MageFormElement_propertyType, 0));
	    setPropertyKey(propertyKey);
	    typedArray.recycle();
	}
	
	public void setPropertyKey(String propertyKey) {
		this.propertyKey = propertyKey;
	}
	
	public String getPropertyKey() {
		return this.propertyKey;
	}
	
	public void setPropertyType(int propertyType) {
		this.propertyType = propertyType;
	}
	
	public MagePropertyType getPropertyType() {
		return MagePropertyType.getPropertyType(this.propertyType);
	}
}
