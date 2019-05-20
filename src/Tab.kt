
import kotlinx.html.button
import kotlinx.html.classes
import kotlinx.html.dom.append
import kotlinx.html.id
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.get
import kotlin.browser.document

object LocationIndicator {
    private val DIV = document.getElementById("location-indicator") as HTMLDivElement
    private val SPAN = document.getElementById("location-indicator-text") as HTMLSpanElement

    fun show() {
        DIV.style.display = "block"
    }

    fun set(name: String) {
        SPAN.innerText = name
    }
}

open class Tab(val divName: String, val displayName: String, val order: Int) {
    private val MAIN_DIV = document.getElementById(divName) as HTMLDivElement
    private val INTERACTIONS = MAIN_DIV.getElementsByClassName("interactions")[0] as HTMLDivElement
    private val DESCRIPTION = MAIN_DIV.getElementsByClassName("description")[0] as HTMLDivElement

    private var enabled = false

    fun show() {
        val tabs = document.getElementsByClassName("tab")
        repeat(tabs.length) { i -> (tabs[i] as HTMLDivElement).style.display = "none" }
        MAIN_DIV.style.display = "block"
        LocationIndicator.set(displayName)
    }

    fun addSingleUseInteraction(tag: String, desc: String, action: (MouseEvent) -> dynamic) {
        INTERACTIONS.append {
            button {
                +desc
                id = tag
                classes = setOf("interaction")
            }.onclick = { e -> action(e); removeInteraction(tag) }
        }
    }

    fun addInteraction(tag: String, desc: String, action: (MouseEvent) -> dynamic) {
        INTERACTIONS.append {
            button {
                +desc
                id = tag
                classes = setOf("interaction")
            }.onclick = action
        }
    }

    fun removeInteraction(tag: String) {
        document.getElementById(tag)!!.remove()
    }

    fun setDescription(text: String) {
        DESCRIPTION.innerText = text
    }

    open fun enable() {
        if (!enabled) {
            enabled = true
            val tabSwitcher = document.getElementById("tab-switcher") as HTMLDivElement
            tabSwitcher.append {
                button {
                    +displayName
                    id = "go-to-$divName"
                    classes = setOf("go-to-button")
                    onClickFunction = { show() }
                }.style.order = order.toString()
            }
        }
    }
}