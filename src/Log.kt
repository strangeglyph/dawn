
import kotlinx.html.classes
import kotlinx.html.dom.prepend
import kotlinx.html.js.p
import org.w3c.dom.HTMLDivElement
import kotlin.dom.addClass

object Log: Tab(divName = "log", displayName = "Log", order = 1000) {

    fun newEntry(message: String) {
        MAIN_DIV.prepend.p {
            classes = setOf("log-element")
            +message
        }
    }

    fun newEntry(div: HTMLDivElement) {
        div.addClass("log-element")
        MAIN_DIV.prepend(div)
    }
}