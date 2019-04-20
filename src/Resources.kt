import kotlinx.html.classes
import kotlinx.html.dom.create
import kotlinx.html.img
import kotlinx.html.js.div
import kotlinx.html.span
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.get
import kotlin.browser.document

object Resources {
    private val DIV = document.getElementById("resources-div") as HTMLDivElement

    private val resourceCounter: Array<Int> = Resource.values().map { it.initialAmount }.toTypedArray()
    private val isResourceVisible: Array<Boolean> = Array(Resource.values().size) { false }

    private val resoureDivs: Array<HTMLDivElement> = Resource.values().map { res ->
        val div = document.create.div {
            classes = setOf("resource-line")
            img {
                classes = setOf("resource-icon")
                alt = res.displayName
                src = res.iconPath
            }
            span {
                classes = setOf("resource-name")
                +res.displayName
            }
            span {
                classes = setOf("resource-counter")
                +"0"
            }
        }
        div.style.order = res.order.toString()
        div
    }.toTypedArray()

    private val resourceCounterSpans: Array<HTMLSpanElement> = resoureDivs.map {
        it.getElementsByClassName("resource-counter")[0] as HTMLSpanElement
    }.toTypedArray()


    fun add(resource: Resource, amount: Int) {
        Messages.append("You gained $amount ${resource.displayName}.")
        val index = resource.ordinal
        resourceCounter[index] += amount
        resourceCounterSpans[index].innerText = resourceCounter[index].toString()

        if (!isResourceVisible[index]) {
            DIV.appendChild(resoureDivs[index])
            isResourceVisible[index] = true
        }
    }

    fun remove(resource: Resource, amount: Int) {
        Messages.append("You lost $amount ${resource.displayName}.")
        val index = resource.ordinal
        resourceCounter[index] -= amount
        resourceCounterSpans[index].innerText = resourceCounter[index].toString()

        if (!isResourceVisible[index]) {
            DIV.appendChild(resoureDivs[index])
            isResourceVisible[index] = true
        }
    }

    fun get(resource: Resource): Int {
        return resourceCounter[resource.ordinal]
    }
}

enum class Resource(val displayName: String, val order: Int, val iconPath: String, val initialAmount: Int = 0) {
    ENERGY("Energy", 0, "resources/energy.svg"),
    EXPLOSIVES("Explosives", 500, "resources/explosives.svg", initialAmount = 3),
    APPROACH_DIRECT("No Nonsense", 1000, "resources/direct.svg"),
    APPROACH_INDIRECT("Thinking Outside The Box", 1010, "resources/indirect.svg")
}