package mil.nga.giat.mage.coordinate;

public enum CoordinateSystem {
    WGS84(0),
    MGRS(1);

    private int preferenceValue;

    CoordinateSystem(int preferenceValue) {
        this.preferenceValue = preferenceValue;
    }

    public int getPreferenceValue() {
        return preferenceValue;
    }

    public static CoordinateSystem get(int preferenceValue) {
        for(CoordinateSystem coordinateSystem : values()) {
            if(coordinateSystem.preferenceValue == preferenceValue) {
                return coordinateSystem;
            }
        }

        return null;
    }
}
