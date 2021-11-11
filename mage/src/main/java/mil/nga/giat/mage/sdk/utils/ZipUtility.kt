package mil.nga.giat.mage.sdk.utils

import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 *
 * Zip and unzips files
 *
 */
object ZipUtility {

   @Throws(IOException::class)
   fun unzip(source: File, directory: File) {
      ZipFile(source).use { zip ->
         zip.entries().asSequence().forEach { entry ->
            if (validateEntry(entry, directory)) {
               extractEntry(zip, entry, directory)
            }
         }
      }
   }

   private fun validateEntry(entry: ZipEntry, root: File): Boolean {
      val destination = File(root, entry.name)
      return destination.canonicalPath.startsWith(root.canonicalPath + File.separator)
   }

   private fun extractEntry(zipFile: ZipFile, entry: ZipEntry, directory: File)  {
      zipFile.getInputStream(entry).use { inputStream ->
         val destination = File(directory, entry.name)

         if (destination.exists()) {
            destination.deleteRecursively()
         }

         if (entry.isDirectory) {
            destination.mkdirs()
         } else {
            if (destination.parentFile?.exists() != true) {
               destination.parentFile?.mkdirs()
            }

            if (!destination.exists()) {
               destination.createNewFile()
            }

            destination.outputStream().use { outputStream ->
               inputStream.copyTo(outputStream)
            }
         }
      }
   }
}