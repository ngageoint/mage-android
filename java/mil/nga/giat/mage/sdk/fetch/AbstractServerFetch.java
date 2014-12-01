package mil.nga.giat.mage.sdk.fetch;

import android.content.Context;

public abstract class AbstractServerFetch {

	protected Context mContext;
	
	public AbstractServerFetch(Context context) {
		mContext = context;
	}
}
