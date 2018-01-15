package mil.nga.giat.mage.filter;

import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;

import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.Event;

public class LocationEventFilter implements Filter<Location> {

	private Long eventId;

	public LocationEventFilter(Event event) {
		this.eventId = event.getId();
	}

	@Override
	public QueryBuilder<Event, Long> query() throws SQLException {
		return null;
	}

	@Override
	public void and(Where<? extends Location, Long> where) throws SQLException {

	}

	@Override
	public boolean passesFilter(Location location) {
		return eventId.equals(location.getEvent().getId());
	}
}