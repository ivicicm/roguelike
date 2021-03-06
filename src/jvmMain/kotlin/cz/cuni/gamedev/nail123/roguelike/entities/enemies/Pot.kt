package cz.cuni.gamedev.nail123.roguelike.entities.enemies

import cz.cuni.gamedev.nail123.roguelike.entities.attributes.HasSmell
import cz.cuni.gamedev.nail123.roguelike.entities.items.Sword
import cz.cuni.gamedev.nail123.roguelike.mechanics.Pathfinding
import cz.cuni.gamedev.nail123.roguelike.mechanics.goBlindlyTowards
import cz.cuni.gamedev.nail123.roguelike.tiles.GameTiles

class Pot: Enemy(GameTiles.POT) {
    override val blocksMovement = true
    override val blocksVision = false

    override val maxHitpoints = 10
    override var hitpoints = 4
    override var attack = 0
    override var defense = 0

}