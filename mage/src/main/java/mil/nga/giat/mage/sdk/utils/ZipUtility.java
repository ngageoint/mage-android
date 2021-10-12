package mil.nga.giat.mage.sdk.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *
 * Zip and unzips files
 *
 * @author wiedemanns
 *
 */
public class ZipUtility {

	public static void unzip(File zip, File extractTo) throws IOException {
		ZipFile archive = new ZipFile(zip);
		Enumeration<? extends ZipEntry> e = archive.entries();
		while (e.hasMoreElements()) {
			ZipEntry entry = e.nextElement();
			String zipCanonicalPath = validateZipEntry(entry.getName(), extractTo);
			File file = new File(zipCanonicalPath);

			if (file.exists()) {
				deleteDirectoryOrFile(file);
			}

			if (entry.isDirectory() && !file.exists()) {
				file.mkdirs();
			} else {
				if (!file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}
				if (!file.exists()) {
					file.createNewFile();
				}

				InputStream in = archive.getInputStream(entry);
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));

				byte[] buffer = new byte[8192];
				int read;

				while (-1 != (read = in.read(buffer))) {
					out.write(buffer, 0, read);
				}

				in.close();
				out.close();
			}
		}

		archive.close();
	}

	private static String validateZipEntry(String zipEntryRelativePath, File destDir) throws IOException {
		File zipEntryTarget = new File(destDir, zipEntryRelativePath);
		String zipCanonicalPath = zipEntryTarget.getCanonicalPath();

		if (zipCanonicalPath.startsWith(destDir.getCanonicalPath())) {
			return(zipCanonicalPath);
		}

		throw new IllegalStateException("ZIP entry tried to write outside destination directory");
	}

	private static void deleteDirectoryOrFile(File file) {
		if (file.exists()) {
			if (file.isDirectory()) {
				File[] files = file.listFiles();

				for (File child : files) {
                    if (child.isDirectory()) {
                        deleteDirectoryOrFile(child);
                    } else {
                        child.delete();
                    }
                }
			}

			file.delete();
		}
	}

}