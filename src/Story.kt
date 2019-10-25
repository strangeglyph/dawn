object Story {

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
        AttractorTab.enable()
        Log.enable()

        Outside.setDescription("Your torches shed light on a small area outside the base, maybe fifty strides across. In it, you recognize the all-too-familiar monotonicity of the icy wasteland.")
        Inside.setDescription("The walls are iron plate and frost gives them a faint shimmer. A frozen wall blocks the path ahead.")
        AttractorTab.setDescription("The accelerator lies before you: The silent remnant of a long-gone past.")

        LocationIndicator.show()
        LocationIndicator.set(Outside.displayName)

        Resources.add(Resource.MANPOWER, 0)
        Resources.add(Resource.ENERGY, 8)

        val clearIceTask = Interaction("Clear ice block", "clear_ice_block") {
            Resources.add(Resource.ICE, 1)
        }
        clearIceTask.setRepeatable()
                .incrementCost(Resource.MANPOWER, 1)
                .progressCost(Resource.ENERGY, 2)
                .withMaxConcurrent(5)
                .timed(5000)

        Inside.addInteraction(clearIceTask)

    }
}