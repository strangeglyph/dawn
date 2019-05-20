
import org.w3c.dom.HTMLDivElement
import kotlin.browser.document


object Messages {
    private val MESSAGES_DIV = document.getElementById("messages") as HTMLDivElement

    fun append(msg: String) {
        MESSAGES_DIV.innerText = msg
        Log.newEntry(msg)
    }
}