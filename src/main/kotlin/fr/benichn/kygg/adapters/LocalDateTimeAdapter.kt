package fr.benichn.kygg.types.adapters

import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.util.*

class LocalDateTimeAdapter : JsonSerializer<LocalDateTime> {
    override fun serialize(src: LocalDateTime, typeOfSrc: Type, context: JsonSerializationContext) =
        JsonPrimitive(src.atZone(TimeZone.getDefault().toZoneId()).toInstant().toEpochMilli())
}