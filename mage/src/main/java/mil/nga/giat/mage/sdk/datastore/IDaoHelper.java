package mil.nga.giat.mage.sdk.datastore;

public interface IDaoHelper<T> {

	T create(T pDao) throws Exception;

	T read(Long id) throws Exception;

	T read(String remoteId) throws Exception;

	// TODO : readAll

	T update(T pDao) throws Exception;

	// TODO : delete

}
