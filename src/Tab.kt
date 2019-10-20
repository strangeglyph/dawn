
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.div
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.progress
import org.w3c.dom.*
import org.w3c.dom.events.MouseEvent
import kotlin.browser.document
import kotlin.dom.addClass
import kotlin.dom.removeClass

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

class InteractionRequirementsModelViewMapping(
        val resourceStack: ResourceStack,
        val fullSpan: HTMLSpanElement,
        val image: HTMLImageElement,
        val amountSpan: HTMLSpanElement
)

class InteractionModelViewMapping(
        val interaction: Interaction,
        val incButton: HTMLButtonElement,
        val decButton: HTMLButtonElement,
        val amountSpan: HTMLSpanElement,
        val requirements: MutableSet<InteractionRequirementsModelViewMapping> = mutableSetOf()
)

object InteractionModelViewMappings {
    val items: MutableMap<Interaction, InteractionModelViewMapping> = mutableMapOf()


    fun updateInteractionsRequirementDisplay(interaction: Interaction) {
        val viewMapping = items[interaction]
                ?: throw IllegalArgumentException("Interaction has no defined view: ${interaction.tag}")

        var allReqsFulfilled = true
        viewMapping.requirements.forEach {
            val stack = it.resourceStack
            if (Resources.get(stack.resource) >= stack.amount) {
                it.image.src = stack.resource.iconPath
                it.amountSpan.removeClass("text-red")
            } else {
                it.image.src = stack.resource.iconPathDepleted
                it.amountSpan.addClass("text-red")
                allReqsFulfilled = false
            }
        }

        viewMapping.incButton.disabled = !allReqsFulfilled || interaction.getCurrentActive() == interaction.getMaxConcurrent()
        viewMapping.amountSpan.innerText = "${interaction.getCurrentActive()}/${interaction.getMaxConcurrent()}"
    }

    fun updateInteractionsRequirementDisplay() {
        items.forEach { updateInteractionsRequirementDisplay(it.key) }
    }
}

open class Tab(val divName: String, val displayName: String, val order: Int) {
    protected val MAIN_DIV = document.getElementById(divName) as HTMLDivElement
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

    fun addInteraction(interaction: Interaction) {


        val activeCount = document.create.span {
            +"0/${ interaction.getMaxConcurrent() }"
            classes = setOf("interaction-counter")
        } as HTMLSpanElement

        val progressBar = document.create.progress {
            classes = setOf("interaction-progress")
            value = "0"
        }

        val decButton = document.create.button {
            +"-"
            classes = setOf("interaction-dec")
            disabled = true
        } as HTMLButtonElement
        val incButton = document.create.button {
            +"+"
            classes = setOf("interaction-inc")
            disabled = !interaction.canIncrement()
        } as HTMLButtonElement
        decButton.onclick = {
            interaction.decrement()
            activeCount.innerText = "${interaction.getCurrentActive()}/${interaction.getMaxConcurrent()}"
            if (interaction.canIncrement()) {
                incButton.disabled = false
            }
            if (interaction.getCurrentActive() == 0) {
                decButton.disabled = true
            }
        }
        incButton.onclick = {
            if (interaction.canIncrement()) {
                interaction.increment()
                decButton.disabled = false
                if (!interaction.canIncrement() || interaction.getCurrentActive() == interaction.getMaxConcurrent()) {
                    incButton.disabled = true
                }
            }
            activeCount.innerText = "${interaction.getCurrentActive()}/${interaction.getMaxConcurrent()}"
            Unit
        }

        val mapping = InteractionModelViewMapping(interaction, incButton, decButton, activeCount)

        val incrementReqs = document.create.div("interaction-increment-requirements")
        interaction.getIncrementCosts().forEach {
            val fullSpan = makeRequirementSpan(it, mapping)
            incrementReqs.append(fullSpan)
        }

        val progressReqs = document.create.div("interaction-progress-requirements")
        interaction.getProgressCosts().forEach {
            val fullSpan = makeRequirementSpan(it, mapping)
            fullSpan.addClass("float-right")
            progressReqs.append(fullSpan)
        }

        val interactionDiv = document.create.div {
            id = interaction.tag
            classes = setOf("interaction-long")

            span {
                +interaction.description
                classes = setOf("interaction-label")
            }

        }
        interactionDiv.append(activeCount, progressBar, incrementReqs, progressReqs, incButton, decButton)
        INTERACTIONS.append(interactionDiv)

        interaction.onProgress {
            progressBar.value = it
        }

        interaction.onFinished {
            INTERACTIONS.removeChild(interactionDiv)
        }

        InteractionModelViewMappings.items[interaction] = mapping
    }

    private fun makeRequirementSpan(resStack: ResourceStack, interactionMapping: InteractionModelViewMapping): HTMLSpanElement {
        val img = document.create.img {
            classes = setOf("interaction-requirement-item-icon")
            src =
                    if (Resources.get(resStack.resource) >= resStack.amount)
                        resStack.resource.iconPath
                    else
                        resStack.resource.iconPathDepleted
            alt = resStack.resource.displayName
        } as HTMLImageElement

        val amount = document.create.span {
            +"${resStack.amount}"
            classes =
                    if (Resources.get(resStack.resource) >= resStack.amount)
                        setOf("interaction-requirement-item-amount")
                    else
                        setOf("interaction-requirement-item-amount", "text-red")
        } as HTMLSpanElement

        val fullSpan = document.create.span("interaction-requirement-item") as HTMLSpanElement
        fullSpan.append(img, amount)

        interactionMapping.requirements.add(InteractionRequirementsModelViewMapping(resStack, fullSpan, img, amount))
        return fullSpan
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