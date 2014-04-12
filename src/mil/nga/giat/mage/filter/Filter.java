package mil.nga.giat.mage.filter;

public interface Filter<T> {
        
    public boolean passesFilter(T item);
}
