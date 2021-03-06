package cz.cuni.gamedev.nail123.roguelike.entities.enemies

import cz.cuni.gamedev.nail123.roguelike.entities.GameEntity
import cz.cuni.gamedev.nail123.roguelike.entities.MovingEntity
import cz.cuni.gamedev.nail123.roguelike.entities.Player
import cz.cuni.gamedev.nail123.roguelike.entities.attributes.*
import cz.cuni.gamedev.nail123.roguelike.entities.items.Ring
import cz.cuni.gamedev.nail123.roguelike.mechanics.Combat
import cz.cuni.gamedev.nail123.roguelike.mechanics.Pathfinding
import cz.cuni.gamedev.nail123.roguelike.mechanics.goBlindlyTowards
import cz.cuni.gamedev.nail123.roguelike.mechanics.goSmartlyTowards
import org.hexworks.zircon.api.data.Position3D
import org.hexworks.zircon.api.data.Tile

abstract class Enemy(tile: Tile): MovingEntity(tile), HasCombatStats, Interactable, Interacting {
    override val blocksMovement = true

    fun isFriendlyWithPlayer(): Boolean {
        return area.player.items.any { it is Ring && it.friendlyWith.isInstance(this) }
    }

    override fun acceptInteractFrom(other: GameEntity, type: InteractionType) = interactionContext(other, type) {
        withEntity<Player>(InteractionType.BUMPED) { player -> Combat.attack(player, this@Enemy) }
    }

    override fun interactWith(other: GameEntity, type: InteractionType) = interactionContext(other, type) {
        withEntity<Player>(InteractionType.BUMPED) { player -> Combat.attack(this@Enemy, player) }
    }

    var randomWalkTarget: Position3D? = null
    open val dontRoamAroundTooMuch = true
    open val randomWalkDistance = 10
    var roamingCenter: Position3D? = null

    fun goToRandomTarget(resetTarget: Boolean = false) {
        if(resetTarget || randomWalkTarget == null || chasingPlayer || randomWalkTarget == position) {
            chasingPlayer = false
            val closeTiles = Pathfinding.floodFill(roamingCenter ?: position, area, blocking = Pathfinding.defaultBlocking, maxDistance = randomWalkDistance)
            randomWalkTarget = closeTiles.filter { it.value > 5 }.keys.randomOrNull() ?: closeTiles.keys.random()
        }
        if(!goSmartlyTowards(randomWalkTarget!!))
            randomWalkTarget = null
    }

    var chasingPlayer = false
    var seenPlayer = false

    fun goToPlayer() {
        if(!isFriendlyWithPlayer())
            goSmartlyTowards(area.player.position)
        chasingPlayer = true
        seenPlayer = true
        if(dontRoamAroundTooMuch) {
            roamingCenter = position
        }
    }
}