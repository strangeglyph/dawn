
import kotlinx.html.dom.append
import kotlinx.html.p
import org.w3c.dom.HTMLDivElement
import kotlin.browser.document


object Messages {
    private val MESSAGES_DIV = document.getElementById("messages") as HTMLDivElement


    fun append(msg: String) {
        MESSAGES_DIV.append.p {
            +msg
        }
    }
}