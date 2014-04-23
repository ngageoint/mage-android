package mil.nga.giat.mage.filter;

import java.sql.SQLException;
import java.util.Date;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;

import com.j256.ormlite.stmt.Where;

public class ObservationDateTimeFilter implements Filter<Observation> {
    Date start;
    Date end;
    
    public ObservationDateTimeFilter(Date start, Date end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public  Where<Observation, Long> where(Where<Observation, Long> where) throws SQLException {
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
    public boolean passesFilter(Observation observation) {
        return (start == null || observation.getLastModified().after(start) && 
               (end == null || observation.getLastModified().before(end)));
    }
}