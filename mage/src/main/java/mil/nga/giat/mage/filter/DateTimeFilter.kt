package mil.nga.giat.mage.filter

import com.j256.ormlite.stmt.QueryBuilder
import com.j256.ormlite.stmt.Where
import mil.nga.giat.mage.sdk.Temporal
import java.sql.SQLException
import java.util.*

class DateTimeFilter(
   private val start: Date?,
   private val end: Date?,
   private val columnName: String
) : Filter<Temporal> {

   override fun query(): QueryBuilder<out Temporal, Long>? {
      return null
   }

   @Throws(SQLException::class)
   override fun and(where: Where<*, Long>) {
      where.and()

      if (start != null && end != null) {
         where.between(columnName, start, end)
      } else if (start != null) {
         where.ge(columnName, start)
      } else {
         where.lt(columnName, end)
      }
   }

   override fun passesFilter(temporal: Temporal): Boolean {
      return (start == null || temporal.timestamp.after(start)) &&
              (end == null || temporal.timestamp.before(end))
   }
}