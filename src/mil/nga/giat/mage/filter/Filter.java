package mil.nga.giat.mage.filter;

import java.sql.SQLException;

import com.j256.ormlite.stmt.Where;

public interface Filter<T> {
    
    public  Where<T, Long> where(Where<T, Long> where) throws SQLException;
    
    public boolean passesFilter(T obj);
    
}
