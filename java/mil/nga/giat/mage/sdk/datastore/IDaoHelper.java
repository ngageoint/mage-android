package mil.nga.giat.mage.sdk.datastore;

public interface IDaoHelper<T> {

	public T create(T pDao) throws Exception;

	public T read(Long id) throws Exception;

	public T read(String remoteId) throws Exception;

	// TODO : readAll

	public T update(T pDao) throws Exception;

	// TODO : delete

}
