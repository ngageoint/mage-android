package mil.nga.giat.mage.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;

public class CheckedLinearLayout extends LinearLayout implements Checkable {
    
    private CheckedTextView checkedTextView;
    
    public CheckedLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // find checked text view
        int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {
            View v = getChildAt(i);
            if (v instanceof CheckedTextView) {
                checkedTextView = (CheckedTextView)v;
            }
        }       
    }
    
    @Override 
    public boolean isChecked() { 
        return checkedTextView != null ? checkedTextView.isChecked() : false; 
    }
    
    @Override 
    public void setChecked(boolean checked) {
        if (checkedTextView != null) {
            checkedTextView.setChecked(checked);
        }
    }
    
    @Override 
    public void toggle() { 
        if (checkedTextView != null) {
            checkedTextView.toggle();
        }
    } 
} 