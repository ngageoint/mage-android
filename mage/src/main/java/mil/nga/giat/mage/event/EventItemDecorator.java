package mil.nga.giat.mage.event;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import mil.nga.giat.mage.R;

/**
 * Created by wnewman on 2/23/18.
 */

public class EventItemDecorator extends RecyclerView.ItemDecoration  {

    private Drawable divider;
    private final Rect bounds = new Rect();

    public EventItemDecorator(Context context) {
        this.divider = ContextCompat.getDrawable(context, R.drawable.event_divider);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (parent.getLayoutManager() == null) {
            return;
        }

        c.save();
        final int left;
        final int right;
        if (parent.getClipToPadding()) {
            left = parent.getPaddingLeft();
            right = parent.getWidth() - parent.getPaddingRight();
            c.clipRect(left, parent.getPaddingTop(), right, parent.getHeight() - parent.getPaddingBottom());
        } else {
            left = 0;
            right = parent.getWidth();
        }

        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            parent.getDecoratedBoundsWithMargins(child, bounds);

            int position = parent.getChildAdapterPosition(child);
            if (parent.getAdapter().getItemViewType(position) == EventListAdapter.ITEM_TYPE_HEADER ||
                parent.getAdapter().getItemViewType(position + 1) == EventListAdapter.ITEM_TYPE_HEADER ||
                position == state.getItemCount() - 1) {
                continue;
            }

            final int bottom = bounds.bottom + Math.round(ViewCompat.getTranslationY(child));
            final int top = bottom - divider.getIntrinsicHeight();
            divider.setBounds(left, top, right, bottom);
            divider.draw(c);
        }
        c.restore();
    }
}
