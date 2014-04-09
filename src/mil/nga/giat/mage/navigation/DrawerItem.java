package mil.nga.giat.mage.navigation;

import android.app.Fragment;

public class DrawerItem {
	private String itemText;
	private Integer drawableId;
	private Boolean isHeader = false;
	private Fragment fragment;
	
	public Fragment getFragment() {
		return fragment;
	}

	public void setFragment(Fragment fragment) {
		this.fragment = fragment;
	}

	public Boolean getIsHeader() {
		return isHeader;
	}

	public void setIsHeader(Boolean isHeader) {
		this.isHeader = isHeader;
	}

	public Boolean getShowCounter() {
		return showCounter;
	}

	public void setShowCounter(Boolean showCounter) {
		this.showCounter = showCounter;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	private Boolean showCounter = false;
	private Integer count = 0;
	
	public DrawerItem() {
	}
	
	public DrawerItem(String itemText) {
		this(itemText, null, null);
	}
	
	public DrawerItem(String itemText, Fragment fragment) {
		this(itemText, null, fragment);
	}
	
	public DrawerItem(String itemText, Integer drawableId) {
		this.itemText = itemText;
		this.drawableId = drawableId;
	}
	
	public DrawerItem(String itemText, Integer drawableId, Fragment fragment) {
		this.itemText = itemText;
		this.drawableId = drawableId;
		this.fragment = fragment;
	}

	public String getItemText() {
		return itemText;
	}

	public void setItemText(String itemText) {
		this.itemText = itemText;
	}

	public Integer getDrawableId() {
		return drawableId;
	}

	public void setDrawableId(Integer drawableId) {
		this.drawableId = drawableId;
	}
}
