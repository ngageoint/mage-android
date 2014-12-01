package mil.nga.giat.mage.sdk.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtility {

	public static final void zipDirectory(File directory, File zip) throws IOException {
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip));
		zip(directory, directory, zos);
		zos.close();
	}

	private static final void zip(File directory, File base, ZipOutputStream zos) throws IOException {
		File[] files = directory.listFiles();
		byte[] buffer = new byte[8192];
		int read = 0;
		for (int i = 0, n = files.length; i < n; i++) {
			if (files[i].isDirectory()) {
				zip(files[i], base, zos);
				// files[i].delete();
			} else {
				FileInputStream in = new FileInputStream(files[i]);
				ZipEntry entry = new ZipEntry(files[i].getPath().substring(base.getPath().length() + 1));
				zos.putNextEntry(entry);
				while (-1 != (read = in.read(buffer))) {
					zos.write(buffer, 0, read);
				}
				in.close();
			}
		}
	}

	public static final void unzip(File zip, File extractTo) throws IOException {
		ZipFile archive = new ZipFile(zip);
		Enumeration<ZipEntry> e = (Enumeration<ZipEntry>) archive.entries();
		while (e.hasMoreElements()) {
			ZipEntry entry = e.nextElement();
			File file = new File(extractTo, entry.getName());

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
	}

	public static void deleteDirectoryOrFile(File file) {
		if (file.exists()) {
			if (file.isDirectory()) {
				File[] files = file.listFiles();
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory()) {
						deleteDirectoryOrFile(files[i]);
					} else {
						files[i].delete();
					}
				}
			}
			file.delete();
		}
	}

}