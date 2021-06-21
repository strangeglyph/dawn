import kotlin.browser.window
import kotlin.js.Date

object TickThread {
    private val tasks: MutableMap<String, (time : Int) -> Unit> = HashMap()
    private var lastUpdate = Date()

    fun register(name: String, task : (time : Int) -> Unit) {
        if (name in tasks) { throw IllegalStateException("Task $name already registered") }
        tasks[name] = task
    }

    /**
     * Unregister a task, raise an exception if it doesn't exist
     */
    fun unregisterStrict(name: String) {
        if (name !in tasks) { throw IllegalStateException("Task $name is not registered") }
        tasks.remove(name)
    }

    /**
     * Unregister a task, do nothing if it doesn't exist
     */
    fun unregister(name: String) {
        tasks.remove(name)
    }

    fun hasTask(name: String): Boolean {
        return name in tasks
    }

    init {
        lastUpdate = Date()
        window.setInterval({
            val currentTime = Date()
            val deltaMillis = (currentTime.getTime() - lastUpdate.getTime()).toInt()
            lastUpdate = currentTime

            //println(">> Tick delta $deltaMillis")
            for ((tag, task) in tasks) {
                //println("Ticking $tag")
                task(deltaMillis)
            }
        }, 20)
    }
}