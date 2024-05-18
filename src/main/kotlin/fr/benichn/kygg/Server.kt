package fr.benichn.kygg

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import fr.benichn.kygg.Utils.openHTTPConnection
import fr.benichn.kygg.Utils.selectFirstValue
import fr.benichn.kygg.Utils.tableToMap
import fr.benichn.kygg.Utils.tableToPairs
import fr.benichn.kygg.types.*
import fr.benichn.kygg.types.adapters.ElementAdapter
import fr.benichn.kygg.types.adapters.FileTreeAdapter
import fr.benichn.kygg.types.adapters.LocalDateTimeAdapter
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.File
import java.net.URL
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.pow

class Server(
    port: Int,
    val yggUnCFHost: String,
    val yggHost: String,
    val cfCookiePath: String
) {
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .registerTypeAdapter(Element::class.java, ElementAdapter())
        .registerTypeAdapter(FileTree::class.java, FileTreeAdapter())
        .create()

    private inner class YGG {
        val sizeUnits = mapOf(
            "Zo" to 2.0.pow(70),
            "Eo" to 2.0.pow(60),
            "Po" to 2.0.pow(50),
            "To" to 2.0.pow(40),
            "Go" to 2.0.pow(30),
            "Mo" to 2.0.pow(20),
            "ko" to 2.0.pow(10),
            "o" to 2.0.pow(0),
        )

        fun String.toRelativeUrl(uncf: Boolean) =
            drop(if (uncf) yggUnCFHost.length else yggHost.length)

        fun parseSize(s: String) = sizeUnits.keys.firstNotNullOf { u -> s.split(u).let { r -> if (r.size == 2) (r[0].toDouble() * sizeUnits.getValue(u)).toLong() else null } }

        fun getCommentsHTML(id: Int, cookie: String): List<Element> {
            fun getCommentsAt(n: Int) =
                URL(yggUnCFHost + "/engine/get_ajax_torrent_comments").openHTTPConnection().apply {
                    doOutput = true
                    requestMethod = "POST"
                    setRequestProperty("Cookie", "ygg_=$cookie")
                    setRequestProperty("ContentType", "application/x-www-form-urlencoded")
                    setRequestProperty("Charset", "UTF-8")
                    outputStream.bufferedWriter().run {
                        write("type=1&torrent=$id&offset=$n")
                        flush()
                    }
                }.inputStream.bufferedReader().let { br ->
                    val json = gson.fromJson(br, JsonObject::class.java)
                    Jsoup.parse(json["html"].asString).run {
                        select("body > li")
                    }
                }
            var n = 0
            val results = mutableListOf<Elements>()
            do {
                val elements = getCommentsAt(n)
                results.add(elements)
                n += 15
            } while (elements.size == 15)
            return results.flatten()
        }

        fun login(id: String, pass: String) =
            Jsoup.connect(yggUnCFHost + "/auth/process_login")
                .data("id", id)
                .data("pass", pass)
                .post()
                .connection()
                .response()
                .cookie("ygg_")!!

        fun getTorrentInfo(url: String,  cookie: String) = Jsoup.connect(url).cookie("ygg_", cookie).get().run {
            val id = url.substringAfterLast('/').substringBefore('-').toInt()
            val sections = select("#middle section")
            val details = sections[0].select(".infos-torrent > tbody > tr").tableToMap(true)
            val nSeeders = details.getValue("Seeders").text().filter { it != ' ' }.toInt()
            val nLeechers = details.getValue("Leechers").text().filter { it != ' ' }.toInt()
            val nCompleted = details.getValue("Complétés").text().filter { it != ' ' }.toInt()
            val infos = details.getValue("Informations").select(".informations > tbody > tr").tableToMap()
            val title = infos.getValue("Nom").text()
            val category =
                infos.getValue("Catégorie").selectFirstValue("> a").attr("href").substringAfterLast('=').toInt()
            val size = parseSize(infos.getValue("Taille totale").text())
            val hash = infos.getValue("Info Hash").text()
            val uploaderTd = infos.getValue("Uploadé par")
            val uploaderName = uploaderTd.text()
            val uploaderUrl = uploaderTd.select("> a").let { `as` -> if (`as`.isEmpty()) null else `as`[0].attr("href") }
            val date = infos.getValue("Uploadé le").ownText().let { s ->
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                LocalDateTime.parse(s, formatter)
            }
            val files = URL(yggUnCFHost + "/engine/get_files?torrent=$id").openHTTPConnection().apply {
                setRequestProperty("Cookie", "ygg_=$cookie")
            }.inputStream.let { iS ->
                val json = gson.fromJson(iS.bufferedReader(), JsonObject::class.java)
                Jsoup.parse(json["html"].asString).run {
                    select("table > tbody > tr")
                        .tableToPairs().map { (s, n) ->
                            FileInfo(n.text(), parseSize(s))
                        }
                }
            }
            val nfo = URL(yggUnCFHost + "/engine/get_nfo?torrent=$id").openHTTPConnection().apply {
                setRequestProperty("Cookie", "ygg_=$cookie")
            }.inputStream.bufferedReader().use { it.readText() }
            val description = sections[1].selectFirstValue(".default")
            val comments = getCommentsHTML(id, cookie).map { li ->
                val user = li.selectFirstValue(".left").run {
                    val a = selectFirstValue("> a")
                    val userUrl = a.attr("href")
                    val avatarUrl = a.selectFirstValue(".avatar").attr("src")
                    val role = selectFirstValue(".rang").text()
                    val userName = selectFirstValue(".name").text()
                    val p = selectFirstValue(".ratio")
                    val upSize = parseSize(p.selectFirstValue(".green").text())
                    val downSize = parseSize(p.selectFirstValue(".red").text())
                    User(
                        userUrl,
                        avatarUrl,
                        userName,
                        role,
                        upSize,
                        downSize
                    )
                }
                val message = li.selectFirstValue(".message")
                val age = message.selectFirstValue("#comment_author").selectFirstValue("strong").text()
                val content = message.selectFirstValue("#comment_text")
                Comment(
                    user,
                    age,
                    content
                )
            }
            TorrentInfo(
                SearchResult(
                    category,
                    url.toRelativeUrl(true),
                    id,
                    title,
                    comments.size,
                    date,
                    size,
                    nCompleted,
                    nSeeders,
                    nLeechers
                ),
                hash,
                FileTree.fromFiles(files),
                nfo,
                uploaderName,
                uploaderUrl,
                description,
                comments
            )
        }

        fun getSearchResults(url: String, cookie: String) = Jsoup.connect(url).cookie("ygg_", cookie).get().run {
            val results = select(".results > table > tbody > tr").map { tr ->
                val tds = tr.select("td")
                val category = tds[0].selectFirstValue("span").className().substringAfterLast('_').toInt()
                val td1 = tds[1].selectFirstValue("#torrent_name")
                val torrentUrl = td1.attr("href")
                val id = torrentUrl.substringAfterLast('/').substringBefore('-').toInt()
                val title = td1.text()
                val nComments = tds[3].text().toInt()
                val date = (tds[4].selectFirstValue(".hidden").text().toInt()*1000L).let { l ->
                    LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(l),
                        TimeZone.getDefault().toZoneId()
                    )
                }
                val size = parseSize(tds[5].text())
                val nCompleted = tds[6].text().toInt()
                val nSeeders = tds[7].text().toInt()
                val nLeechers = tds[8].text().toInt()
                SearchResult(
                    category,
                    torrentUrl.toRelativeUrl(false),
                    id,
                    title,
                    nComments,
                    date,
                    size,
                    nCompleted,
                    nSeeders,
                    nLeechers
                )
            }
            val paginations = select(".pagination")
            val isEndOfSearch = paginations.isEmpty() || paginations[0].select("li").all { li ->
                li.text() != "suivante →"
            }
            SearchResults(
                results,
                isEndOfSearch
            )
        }

        fun getLoggedUser(cookie: String) = Jsoup.connect(yggUnCFHost).cookie("ygg_", cookie).get().run {
            val lis = select("#top_panel > .ct > ul > li")
            val upSize = parseSize(lis[0].child(0).text())
            val downSize = parseSize(lis[0].child(1).text())
            val ratio = lis[1].text().substringAfterLast(':').toFloat()
            val isActive = when(lis[2].text().trim()) {
                "Compte Actif" -> true
                "Compte Désactivé" -> false
                else -> throw IllegalStateException()
            }
            LoggedUser(
                upSize,
                downSize,
                ratio,
                isActive
            )
        }

        fun getTorrentInputStream(id: Int, cookie: String) =
            URL(yggUnCFHost + "/engine/download_torrent?id=$id").openHTTPConnection().apply {
                setRequestProperty("Cookie", "ygg_=$cookie")
            }.inputStream
    }

    private val ygg = YGG()

    val app = embeddedServer(Netty, port) {
        routing {
            get("/host") {
                call.respondText(yggUnCFHost, ContentType.Text.Plain)
            }
            get("/user") {
                call.request.queryParameters.let { params ->
                    val cookie = ygg.login(params.getOrFail("username"), params.getOrFail("password"))
                    val user = ygg.getLoggedUser(cookie)
                    call.respondText(gson.toJson(user), ContentType.Application.Json)
                }
            }
            post("/cf") {
                val cookie = call.receiveText()
                File(cfCookiePath).writeText(cookie)
                call.respondText("cookie changed to $cookie")
            }
            get("/dl") {
                val params = call.request.queryParameters
                val cookie = ygg.login(params.getOrFail("username"), params.getOrFail("password"))
                val id = params.getOrFail("id")
                val iS = ygg.getTorrentInputStream(id.toInt(), cookie)
                call.response.headers.append(HttpHeaders.ContentDisposition, "inline; filename=\"$id.torrent\"")
                call.respondOutputStream(contentType = ContentType("application", "x-bittorrent")) {
                    iS.transferTo(this)
                }
            }
            get("/search") {
                val params = call.request.queryParameters
                val cookie = ygg.login(params.getOrFail("username"), params.getOrFail("password"))
                val results =
                    ygg.getSearchResults(yggUnCFHost + "/engine/search?" + call.request.queryParameters.formUrlEncode(), cookie)
                call.respondText(gson.toJson(results), ContentType.Application.Json)
            }
            get("/torrent{...}") {
                val params = call.request.queryParameters
                val cookie = ygg.login(params.getOrFail("username"), params.getOrFail("password"))
                val url = yggUnCFHost + call.request.uri.substringBefore('?')
                try {
                    val torrent = ygg.getTorrentInfo(url, cookie)
                    call.respondText(gson.toJson(torrent), ContentType.Application.Json)
                } catch (e: Exception) {
                    println(e.toString())
                }
            }
        }
    }
}