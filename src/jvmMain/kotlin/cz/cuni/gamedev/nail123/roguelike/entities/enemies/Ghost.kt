package cz.cuni.gamedev.nail123.roguelike.entities.enemies

import cz.cuni.gamedev.nail123.roguelike.entities.attributes.HasSmell
import cz.cuni.gamedev.nail123.roguelike.entities.items.Sword
import cz.cuni.gamedev.nail123.roguelike.mechanics.Pathfinding
import cz.cuni.gamedev.nail123.roguelike.mechanics.goBlindlyTowards
import cz.cuni.gamedev.nail123.roguelike.tiles.GameTiles

class Ghost: Enemy(GameTiles.GHOST), HasSmell {
    override val blocksMovement = false
    override val blocksVision = false
    override val smellingRadius = 1

    override val maxHitpoints = 10
    override var hitpoints = 9
    override var attack = 7
    override var defense = 0

    override fun update() {
        if (Pathfinding.chebyshev(position, area.player.position) <= smellingRadius) {
            goToPlayer()
        } else {
            goToRandomTarget()
        }
    }

    override fun die() {
        super.die()
        // Drop a sword
        this.block.entities.add(Sword(2))
    }
}