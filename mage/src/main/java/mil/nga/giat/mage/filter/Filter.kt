package mil.nga.giat.mage.filter

import com.j256.ormlite.stmt.QueryBuilder
import com.j256.ormlite.stmt.Where
import java.sql.SQLException

interface Filter<in T> {
   @Throws(SQLException::class)
   fun query(): QueryBuilder<*, Long>?

   @Throws(SQLException::class)
   fun and(where: Where<*, Long>)

   fun passesFilter(obj: T): Boolean
}