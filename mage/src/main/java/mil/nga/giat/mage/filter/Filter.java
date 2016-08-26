package mil.nga.giat.mage.filter;

import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;

public interface Filter<T> {

    public QueryBuilder<?, Long> query() throws SQLException;
    public void and(Where<? extends T, Long> where) throws SQLException;

    public boolean passesFilter(T obj);
    
}
