package mil.nga.giat.mage.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.LinearLayout;

import mil.nga.giat.mage.R;

public class CheckedLinearLayout extends LinearLayout implements Checkable {
    
    private CheckBox checkBox;
    
    public CheckedLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        checkBox = (CheckBox) findViewById(R.id.checkbox);
    }
    
    @Override 
    public boolean isChecked() { 
        return checkBox != null ? checkBox.isChecked() : false;
    }
    
    @Override 
    public void setChecked(boolean checked) {
        if (checkBox != null) {
            checkBox.setChecked(checked);
        }
    }
    
    @Override 
    public void toggle() { 
        if (checkBox != null) {
            checkBox.toggle();
        }
    } 
} 