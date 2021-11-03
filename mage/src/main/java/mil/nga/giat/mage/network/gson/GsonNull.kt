package mil.nga.giat.mage.network.gson

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken

fun JsonReader.nextStringOrNull(): String? {
   return if (peek() == JsonToken.NULL) {
      nextNull()
      null
   } else {
      nextString()
   }
}

fun JsonReader.nextLongOrNull(): Long? {
   return if (peek() == JsonToken.NULL) {
      nextNull()
      null
   } else {
      nextLong()
   }
}

fun JsonReader.nextDoubleOrNull(): Double? {
   return if (peek() == JsonToken.NULL) {
      nextNull()
      null
   } else {
      nextDouble()
   }
}

fun JsonReader.nextBooleanOrNull(): Boolean? {
   return if (peek() == JsonToken.NULL) {
      nextNull()
      null
   } else {
      nextBoolean()
   }
}

fun JsonReader.nextNumberOrNull(): Number? {
   return if (peek() == JsonToken.NULL) {
      nextNull()
      null
   } else {
      val value = nextString()
      try {
         value.toLong()
      } catch (e: NumberFormatException) {
         try {
            val asDouble = value.toDouble()
            if ((asDouble.isInfinite() || asDouble.isNaN()) && !isLenient) {
               null
            } else asDouble
         } catch(e: NumberFormatException) {
            null
         }
      }
   }
}