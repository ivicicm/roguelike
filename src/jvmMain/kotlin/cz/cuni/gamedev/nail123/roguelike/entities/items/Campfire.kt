package cz.cuni.gamedev.nail123.roguelike.entities.items

import cz.cuni.gamedev.nail123.roguelike.entities.Player
import cz.cuni.gamedev.nail123.roguelike.entities.attributes.HasInventory
import cz.cuni.gamedev.nail123.roguelike.entities.attributes.Inventory
import cz.cuni.gamedev.nail123.roguelike.tiles.GameTiles
import org.hexworks.zircon.api.data.Tile

class Campfire(val healths: Int): Item(GameTiles.CAMPFIRE) {
    override fun isEquipable(character: HasInventory): Inventory.EquipResult {
        return Inventory.EquipResult(true, "")
    }

    override val isOneTimeEffect = true

    override fun onEquip(character: HasInventory) {
        if (character is Player) {
            character.maxHitpoints += healths
        }
    }

    override fun onUnequip(character: HasInventory) {
    }

    override fun toString(): String {
        return "Bonefire flame ($healths)"
    }
}