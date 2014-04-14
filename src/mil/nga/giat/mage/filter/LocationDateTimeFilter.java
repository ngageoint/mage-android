package mil.nga.giat.mage.filter;

import java.util.Date;

import mil.nga.giat.mage.sdk.datastore.location.Location;

public class LocationDateTimeFilter implements Filter<Location> {
    Date start;
    Date end;
    
    public LocationDateTimeFilter(Date start, Date end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean passesFilter(Location location) {
        Date timestamp = new Date(location.getLastModified());
        return (start == null || timestamp.after(start) && 
               (end == null || timestamp.before(end)));
    }
}