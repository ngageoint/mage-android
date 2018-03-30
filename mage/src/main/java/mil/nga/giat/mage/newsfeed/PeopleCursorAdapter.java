package mil.nga.giat.mage.newsfeed;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.stmt.PreparedQuery;

import org.apache.commons.lang3.StringUtils;
import org.ocpsoft.prettytime.PrettyTime;

import java.sql.SQLException;
import java.util.Collection;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.Team;
import mil.nga.giat.mage.sdk.datastore.user.TeamHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserLocal;

public class PeopleCursorAdapter extends CursorAdapter {
	private static final String LOG_NAME = PeopleCursorAdapter.class.getName();
	
	private LayoutInflater inflater = null;
	private PreparedQuery<Location> query;
	private TeamHelper teamHelper;
	Collection<Team> eventTeams;

	public PeopleCursorAdapter(Context context, Cursor c, PreparedQuery<Location> query) {
		super(context, c, false);

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

			ImageView personImageView = (ImageView) v.findViewById(R.id.avatarImageView);
			Drawable defaultPersonIcon = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_person_white_24dp));
			DrawableCompat.setTint(defaultPersonIcon, ContextCompat.getColor(context, R.color.icon));
			DrawableCompat.setTintMode(defaultPersonIcon, PorterDuff.Mode.SRC_ATOP);
			personImageView.setImageDrawable(defaultPersonIcon);

			final ImageView avatarView = (ImageView) v.findViewById(R.id.avatarImageView);
			UserLocal userLocal = user.getUserLocal();
			Glide.with(context)
					.load(userLocal.getLocalAvatarPath())
					.asBitmap()
					.fallback(defaultPersonIcon)
					.error(defaultPersonIcon)
					.centerCrop()
					.into(new BitmapImageViewTarget(avatarView) {
						@Override
						protected void setResource(Bitmap resource) {
							RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), resource);
							circularBitmapDrawable.setCircular(true);
							avatarView.setImageDrawable(circularBitmapDrawable);
						}
					});

			final ImageView iconView = (ImageView) v.findViewById(R.id.iconImageView);
			Glide.with(context)
					.load(userLocal.getLocalIconPath())
					.centerCrop()
					.into(iconView);

			TextView name = (TextView) v.findViewById(R.id.name);
			name.setText(user.getDisplayName());

			TextView date = (TextView) v.findViewById(R.id.date);
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

			TextView teamsView = (TextView) v.findViewById(R.id.teams);
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
