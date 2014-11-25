package mil.nga.giat.mage.map;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class GoogleMapWrapper extends FrameLayout {

    public interface OnMapPanListener {
        public void onMapPan();
    }
    
    private OnMapPanListener mapPanListener;

    public GoogleMapWrapper(Context context) {
        super(context);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        int action = MotionEventCompat.getActionMasked(event);

        if (action == MotionEvent.ACTION_MOVE && mapPanListener != null) {
            mapPanListener.onMapPan();
        }

        return super.dispatchTouchEvent(event);
    }

    public void setOnMapPanListener(OnMapPanListener mapPanListener) {
        this.mapPanListener = mapPanListener;
    }
}