/**
 * Return a smoothly interpolated value from initialValue to targetValue in the domain [0, maxTime]
 */
class SmootherStep(val period: Double, val initialValue: Double, val targetValue: Double) {
    constructor(period: Double, targetValue: Double) : this(period, 0.0, targetValue);

    var lastUpdate: Double = 0.0
    var lastValue: Double = initialValue

    val diff = targetValue - initialValue;

    fun at(time: Double): Double {
        if (time < 0) return initialValue;
        if (time > period) return targetValue;

        val x = time / period;
        val fractValue = x * x * x * (6 * x * x - 15 * x + 10)

        return initialValue + fractValue * diff
    }

    fun current(): Double {
        return lastValue
    }

    /**
     * Increments the current animation time and returns the change between the current and
     * the previous y
     */
    fun increment(delta: Double): Double {
        val oldCurrent = current()
        val newCurrent = at(lastUpdate + delta)

        lastValue = newCurrent
        lastUpdate += delta

        return newCurrent - oldCurrent
    }

    fun percentCompleted(): Double = lastUpdate / period

    fun isFinished(): Boolean = lastUpdate >= period
}