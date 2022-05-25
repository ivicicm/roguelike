package cz.cuni.gamedev.nail123.roguelike.entities.enemies

import cz.cuni.gamedev.nail123.roguelike.entities.attributes.HasSmell
import cz.cuni.gamedev.nail123.roguelike.entities.items.Sword
import cz.cuni.gamedev.nail123.roguelike.mechanics.Pathfinding
import cz.cuni.gamedev.nail123.roguelike.mechanics.goBlindlyTowards
import cz.cuni.gamedev.nail123.roguelike.tiles.GameTiles

class Dog: Enemy(GameTiles.Dog), HasSmell {
    override val blocksMovement = true
    override val blocksVision = false
    override val smellingRadius = 7

    override val maxHitpoints = 10
    override var hitpoints = 3
    override var attack = 2
    override var defense = 0

    override fun update() {
        if (Pathfinding.chebyshev(position, area.player.position) <= smellingRadius) {
            goToPlayer()
        }
    }

    override fun die() {
        super.die()
        // Drop a sword
        this.block.entities.add(Sword(2))
    }
}