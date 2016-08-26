package mil.nga.giat.mage.filter;

import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;
import java.util.Date;

import mil.nga.giat.mage.sdk.Temporal;

public class DateTimeFilter implements Filter<Temporal> {
	private Date start;
	private Date end;
	private String columnName;

	public DateTimeFilter(Date start, Date end, String columnName) {
		this.start = start;
		this.end = end;
		this.columnName = columnName;
	}

	@Override
	public QueryBuilder<? extends Temporal, Long> query() {
		return null;
	}

	@Override
	public void and(Where<? extends Temporal, Long> where) throws SQLException {
		where.and();

		if (start != null && end != null) {
			where.between(columnName, start, end);
		} else if (start != null) {
			where.ge(columnName, start);
		} else {
			where.lt(columnName, end);
		}
	}

	@Override
	public boolean passesFilter(Temporal t) {
		return (start == null || t.getTimestamp().after(start) && (end == null || t.getTimestamp().before(end)));
	}
}