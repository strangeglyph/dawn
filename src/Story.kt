object Story {
    private var baseClearProgress: Int = 0;
    private val BASE_CLEAR_THRESHOLD_1: Int = 5

    fun beginStory() {
        Messages.append("The sun has long become a pale spot on the sky, incapable of spending light and warmth. Your group " +
                "has trekked across the frozen ruins of your once-great civilization, spurred by traces and documents " +
                "of a technology that might help revive your dieing planet. \n" +
                "\n" +
                "Finally you arrive in a valley, steep cliffs rising all around you. On the far end of the valley, you can " +
                "make out a large structure seemingly built into the mountain."
        )
        Outside.addSingleUseInteraction("approach_gate", "Approach the structure") {
            arriveAtGate()
        }
    }

    fun arriveAtGate() {
        Messages.append("You arrive at an iron gate: Heavy, forbidding, inset into the mountain face that fades into the " +
                "darkness of the Forever Night. The other members of your party gather around you, shivering in the cold as " +
                "you debate how to proceed.")

        Outside.addInteraction("force_open", "Force the door open") {
            Messages.append("You gather the strongest members of your group around you and try to find an angle to push the door aside. " +
                    "Unsucessfully, until someone remembers that your brought a small amount of plastic explosives with you. " +
                    "A loud bang and the happy shrieking of the children in your group and the locking mechanism of the gate is no more.")
            Resources.add(Resource.APPROACH_DIRECT, 1)
            Resources.add(Resource.BROKEN_DOOR, 1)
            gainEntry()
        }

        Outside.addInteraction("go_around", "Find a different way in") {
            Messages.append("After searching for a while, a small girl in your entourage manages to find a ventilation shaft hidden by ice and snow. " +
                    "She crawls inside and after a tense few minutes, an audible \"click\" signifies that the gate has been unlocked.")
            Resources.add(Resource.APPROACH_INDIRECT, 1)
            gainEntry()
        }
    }

    private fun gainEntry() {
        Outside.removeInteraction("force_open")
        Outside.removeInteraction("go_around")

        Outside.enable()
        Inside.enable()
        Log.enable()

        Outside.setDescription("Your torches shed light on a small area outside the door, maybe fifty strides across. " +
                "In it, you recognize the all-too-familiar monotonicity of the icy wasteland.")
        Inside.setDescription("The walls are iron plate and frost gives them a faint shimmer. A solid wall of ice blocks the path ahead.")

        LocationIndicator.show()
        LocationIndicator.set(Outside.displayName)

        Resources.add(Resource.MANPOWER, 0)

        val clearIceTask = Interaction("Clear ice block", "clear_ice_block") {
            Resources.add(Resource.ICE, 1)
            baseClearProgress += 1
            if (baseClearProgress == BASE_CLEAR_THRESHOLD_1) {
                enterBaseProper()
                it.end()
            }
        }
                .setRepeatable()
                .incrementCost(Resource.MANPOWER, 1)
                .withMaxConcurrent(5)
                .timed(5000)

        Inside.addInteraction(clearIceTask)

    }

    private fun enterBaseProper() {
        Messages.append("With a final 'Ho!' your pickaxes break through the wall ahead. In the area behind, " +
                "you find a storage room where you drop off the supplies you brought with you, " +
                "as well a couple of barracks. It is freezing cold - you should probably think about " +
                "starting a campfire.\n" +
                "\n" +
                "You find a door the seems to lead deeper into the mountain, but it it frozen shut.")

        Inside.setDescription("The walls are iron plate and frost gives them a faint shimmer. A storage room " +
                "keeps your meager supplies. The barracks contain enough space for everyone, though " +
                "the current temperature makes sleep a dangerous proposition.")

        Resources.add(Resource.WOOD, 15)
        Resources.add(Resource.FOOD, 100)

        Inside.enableHeat()

        val burnWoodTask = Interaction("Make a camp fire", "burn_wood_for_heat") {
            // Inside.increaseHeat(2.0)
            val heatLevel = when {
                it.getCurrentActive() >= 10 -> {
                    Inside.HeatLevel.WARM
                }
                it.getCurrentActive() >= 5 -> {
                    Inside.HeatLevel.COOL
                }
                it.getCurrentActive() >= 2 -> {
                    Inside.HeatLevel.COLD
                }
                else -> {
                    Inside.HeatLevel.FREEZING
                }
            }
            val partialProgress = when {
                it.getCurrentActive() >= 10 -> {
                    0.0
                }
                it.getCurrentActive() >= 5 -> {
                    (it.getCurrentActive() - 5) / 5.0
                }
                it.getCurrentActive() >= 2 -> {
                    (it.getCurrentActive() - 2) / 3.0
                }
                else -> {
                    it.getCurrentActive() / 2.0
                }
            }
            Inside.fadeHeatTo(heatLevel, partialProgress)
        }
        .setRepeatable()
        //.progressCost(Resource.WOOD)
        .timed(3000)
        .withMaxConcurrent(10)
        .disableTimeScaling()
        .onPause { Inside.fadeHeatTo(Inside.HeatLevel.FREEZING, 0.0) }

        Inside.addInteraction(burnWoodTask)
    }
}