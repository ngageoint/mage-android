package mil.nga.giat.mage.newsfeed;

import android.content.Context;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.stmt.PreparedQuery;

import org.apache.commons.lang3.StringUtils;
import org.ocpsoft.prettytime.PrettyTime;

import java.sql.SQLException;
import java.util.Collection;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.glide.GlideApp;
import mil.nga.giat.mage.glide.model.Avatar;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.Team;
import mil.nga.giat.mage.sdk.datastore.user.TeamHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;

public class PeopleCursorAdapter extends CursorAdapter {
	private static final String LOG_NAME = PeopleCursorAdapter.class.getName();
	
	private LayoutInflater inflater = null;
	private PreparedQuery<Location> query;
	private TeamHelper teamHelper;
	Collection<Team> eventTeams;

	public PeopleCursorAdapter(Context context, Cursor cursor, PreparedQuery<Location> query) {
		super(context, cursor, false);

		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.query = query;

		teamHelper = TeamHelper.getInstance(context);
		eventTeams = teamHelper.getTeamsByEvent(EventHelper.getInstance(context).getCurrentEvent());
	}

	@Override
	public void bindView(View v, final Context context, Cursor cursor) {
		try {
			Location location = query.mapRow(new AndroidDatabaseResults(cursor, null, false));
			User user = location.getUser();
			if (user == null) {
				return;
			}

			ImageView avatarView = v.findViewById(R.id.avatarImageView);
			Drawable defaultPersonIcon = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_person_white_24dp));
			DrawableCompat.setTint(defaultPersonIcon, ContextCompat.getColor(context, R.color.icon));
			DrawableCompat.setTintMode(defaultPersonIcon, PorterDuff.Mode.SRC_ATOP);
			avatarView.setImageDrawable(defaultPersonIcon);

			GlideApp.with(context)
					.load(Avatar.Companion.forUser(user))
					.fallback(defaultPersonIcon)
					.error(defaultPersonIcon)
					.circleCrop()
					.into(avatarView);

			final ImageView iconView = v.findViewById(R.id.iconImageView);
			GlideApp.with(context)
					.load(user.getUserLocal().getLocalIconPath())
					.centerCrop()
					.into(iconView);

			TextView name = v.findViewById(R.id.name);
			name.setText(user.getDisplayName());

			TextView date = v.findViewById(R.id.date);
			String timeText = new PrettyTime().format(location.getTimestamp());
			date.setText(timeText);

			Collection<Team> userTeams = teamHelper.getTeamsByUser(user);
			userTeams.retainAll(eventTeams);
			Collection<String> teamNames = Collections2.transform(userTeams, new Function<Team, String>() {
				@Override
				public String apply(Team team) {
					return team.getName();
				}
			});

			TextView teamsView = v.findViewById(R.id.teams);
			teamsView.setText(StringUtils.join(teamNames, ", "));

		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Could not set location view information.", sqle);
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parentView) {
		return LayoutInflater.from(parentView.getContext()).inflate(R.layout.people_list_item, parentView, false);
	}
}
