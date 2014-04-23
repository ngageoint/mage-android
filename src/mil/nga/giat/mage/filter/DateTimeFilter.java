package mil.nga.giat.mage.filter;

import java.sql.SQLException;
import java.util.Date;

import mil.nga.giat.mage.sdk.utils.Temporal;

import com.j256.ormlite.stmt.Where;

public class DateTimeFilter implements Filter<Temporal> {
    Date start;
    Date end;
    
    public DateTimeFilter(Date start, Date end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public  Where<? extends Temporal, Long> where(Where<? extends Temporal, Long> where) throws SQLException {
        if (start != null && end != null) {
            where.between("last_modified", start, end);
        } else if (start != null) {
            where.ge("last_modified", start);
        } else {
            where.lt("last_modified", end);            
        }
        
        return where;
    }

    @Override
    public boolean passesFilter(Temporal t) {
        return (start == null || t.getTimestamp().after(start) && 
               (end == null || t.getTimestamp().before(end)));
    }
}