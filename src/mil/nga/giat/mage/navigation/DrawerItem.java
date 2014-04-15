package mil.nga.giat.mage.navigation;

import android.app.Fragment;

public class DrawerItem {
	private String itemText;
	private Integer drawableId;
	private Boolean isHeader = false;
	private Fragment fragment;
	private int id;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Fragment getFragment() {
		return fragment;
	}

	public void setFragment(Fragment fragment) {
		this.fragment = fragment;
	}

	public Boolean isHeader() {
		return isHeader;
	}

	public void isHeader(Boolean isHeader) {
		this.isHeader = isHeader;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	private Integer count = 0;
	
	public DrawerItem(int id) {
		this.id = id;
	}
	
	public DrawerItem(int id, String itemText) {
		this(id, itemText, null, null);
	}
	
	public DrawerItem(int id, String itemText, Fragment fragment) {
		this(id, itemText, null, fragment);
	}
	
	public DrawerItem(int id, String itemText, Integer drawableId) {
		this.itemText = itemText;
		this.drawableId = drawableId;
		this.id = id;
	}
	
	public DrawerItem(int id, String itemText, Integer drawableId, Fragment fragment) {
		this.itemText = itemText;
		this.drawableId = drawableId;
		this.fragment = fragment;
		this.id = id;
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
