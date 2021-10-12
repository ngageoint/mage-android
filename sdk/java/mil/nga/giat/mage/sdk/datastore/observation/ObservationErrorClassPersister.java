package mil.nga.giat.mage.sdk.datastore.observation;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.StringType;

import org.json.JSONException;
import org.json.JSONObject;

public class ObservationErrorClassPersister extends StringType {

	private static String ERROR_STATUS_CODE_KEY = "ERROR_STATUS_CODE_KEY";
	private static String ERROR_MESSAGE_KEY = "ERROR_MESSAGE_KEY";
	private static String ERROR_DESCRIPTION_KEY = "ERROR_DESCRIPTION_KEY";

	private static final ObservationErrorClassPersister INSTANCE = new ObservationErrorClassPersister();

	private ObservationErrorClassPersister() {
		super(SqlType.STRING, new Class<?>[] { ObservationErrorClassPersister.class });
	}

	public static ObservationErrorClassPersister getSingleton() {
		return INSTANCE;
	}

	@Override
	public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
		ObservationError error = (ObservationError) javaObject;
		return error != null ? jsonFromError(error) : null;
	}

	@Override
	public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
		return sqlArg != null ? errorFromJson((String) sqlArg) : null;
	}

	private String jsonFromError(ObservationError error) {
		if (error == null) {
			return null;
		}

		JSONObject jsonObject = new JSONObject();
		try {
			Integer statusCode = error.getStatusCode();
			if (statusCode != null) {
				jsonObject.put(ERROR_STATUS_CODE_KEY, error.getStatusCode());
			}

			String message = error.getMessage();
			if (message != null) {
				jsonObject.put(ERROR_MESSAGE_KEY, error.getMessage());
			}

			String description = error.getDescription();
			if (description != null) {
				jsonObject.put(ERROR_DESCRIPTION_KEY, error.getDescription());
			}
		} catch (JSONException e) {
		}

		return jsonObject.toString();
	}

	private ObservationError errorFromJson(String errorJson) {
		ObservationError observationError = new ObservationError();
		try {
			JSONObject jsonObject = new JSONObject(errorJson);

			if (jsonObject.has(ERROR_STATUS_CODE_KEY)) {
				observationError.setStatusCode(jsonObject.getInt(ERROR_STATUS_CODE_KEY));
			}

			if (jsonObject.has(ERROR_MESSAGE_KEY)) {
				observationError.setMessage(jsonObject.getString(ERROR_MESSAGE_KEY));
			}

			if (jsonObject.has(ERROR_DESCRIPTION_KEY)) {
				observationError.setDescription(jsonObject.getString(ERROR_DESCRIPTION_KEY));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return observationError;
	}

}
