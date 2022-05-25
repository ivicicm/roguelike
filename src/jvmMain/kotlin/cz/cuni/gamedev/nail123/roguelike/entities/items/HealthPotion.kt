package cz.cuni.gamedev.nail123.roguelike.entities.items

import cz.cuni.gamedev.nail123.roguelike.entities.Player
import cz.cuni.gamedev.nail123.roguelike.entities.attributes.HasInventory
import cz.cuni.gamedev.nail123.roguelike.entities.attributes.Inventory
import cz.cuni.gamedev.nail123.roguelike.tiles.GameTiles
import org.hexworks.zircon.api.data.Tile

class HealthPotion(val healths: Int): Item(GameTiles.POTION) {
    override fun isEquipable(character: HasInventory): Inventory.EquipResult {
        return Inventory.EquipResult(true, "")
    }

    override val isOneTimeEffect = true

    override fun onEquip(character: HasInventory) {
        if (character is Player) {
            character.hitpoints = (character.hitpoints + healths).coerceAtMost(character.maxHitpoints)
        }
    }

    override fun onUnequip(character: HasInventory) {
    }

    override fun toString(): String {
        return "Health potion ($healths)"
    }
}