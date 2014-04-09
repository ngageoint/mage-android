package mil.nga.giat.mage.navigation;

public class DrawerItem {
	private String itemText;
	private Integer drawableId;
	private Boolean isHeader = false;
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
		this(itemText, null);
	}
	
	public DrawerItem(String itemText, Integer drawableId) {
		this.itemText = itemText;
		this.drawableId = drawableId;
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
