package cz.cuni.gamedev.nail123.roguelike.entities.enemies

import cz.cuni.gamedev.nail123.roguelike.entities.attributes.HasSmell
import cz.cuni.gamedev.nail123.roguelike.entities.items.Sword
import cz.cuni.gamedev.nail123.roguelike.mechanics.Pathfinding
import cz.cuni.gamedev.nail123.roguelike.mechanics.goBlindlyTowards
import cz.cuni.gamedev.nail123.roguelike.mechanics.goSmartlyTowards
import cz.cuni.gamedev.nail123.roguelike.tiles.GameTiles

class Golem: Enemy(GameTiles.GOLEM), HasSmell {
    override val blocksMovement = true
    override val blocksVision = false
    override val smellingRadius = 7

    override val maxHitpoints = 15
    override var hitpoints = 15
    override var attack = 6
    override var defense = 2

    val movePattern = listOf(true, false)
    var time = 0

    override fun update() {
        time++
        if(movePattern[time % movePattern.size]) {
            if (Pathfinding.chebyshev(position, area.player.position) <= smellingRadius) {
                goToPlayer()
            } else if (seenPlayer) {
                goToRandomTarget()
            }
        }
    }
}