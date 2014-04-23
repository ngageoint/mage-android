package mil.nga.giat.mage.filter;

import java.sql.SQLException;

import mil.nga.giat.mage.sdk.utils.Temporal;

import com.j256.ormlite.stmt.Where;

public interface Filter<T> {
    
    public  Where<? extends Temporal, Long> where(Where<? extends Temporal, Long> where) throws SQLException;
    
    public boolean passesFilter(T obj);
    
}
