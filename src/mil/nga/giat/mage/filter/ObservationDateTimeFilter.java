package mil.nga.giat.mage.filter;

import java.util.Date;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;

public class ObservationDateTimeFilter implements Filter<Observation> {
    Date start;
    Date end;
    
    public ObservationDateTimeFilter(Date start, Date end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean passesFilter(Observation observation) {
        return (start == null || observation.getLastModified().after(start) && 
               (end == null || observation.getLastModified().before(end)));
    }
}