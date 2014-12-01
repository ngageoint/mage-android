package mil.nga.giat.mage.sdk.datastore;

import android.content.Context;

/**
 * Abstract class that Helpers should extend
 * 
 * @author wiedemannse
 *
 */
public abstract class DaoHelper<T> implements IDaoHelper<T> {
	protected final DaoStore daoStore;
	
	protected DaoHelper(Context pContext) {
		daoStore = DaoStore.getInstance(pContext);
	}
}
