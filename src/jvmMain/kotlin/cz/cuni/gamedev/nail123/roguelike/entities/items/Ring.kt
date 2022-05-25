package cz.cuni.gamedev.nail123.roguelike.entities.items

import cz.cuni.gamedev.nail123.roguelike.entities.Player
import cz.cuni.gamedev.nail123.roguelike.entities.attributes.HasInventory
import cz.cuni.gamedev.nail123.roguelike.entities.attributes.Inventory
import cz.cuni.gamedev.nail123.roguelike.entities.enemies.Enemy
import cz.cuni.gamedev.nail123.roguelike.tiles.GameTiles
import org.hexworks.zircon.api.data.Tile
import kotlin.reflect.KClass

class Ring(val friendlyWith: KClass<Enemy>): Item(GameTiles.RING) {
    override fun isEquipable(character: HasInventory): Inventory.EquipResult {
        return if (character.inventory.equipped.filterIsInstance<Ring>().isNotEmpty()) {
            Inventory.EquipResult(false, "You already have a ring.")
        } else Inventory.EquipResult.Success
    }

    override fun onEquip(character: HasInventory) {

    }

    override fun onUnequip(character: HasInventory) {

    }

    override fun toString(): String {
        return "Ring of ${friendlyWith.simpleName?.lowercase()} friendship"
    }
}