package cz.cuni.gamedev.nail123.roguelike.entities.items

import cz.cuni.gamedev.nail123.roguelike.entities.Player
import cz.cuni.gamedev.nail123.roguelike.entities.attributes.HasInventory
import cz.cuni.gamedev.nail123.roguelike.entities.attributes.Inventory
import cz.cuni.gamedev.nail123.roguelike.tiles.GameTiles
import org.hexworks.zircon.api.data.Tile

class Armor(val defence: Int): Item(GameTiles.ARMOR) {
    override fun isEquipable(character: HasInventory): Inventory.EquipResult {
        return if (character.inventory.equipped.filterIsInstance<Armor>().isNotEmpty()) {
            Inventory.EquipResult(false, "You already have armor.")
        } else Inventory.EquipResult.Success
    }

    override fun onEquip(character: HasInventory) {
        if (character is Player) {
            character.defense += defence
        }
    }

    override fun onUnequip(character: HasInventory) {
        if (character is Player) {
            character.defense -= defence
        }
    }

    override fun toString(): String {
        return "Armor ($defence)"
    }
}