package fr.benichn.kygg

fun main(args: Array<String>) {
    val yggUnCFHost = System.getProperty("ygg_uncf_host") ?: throw IllegalStateException("""System.getProperty("ygg_uncf_host") returned null, cannot continue.""")
    val yggHost = System.getProperty("ygg_host") ?: throw IllegalStateException("""System.getProperty("ygg_host") returned null, cannot continue.""")
    val cfCookiePath = System.getProperty("cf_cookie_path") ?: throw IllegalStateException("""System.getProperty("cf_cookie_path") returned null, cannot continue.""")
    val port = System.getProperty("port") ?: throw IllegalStateException("""System.getProperty("port") returned null, cannot continue.""")
    Server(port.toInt(), yggUnCFHost, yggHost, cfCookiePath).apply {
        println("kYgg is now listening to port $port")
        app.start(true)
    }
}