package cz.cuni.gamedev.nail123.roguelike.world.worlds

import cz.cuni.gamedev.nail123.roguelike.GameConfig
import cz.cuni.gamedev.nail123.roguelike.entities.objects.Stairs
import cz.cuni.gamedev.nail123.roguelike.entities.unplacable.FogOfWar
import cz.cuni.gamedev.nail123.roguelike.mechanics.Pathfinding
import cz.cuni.gamedev.nail123.roguelike.world.Area
import cz.cuni.gamedev.nail123.roguelike.world.builders.wavefunctioncollapse.WFCAreaBuilder
import org.hexworks.zircon.api.data.Position3D

enum class HelperMapTile {
    Room, Corridor, Wall
}

class WaveFunctionCollapsedWorld: DungeonWorld() {
    override fun buildLevel(floor: Int): Area {
        val area = WFCAreaBuilder(GameConfig.AREA_SIZE).create()

        val helperMap = emptyMap<Position3D, HelperMapTile>()
        val directionBlocked = { x: Position3D -> area.blocks[key + x] }

        area.blocks.forEach { (key, value) ->
            val directionBlocked = { x: Position3D -> Pathfinding.doorOpening(area.blocks[key + x]) }
            if(directionBlocked(Position3D.defaultPosition())
                helperMap[key] = HelperMapTile.Wall
            if(directionBlocked(Position3D.create(1,0,0)) && directionBlocked(Position3D.create(-1,0,0))
                || (directionBlocked(Position3D.create(0,1,0)) && directionBlocked(Position3D.create(0,-1,0)))) {
                helperMap[key] = HelperMapTile.Corridor
            } else
                helperMap[ke] = HelperMapTile.Room
        }




        area.addAtEmptyPosition(
            area.player,
            Position3D.create(0, 0, 0),
            GameConfig.VISIBLE_SIZE
        )

        area.addEntity(FogOfWar(), Position3D.unknown())

        // Add stairs up
        if (floor > 0) area.addEntity(Stairs(false), area.player.position)

        // Add stairs down
        val floodFill = Pathfinding.floodFill(area.player.position, area)
        val maxDistance = floodFill.values.maxOrNull()!!
        val staircasePosition = floodFill.filter { it.value > maxDistance / 2 }.keys.random()
        area.addEntity(Stairs(), staircasePosition)

        return area.build()
    }
}