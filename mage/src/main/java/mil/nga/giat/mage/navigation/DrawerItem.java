package mil.nga.giat.mage.navigation;

import android.app.Fragment;

public class DrawerItem {
    private int id;
    private String text;
    private Integer drawableId;
    private Fragment fragment;
    private boolean isHeader;
    private boolean isSecondary;
    private Integer count = 0;

    public String getText() {
        return text;
    }

    public Integer getDrawableId() {
        return drawableId;
    }

    public Boolean isHeader() {
        return isHeader;
    }
    
    public Boolean isSecondary() {
        return isSecondary;
    }

    public Fragment getFragment() {
        return fragment;
    }

    public int getId() {
        return id;
    }

    public Integer getCount() {
        return count;
    }

    private DrawerItem(Builder builder) {
        this.id = builder.id;
        this.text = builder.text;
        this.isHeader = builder.isHeader;
        this.isSecondary = builder.isSecondary;
        this.drawableId = builder.drawableId;
        this.fragment = builder.fragment;
    }

    public static class Builder {
        private int id;
        private String text;
        private boolean isHeader = false;
        private boolean isSecondary = false;
        private Integer drawableId;
        private Fragment fragment;

        public Builder(String text) {
            this.text = text;
        }

        public Builder id(int id) {
            this.id = id;
            return this;
        }
     
        public Builder header(boolean isHeader) {
            this.isHeader = isHeader;
            return this;
        }
        
        public Builder secondary(boolean isSecondary) {
            this.isSecondary = isSecondary;
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