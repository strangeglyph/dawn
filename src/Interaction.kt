import kotlin.browser.window
import kotlin.js.Date

class Interaction(val description: String, val tag: String, val action: () -> dynamic) {
    private var paused: Boolean = true
    private var repeatable: Boolean = false
    private val requirements: MutableList<ResourceStack> = ArrayList()
    private var returns: MutableList<ResourceStack> = ArrayList()
    private var timeToExecute: Int = 0
    private var maxConcurrent: Int = 1
    private var currentConcurrent: Int = 1
    private var progress: Int = 0
    private var lastTick = Date()
    private var finishedCallback: () -> dynamic = {}
    private var progressCallback: (Float) -> dynamic = {}

    fun setRepeatable(): Interaction {
        this.repeatable = true
        return this
    }

    fun requires(res: Resource, amount: Int = 1): Interaction {
        this.requirements.add(ResourceStack(res, amount))
        return this
    }

    fun requires(resStack: ResourceStack): Interaction {
        this.requirements.add(resStack)
        return this
    }

    fun returns(res: Resource, amount: Int = 1): Interaction {
        this.returns.add(ResourceStack(res, amount))
        return this
    }

    fun returns(resStack: ResourceStack): Interaction {
        this.returns.add(resStack)
        return this
    }

    /**
     * Set the execution time for a (non-concurrent) execution of this task in milliseconds
     */
    fun timed(time: Int): Interaction {
        this.timeToExecute = time
        return this
    }

    fun withMaxConcurrent(amount: Int): Interaction {
        this.maxConcurrent = amount
        return this
    }

    fun onFinished(finishedCallback: () -> dynamic): Interaction {
        this.finishedCallback = finishedCallback
        return this
    }

    fun onProgress(progressCallback: (Float) -> dynamic): Interaction {
        this.progressCallback = progressCallback
        return this
    }

    fun increment() {
        if (currentConcurrent < maxConcurrent) {
            if (!canIncrement()) {
                throw IllegalStateException("Not enough resources to activate action")
            }
            takeRequirements()
            currentConcurrent++
            if (currentConcurrent == 1) {
                // Automatically start if the task had no users before and now has one
                start()
            }
        }
    }

    fun canIncrement(): Boolean {
        return requirements.all {
            Resources.get(it.resource) >= it.amount
        }
    }

    private fun takeRequirements() {
        requirements.forEach {
            Resources.remove(it.resource, it.amount)
        }
    }

    fun decrement() {
        if (currentConcurrent > 0) {
            returnReturns()
            currentConcurrent--
            if (currentConcurrent == 0) {
                pause()
            }
        }
    }

    private fun returnReturns() {
        returns.forEach {
            Resources.add(it.resource, it.amount)
        }
    }

    fun start() {
        if (timeToExecute == 0) {
            if (!canIncrement()) {
                throw IllegalStateException("Not enough resources to activate action")
            }
            takeRequirements()
            action()
            returnReturns()
        } else {
            this.paused = false
            lastTick = Date()
            window.setTimeout({ tick() }, 20)
        }
    }

    fun pause() {
        this.paused = true
    }

    private fun tick() {
        val currentTime = Date()
        val deltaMillis = (currentTime.getTime() - lastTick.getTime()).toInt()
        lastTick = currentTime

        progress += deltaMillis * currentConcurrent
        progressCallback(progress / timeToExecute.toFloat())

        if (progress > timeToExecute) {
            action()
            if (repeatable) {
                progress -= timeToExecute
            } else {
                paused = true
                returnReturns()
                finishedCallback()
            }
        }

        if (!paused) {
            window.setTimeout({ tick() }, 20)
        }
    }
}