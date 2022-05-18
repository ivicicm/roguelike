package cz.cuni.gamedev.nail123.roguelike.world.worlds

import cz.cuni.gamedev.nail123.roguelike.GameConfig
import cz.cuni.gamedev.nail123.roguelike.entities.objects.Stairs
import cz.cuni.gamedev.nail123.roguelike.entities.unplacable.FogOfWar
import cz.cuni.gamedev.nail123.roguelike.mechanics.Pathfinding
import cz.cuni.gamedev.nail123.roguelike.world.Area
import cz.cuni.gamedev.nail123.roguelike.world.builders.wavefunctioncollapse.WFCAreaBuilder
import org.hexworks.zircon.api.data.Position3D
import java.util.ArrayDeque

enum class HelperMapTileType {
    Room, Corridor, Wall
}

class WaveFunctionCollapsedWorld: DungeonWorld() {
    data class HelperMapTile(val type: HelperMapTileType, val index: Int = 0)
    data class GetSameNeighbouringTilesResult(val tiles: List<Position3D>, val neighboursOfTiles: List<Position3D>)

    // First result are all the tiles with same type that can be reached from position by tiles with same type, next result is list of neighbours of the first tiles
    fun getSameNeighbouringTiles(helperMap: Map<Position3D, HelperMapTile>, position: Position3D): GetSameNeighbouringTilesResult {
        val tileType = helperMap[position]?.type
        val openVertices = ArrayDeque<Position3D>()
        val visitedVertices = mutableSetOf<Position3D>()
        openVertices.add(position)
        visitedVertices.add(position)
        val neighbouringVertices = mutableSetOf<Position3D>()

        while (openVertices.isNotEmpty()) {
            val currentPos = openVertices.pollFirst()
            Pathfinding.fourDirectional(currentPos)
                .filter { helperMap[it]?.type == tileType && !visitedVertices.contains(it) }
                .forEach {
                    openVertices.addLast(it)
                    visitedVertices.add(it)
                }
            Pathfinding.fourDirectional(currentPos)
                .filter { helperMap[it] != null && helperMap[it]?.type != tileType }
                .forEach {
                    neighbouringVertices.add(it)
                }
        }

        return GetSameNeighbouringTilesResult(visitedVertices.toList(), neighbouringVertices.toList())
    }

    override fun buildLevel(floor: Int): Area {
        val area = WFCAreaBuilder(GameConfig.AREA_SIZE).create()

        // Make helper map
        val helperMap = mutableMapOf<Position3D, HelperMapTile>()
        area.blocks.forEach { (key, value) ->
            val directionBlocked = { x: Position3D -> area.blocks[key + x].let { if (it == null) true else Pathfinding.doorOpening(it) } }
            if(directionBlocked(Position3D.defaultPosition())) {
                helperMap[key] = HelperMapTile(HelperMapTileType.Wall)
            }
            else if(directionBlocked(Position3D.create(1,0,0)) && directionBlocked(Position3D.create(-1,0,0))
                || (directionBlocked(Position3D.create(0,1,0)) && directionBlocked(Position3D.create(0,-1,0)))) {
                helperMap[key] = HelperMapTile (HelperMapTileType.Corridor)
            } else
                helperMap[key] = HelperMapTile (HelperMapTileType.Room)
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