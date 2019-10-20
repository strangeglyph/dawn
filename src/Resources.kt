import kotlinx.html.classes
import kotlinx.html.dom.create
import kotlinx.html.img
import kotlinx.html.js.div
import kotlinx.html.span
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.get
import kotlin.browser.document

enum class Resource(val displayName: String, val order: Int, val iconPath: String, val iconPathDepleted: String, val initialAmount: Int = 0) {
    ENERGY("Energy", 0, "resources/energy.svg", "resources/energy_depleted.svg"),
    BROKEN_DOOR("A Broken Door", 500, "resources/door_broken.svg", "resources/door_broken_depleted.svg"),
    APPROACH_DIRECT("No Nonsense", 1000, "resources/direct.svg", "resources/direct_depleted.svg"),
    APPROACH_INDIRECT("Thinking Outside The Box", 1010, "resources/indirect.svg", "resources/indirect_depleted.svg"),
    MANPOWER("Manpower", 10, "resources/manpower.svg", "resources/manpower_depleted.svg", 10),
    ICE("Ice", 20, "resources/ice.svg", "resources/ice_depleted.svg")
}

class ResourceStack(val resource: Resource, val amount: Int)


object Resources {
    private val DIV = document.getElementById("resources-div") as HTMLDivElement

    private val resourceCounter: Array<Int> = Resource.values().map { it.initialAmount }.toTypedArray()
    private val isResourceVisible: Array<Boolean> = Array(Resource.values().size) { false }

    private val resourceDivs: Array<HTMLDivElement> = Resource.values().map { res ->
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

    private val resourceCounterSpans: Array<HTMLSpanElement> = resourceDivs.map {
        it.getElementsByClassName("resource-counter")[0] as HTMLSpanElement
    }.toTypedArray()


    fun add(resource: Resource, amount: Int) {
        // Messages.append("You gained $amount ${resource.displayName}.")
        val index = resource.ordinal
        resourceCounter[index] += amount
        resourceCounterSpans[index].innerText = resourceCounter[index].toString()

        if (!isResourceVisible[index]) {
            DIV.appendChild(resourceDivs[index])
            isResourceVisible[index] = true
        }

        InteractionModelViewMappings.updateInteractionsRequirementDisplay()
    }

    fun remove(resource: Resource, amount: Int) {
        // Messages.append("You lost $amount ${resource.displayName}.")
        val index = resource.ordinal
        resourceCounter[index] -= amount
        resourceCounterSpans[index].innerText = resourceCounter[index].toString()

        if (!isResourceVisible[index]) {
            DIV.appendChild(resourceDivs[index])
            isResourceVisible[index] = true
        }

        InteractionModelViewMappings.updateInteractionsRequirementDisplay()
    }

    fun get(resource: Resource): Int {
        return resourceCounter[resource.ordinal]
    }
}