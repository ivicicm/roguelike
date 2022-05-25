package cz.cuni.gamedev.nail123.roguelike.entities.items

import cz.cuni.gamedev.nail123.roguelike.entities.GameEntity

object LootSystem {
    fun getChestItem(floor: Int): GameEntity {
        return Sword(3)
    }
}
