package fr.benichn.kygg.types.adapters

import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import fr.benichn.kygg.types.FileTree
import java.lang.reflect.Type

class FileTreeAdapter : JsonSerializer<FileTree> {
    override fun serialize(src: FileTree, typeOfSrc: Type, context: JsonSerializationContext) =
        context.serialize(src).asJsonObject.apply {
            if (src is FileTree.Directory) addProperty("size", src.size)
        }
}