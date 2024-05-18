package fr.benichn.kygg.types.adapters

import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.jsoup.nodes.Element
import java.lang.reflect.Type

class ElementAdapter : JsonSerializer<Element> {
    override fun serialize(src: Element, typeOfSrc: Type, context: JsonSerializationContext) =
        JsonPrimitive(src.toString())
}