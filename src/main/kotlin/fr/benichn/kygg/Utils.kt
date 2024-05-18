package fr.benichn.kygg

import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.net.HttpURLConnection
import java.net.URL
import java.util.NoSuchElementException
import kotlin.math.max

object Utils {
    fun URL.openHTTPConnection() = openConnection() as HttpURLConnection

    fun Element.selectFirstValue(cssQuery: String) =
        selectFirst(cssQuery) ?: throw NoSuchElementException(cssQuery)

    fun <T> List<T>.with(index: Int, element: T): List<T> = (0 .. max(index, size-1)).map { i ->
        if (i == index) element
        else this[i]
    }

    fun <T> List<T>.with(index: Int, f: (T) -> T): List<T> = (0 .. max(index, size-1)).map { i ->
        if (i == index) f(this[i])
        else this[i]
    }

    fun Elements.tableToMap(chunked: Boolean = false) =
        tableToPairs(chunked).toMap()
    fun Elements.tableToPairs(chunked: Boolean = false) =
        (if (chunked) flatMap { tr ->
            tr.select("> td").chunked(2)
        } else map { tr -> tr.select("> td") }).map { tds ->
            tds[0].text() to tds[1]
        }
}