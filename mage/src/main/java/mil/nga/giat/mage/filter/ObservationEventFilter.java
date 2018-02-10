package mil.nga.giat.mage.filter;

import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.user.Event;

public class ObservationEventFilter implements Filter<Observation> {

	private Long eventId;

	public ObservationEventFilter(Event event) {
		if (event != null) {
			this.eventId = event.getId();
		}
	}

	@Override
	public QueryBuilder<Event, Long> query() throws SQLException {
		return null;
	}

	@Override
	public void and(Where<? extends Observation, Long> where) throws SQLException {

	}

	@Override
	public boolean passesFilter(Observation observation) {
		boolean passes = false;

		Event observationEvent = observation.getEvent();
		if (observationEvent != null) {
			passes = observationEvent.getId().equals(eventId);
		}

		return passes;
	}
}