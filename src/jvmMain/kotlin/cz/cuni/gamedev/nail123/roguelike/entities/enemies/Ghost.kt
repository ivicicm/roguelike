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

    override val maxHitpoints = 20
    override var hitpoints = 20
    override var attack = 6
    override var defense = 0

    override val dontRoamAroundTooMuch = false

    override fun update() {
        if (Pathfinding.chebyshev(position, area.player.position) <= smellingRadius) {
            goToPlayer()
        } else {
            goToRandomTarget()
        }
    }
}