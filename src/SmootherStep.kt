/**
 * Return a smoothly interpolated value from 0 to maxValue in the domain [0, maxTime]
 */
class SmootherStep(val maxTime: Double, val maxValue: Double) {
    var lastUpdate: Double = 0.0
    var lastValue: Double = 0.0

    fun at(time: Double): Double {
        if (time < 0) return 0.0;
        if (time > maxTime) return maxValue;

        val x = time / maxTime;
        val fractValue = x * x * x * (6 * x * x - 15 * x + 10)

        return fractValue * maxValue
    }

    fun current(): Double {
        return lastValue
    }

    /**
     * Returns the difference between the old value and the new value and updates the current delta
     */
    fun increment(delta: Double): Double {
        val oldCurrent = current()
        val newCurrent = at(lastUpdate + delta)

        lastValue = newCurrent
        lastUpdate += delta

        return newCurrent - oldCurrent
    }

    fun isFinished(): Boolean = lastUpdate >= maxTime
}