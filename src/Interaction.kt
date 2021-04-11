class Interaction(val description: String, val tag: String, val action: (Interaction) -> Unit) {
    private var repeatable: Boolean = false
    private val incrementCosts: MutableList<ResourceStack> = ArrayList()
    private val progressCosts: MutableList<ResourceStack> = ArrayList()
    private var timeToExecute: Int = 0
    private var maxConcurrent: Int = 1
    private var currentConcurrent: Int = 0
    private var progress: Int = 0
    private var finishedCallback: () -> Unit = {}
    private var progressCallback: (Double) -> Unit = {}
    private var pauseCallback: () -> Unit = {}

    /**
     * Marks a task as repeatable. Nonrepeatable tasks are automatically cleared once completed.
     *
     * Defaults to nonrepeatable.
     */
    fun setRepeatable(): Interaction {
        this.repeatable = true
        return this
    }

    /**
     * Marks this task as requiring some materials to for each concurrent task. Required materials are automatically
     * returned on task completion.
     *
     * Can be called multiple times, all provided resources will be required.
     */
    fun incrementCost(res: Resource, amount: Int = 1): Interaction {
        this.incrementCosts.add(ResourceStack(res, amount))
        return this
    }

    /**
     * Marks this task as requiring some materials to for each concurrent task. Required materials are automatically
     * returned on task completion.
     *
     * Can be called multiple times, all provided resources will be required.
     */
    fun incrementCost(resStack: ResourceStack): Interaction {
        this.incrementCosts.add(resStack)
        return this
    }

    fun getIncrementCosts(): List<ResourceStack> {
        return incrementCosts
    }

    /**
     * Progress costs are required every time the task is incremented, as well as every time the task starts over.
     * Progress costs are not returned after task completion.
     *
     * Can be called multiple times, all provided resources will be required.
     */
    fun progressCost(res: Resource, amount: Int = 1): Interaction {
        this.progressCosts.add(ResourceStack(res, amount))
        return this
    }

    /**
     * Progress costs are required every time the task is incremented, as well as every time the task starts over.
     * Progress costs are not returned after task completion.
     *
     * Can be called multiple times, all provided resources will be required.
     */
    fun progressCost(resStack: ResourceStack): Interaction {
        this.progressCosts.add(resStack)
        return this
    }

    fun getProgressCosts(): List<ResourceStack> {
        return progressCosts
    }


    /**
     * Set the execution time for a (non-concurrent) execution of this task in milliseconds. Actual execution time
     * will be scaled according to how many concurrent instances of this task are running.
     *
     * Defaults to 0, i.e. the task completes immediately
     */
    fun timed(time: Int): Interaction {
        this.timeToExecute = time
        return this
    }

    /**
     * Set a limit on how often this task can be executed in parallel. Actual execution time
     * will be scaled according to how many concurrent instances of this task are running.
     *
     * Defaults to 1.
     */
    fun withMaxConcurrent(amount: Int): Interaction {
        this.maxConcurrent = amount
        return this
    }

    fun getMaxConcurrent(): Int = maxConcurrent
    fun getCurrentActive(): Int = currentConcurrent

    /**
     * Register a callback to be called when this task completes. This is intended for UI managers to be able
     * to react to the task being finished, gameplay changes should be done in the action callback of the task.
     * Note that this is only called for non-repeatable tasks.
     */
    fun onFinished(finishedCallback: () -> Unit): Interaction {
        this.finishedCallback = finishedCallback
        return this
    }

    /**
     * Register a callback to be called every time this task is ticked. The callback is passed the progress of the
     * task from 0 (no progress) to 1 (completed)
     */
    fun onProgress(progressCallback: (Double) -> Unit): Interaction {
        this.progressCallback = progressCallback
        return this
    }

    /**
     * Register a task callback to be called every time this task is paused.
     */
    fun onPause(pauseCallback: () -> Unit): Interaction {
        this.pauseCallback = pauseCallback
        return this
    }

    /**
     * Increment the number of currently running tasks. Has no effect if the number of concurrent instances is at max.
     *
     * If the necessary resources cannot be provided, throws {IllegalStateException}.
     *
     * If this is incrementing the task from zero instances to one, starts the task.
     */
    fun increment() {
        if (currentConcurrent < maxConcurrent) {
            if (!canIncrement()) {
                throw IllegalStateException("Not enough resources to activate action")
            }
            takeIncrementCosts()
            currentConcurrent++
            if (currentConcurrent == 1) {
                // Automatically start if the task had no users before and now has one
                start()
            }
        }
    }

    /**
     * Check if the player has enough resources to increment the task.
     */
    fun canIncrement(): Boolean {
        return canProgress() && incrementCosts.all {
            Resources.get(it.resource) >= it.amount
        }
    }

    fun canProgress(): Boolean {
        return progressCosts.all {
            Resources.get(it.resource) >= it.amount
        }
    }

    fun getResourceConcurrencyLimit(): Int {
        return progressCosts.map { Resources.get(it.resource) / it.amount }.min() ?: Int.MAX_VALUE
    }

    private fun takeIncrementCosts() {
        incrementCosts.forEach {
            Resources.remove(it.resource, it.amount)
        }
        progressCosts.forEach {
            Resources.remove(it.resource, it.amount)
        }
    }

    private fun takeProgressCosts() {
        progressCosts.forEach {
            Resources.remove(it.resource, it.amount * currentConcurrent)
        }
    }

    /**
     * Decrements the task, reducing the number of concurrent executions by one. Has no effect if the number of
     * currently executing tasks is at zero. Returns all the resources listed in {returns}.
     *
     * If this causes the number of current executions to drop to 0, pauses the task.
     */
    fun decrement() {
        if (currentConcurrent > 0) {
            returnIncrementCosts()
            currentConcurrent--
            if (currentConcurrent == 0) {
                pause()
            }
        }
    }

    fun decrementToLimit() {
        val diff = currentConcurrent - getResourceConcurrencyLimit()
        if (diff > 0) {
            returnMultiple(diff)
            currentConcurrent -= diff
            if (currentConcurrent == 0) {
                pause()
            }
        }
    }

    private fun returnIncrementCosts() {
        returnMultiple(currentConcurrent)
    }

    private fun returnMultiple(amount: Int) {
        incrementCosts.forEach {
            Resources.add(it.resource, it.amount * amount)
        }
    }

    /**
     * Starts the task. If this is an instant execution task, take requirements, execute the completed callback
     * and return the materials to return.
     *
     * Otherwise, schedule a repeating invocation of the tick handler.
     */
    fun start() {
        if (timeToExecute == 0) {
            if (!canIncrement()) {
                throw IllegalStateException("Not enough resources to activate action")
            }
            takeIncrementCosts()
            action(this)
            returnIncrementCosts()
        } else {
            if (!TickThread.hasTask(tag)) {
                println("[$tag] Starting ticking")
                TickThread.register(tag) { ms -> tick(ms) }
            } else println("[$tag] already exists")
        }
    }

    /**
     * Pause execution of the task until {#start} is called again
     */
    fun pause() {
        if (TickThread.hasTask(tag)) {
            println("[$tag] Pausing")
            TickThread.unregister(tag)
        }
        pauseCallback()
    }

    private fun tick(deltaMillis: Int) {
        progress += deltaMillis * currentConcurrent

        // Notify tick handler of our progress, scaled from 0 to 1
        if (progress > timeToExecute) {
            progressCallback(1.0)
        } else {
            progressCallback(progress / timeToExecute.toDouble())
        }

        if (progress > timeToExecute) {
            action(this)
            if (repeatable) {
                progress -= timeToExecute
                decrementToLimit()
                takeProgressCosts()
                progressCallback(0.0)
            } else {
                pause()
                returnIncrementCosts()
                finishedCallback()
            }
        }
    }

    /**
     * If this tasks was repeatable, end it, refunding increment costs and removing it from the UI
     */
    fun end() {
        pause()
        returnIncrementCosts()
        finishedCallback()
    }
}