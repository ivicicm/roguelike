package cz.cuni.gamedev.nail123.roguelike.actions

import cz.cuni.gamedev.nail123.roguelike.world.Area

class Wait : GameAction() {
    override fun tryPerform(area: Area): Boolean {
        return true
    }
}