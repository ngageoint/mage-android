package mil.nga.giat.mage.navigation;

import android.app.Fragment;

public class DrawerItem {
    private int id;
    private String text;
    private Integer drawableId;
    private Fragment fragment;
    private boolean isSeperator;

    public String getText() {
        return text;
    }

    public Integer getDrawableId() {
        return drawableId;
    }

    public Boolean isSeperator() {
        return isSeperator;
    }

    public Fragment getFragment() {
        return fragment;
    }

    public int getId() {
        return id;
    }

    private DrawerItem(Builder builder) {
        this.id = builder.id;
        this.text = builder.text;
        this.isSeperator = builder.isSeperator;
        this.drawableId = builder.drawableId;
        this.fragment = builder.fragment;
    }

    public static class Builder {
        private int id;
        private String text;
        private boolean isSeperator = false;
        private Integer drawableId;
        private Fragment fragment;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }
        
        public Builder seperator(boolean isSeperator) {
            this.isSeperator = isSeperator;
            return this;
        }
        
        public Builder drawableId(int drawableId) {
            this.drawableId = drawableId;
            return this;
        }
        
        public Builder fragment(Fragment fragment) {
            this.fragment = fragment;
            return this;
        }

        public DrawerItem build() {
            return new DrawerItem(this);
        }
    }
}