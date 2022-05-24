package cz.cuni.gamedev.nail123.roguelike.world.worlds

import cz.cuni.gamedev.nail123.roguelike.GameConfig
import cz.cuni.gamedev.nail123.roguelike.blocks.Floor
import cz.cuni.gamedev.nail123.roguelike.blocks.Wall
import cz.cuni.gamedev.nail123.roguelike.entities.objects.Door
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
    // if type is room, centerPoint is one position in the entire room, same for corridor; centerPoint is used like an id for whole corridors and rooms
    data class HelperMapTile(val type: HelperMapTileType, var centerPoint: Position3D? = null)
    data class GetSameNeighbouringTilesResult(val tiles: List<Position3D>, val neighboursOfTiles: List<Position3D>)
    data class Edge(val roomInCenter: Position3D, val edgeCenter: Position3D, val roomOutCenter: Position3D)


    // First result are all the tiles with same type that can be reached from position by tiles with same type, next result is list of neighbours of the first tiles
    private fun getSameNeighbouringTiles(helperMap: Map<Position3D, HelperMapTile>, position: Position3D, canGoThroughCorridors: Boolean = false): GetSameNeighbouringTilesResult {
        val tileType = helperMap[position]?.type
        val openVertices = ArrayDeque<Position3D>()
        val visitedVertices = mutableSetOf<Position3D>()
        openVertices.add(position)
        visitedVertices.add(position)
        val neighbouringVertices = mutableSetOf<Position3D>()

        while (openVertices.isNotEmpty()) {
            val currentPos = openVertices.pollFirst()
            val canConnectThroughTile = { it: Position3D -> helperMap[it]?.type == tileType || (canGoThroughCorridors && helperMap[it]?.type == HelperMapTileType.Corridor) }
            Pathfinding.fourDirectional(currentPos)

                .filter { canConnectThroughTile(it) && !visitedVertices.contains(it) }
                .forEach {
                    openVertices.addLast(it)
                    visitedVertices.add(it)
                }
            Pathfinding.fourDirectional(currentPos)
                .filter { helperMap[it] != null && !canConnectThroughTile(it)}
                .forEach {
                    neighbouringVertices.add(it)
                }
        }

        return GetSameNeighbouringTilesResult(visitedVertices.toList(), neighbouringVertices.toList())
    }

    private fun makeHelperMap(area: WFCAreaBuilder): MutableMap<Position3D, HelperMapTile> {
        val helperMap = mutableMapOf<Position3D, HelperMapTile>()
        area.blocks.forEach { (key, value) ->
            val directionBlocked = { x: Position3D -> area.blocks[key + x].let { if (it == null) true else Pathfinding.doorOpening(it) } }
            if(directionBlocked(Position3D.defaultPosition())) {
                helperMap[key] = HelperMapTile(HelperMapTileType.Wall)
            }
            // corridors are empty blocks with walls on opposite sides
            else if(directionBlocked(Position3D.create(1,0,0)) && directionBlocked(Position3D.create(-1,0,0))
                || (directionBlocked(Position3D.create(0,1,0)) && directionBlocked(Position3D.create(0,-1,0)))) {
                helperMap[key] = HelperMapTile (HelperMapTileType.Corridor)
            } else
                helperMap[key] = HelperMapTile (HelperMapTileType.Room)
        }
        return helperMap
    }

    private fun addCenterPoints(helperMap: MutableMap<Position3D, HelperMapTile>) {
        helperMap.forEach {  (key,value) ->
            if(value.type == HelperMapTileType.Wall || value.centerPoint != null)
                return@forEach
            val (tiles, _) = getSameNeighbouringTiles(helperMap, key)
            tiles.forEach {
                helperMap[it]?.centerPoint = key
            }
        }
    }

    // corridors that connect other number of rooms than 2 are discarded
    // result is a map from rooms into edges for better graph traversal
    private fun removeComplicatedCorridorsAndConstructRoomGraph(helperMap: MutableMap<Position3D, HelperMapTile>): MutableMap<Position3D, MutableList<Edge>> {
        val result = mutableMapOf<Position3D, MutableList<Edge>>()
        helperMap.filter { it.value.type == HelperMapTileType.Room }.map { it.value.centerPoint!! }.distinct().forEach {
            result[it] = mutableListOf()
        }

        helperMap.keys.filter { helperMap[it]?.type == HelperMapTileType.Corridor }.map { helperMap[it]?.centerPoint!! }.distinct().forEach {
            val (tiles, neighbours) = getSameNeighbouringTiles(helperMap, it)
            val roomNeighbours = neighbours.filter { x -> helperMap[x]!!.type == HelperMapTileType.Room }.map { x -> helperMap[x]!!.centerPoint }.distinct()
            if(roomNeighbours.size != 2) {
                // corridor is weird, remove it
                tiles.forEach { x -> helperMap[x] = HelperMapTile(HelperMapTileType.Wall) }
            } else {
                result[roomNeighbours[0]]!!.add(Edge(roomNeighbours[0]!!, it, roomNeighbours[1]!!))
                result[roomNeighbours[1]]!!.add(Edge(roomNeighbours[1]!!, it, roomNeighbours[0]!!))
            }
        }

        return result
    }

    // removes all cycles of length <= 3
    private fun removeCycles(helperMap: MutableMap<Position3D, HelperMapTile>, edges: MutableMap<Position3D, MutableList<Edge>>) {
        // not the best complexity but should be enough for 3 cycles

        var paths = edges.values.flatten().map{ listOf(it) }
        val maxCycleLength = 3

        val extendsPathsByOne = { paths: List<List<Edge>> ->
            paths.flatMap {
                edges[it.last().roomOutCenter]!!.map { x -> listOf(it, listOf(x)).flatten() }
            }
        }

        for (i in 0 until maxCycleLength-1) {
            paths = extendsPathsByOne(paths)
        }
        paths = paths.flatMap { (2..it.size).map {x -> it.slice(0 until x)} }
        val shuffledPaths = paths.toMutableList()
        shuffledPaths.shuffle()

        val edgesToRemove = mutableSetOf<Position3D>()
        shuffledPaths.forEach {
            val pathPoints = listOf(it[0].roomInCenter) + it.map { x -> x.roomOutCenter }
            if(pathPoints.distinct().size < pathPoints.size) {
                val edgePoints = it.map { x -> x.edgeCenter }
                if(edgePoints.distinct().size == edgePoints.size && edgesToRemove.intersect(edgePoints.toSet()).isEmpty()) {
                    // breaking cycle
                    edgesToRemove.add(edgePoints[0])
                }
            }
        }

        edgesToRemove.forEach {
            val (tiles, _) = getSameNeighbouringTiles(helperMap, it)
            tiles.forEach { x -> helperMap[x] = HelperMapTile(HelperMapTileType.Wall) }
        }
        edges.keys.forEach {
            edges[it] = edges[it]!!.filter { x -> !edgesToRemove.contains(x.edgeCenter) }.toMutableList()
        }
    }


    private fun connectEmptyRooms(helperMap: MutableMap<Position3D, HelperMapTile>, edges: MutableMap<Position3D, MutableList<Edge>>) {
        val directions = listOf(
            Position3D.create(0, -1, 0),
            Position3D.create(1, 0, 0),
            Position3D.create(0, 1, 0),
            Position3D.create(-1, 0, 0),
        )
        val firstPoint = edges.keys.random()
        val connectedPoints = mutableSetOf<Position3D>()
        val pointsWaitingToConnectToOthers = mutableSetOf(firstPoint)

        // when doing this for the first time, it creates a corridor of zero length which is a bit hacky
        while(pointsWaitingToConnectToOthers.isNotEmpty()) {
            var tile = pointsWaitingToConnectToOthers.random()

            pointsWaitingToConnectToOthers.remove(tile)

            for(directionIndex in directions.indices) {
                val direction = directions[directionIndex]
                var neighbouringDirections = listOf(
                    directions[(directionIndex + 1) % directions.size],
                    directions[(directionIndex + directions.size - 1) % directions.size]
                )
                neighbouringDirections += neighbouringDirections.map { it + it }

                // ensure corridor can be made, corridor also needs 2 walls on the right and left from it
                var position = tile + direction
                var wallPositions = mutableListOf<Position3D>()
                while (helperMap[position]?.type == HelperMapTileType.Wall && !neighbouringDirections.any {
                        val type =
                            helperMap[position + it]?.type; type == HelperMapTileType.Room || type == HelperMapTileType.Corridor
                    }) {
                    wallPositions.add(position)
                    position += direction
                }
                // if corridor leads to already connected part or can't be finished
                if (helperMap[position]?.type != HelperMapTileType.Room || connectedPoints.contains(position))
                    continue

                // safe to make wall
                for(wallPosition in wallPositions) {
                    helperMap[wallPosition] = HelperMapTile(HelperMapTileType.Corridor, wallPositions.first())
                }
                // add all room tiles
                val (tiles, _) = getSameNeighbouringTiles(helperMap, position, true)
                connectedPoints.addAll(tiles.filter { helperMap[position]?.type == HelperMapTileType.Room })
                pointsWaitingToConnectToOthers.addAll(tiles)
                break

            }
        }

        // remove empty rooms
        for((key, value) in helperMap) {
            if(value.type == HelperMapTileType.Room && !connectedPoints.contains(key)) {
                val (tiles, _) = getSameNeighbouringTiles(helperMap, key, true)
                connectedPoints.addAll(tiles.filter { helperMap[it]?.type == HelperMapTileType.Room })
                for(tile in tiles)
                    helperMap[tile] = HelperMapTile(HelperMapTileType.Wall)
            }
        }
    }

    override fun buildLevel(floor: Int): Area {
        val areaBuilder = WFCAreaBuilder(GameConfig.AREA_SIZE).create()

        val helperMap = makeHelperMap(areaBuilder)
        addCenterPoints(helperMap)
        val roomGraph = removeComplicatedCorridorsAndConstructRoomGraph(helperMap)
        removeCycles(helperMap, roomGraph)
        connectEmptyRooms(helperMap, roomGraph)

        // update areaBuilder
        areaBuilder.blocks.forEach { (key, value) ->
            if(helperMap[key]?.type == HelperMapTileType.Wall) {
                areaBuilder.blocks[key] = Wall()
            } else if (helperMap[key]?.type == HelperMapTileType.Room) {
                areaBuilder.blocks[key] = Floor()
            }
            else if (helperMap[key]?.type == HelperMapTileType.Corridor) {
                if(Pathfinding.fourDirectional(key).any { helperMap[it]?.type == HelperMapTileType.Room } ) {
                    areaBuilder.blocks[key] = Floor().apply { entities.add(Door()) }
                } else
                    areaBuilder.blocks[key] = Floor()
            }

        }

        areaBuilder.addAtEmptyPosition(
            areaBuilder.player,
            Position3D.create(0, 0, 0),
            GameConfig.VISIBLE_SIZE
        )

        areaBuilder.addEntity(FogOfWar(), Position3D.unknown())

        // Add stairs up
        if (floor > 0) areaBuilder.addEntity(Stairs(false), areaBuilder.player.position)

        // Add stairs down
        val floodFill = Pathfinding.floodFill(areaBuilder.player.position, areaBuilder)
        val maxDistance = floodFill.values.maxOrNull()!!
        val staircasePosition = floodFill.filter { it.value > maxDistance / 2 }.keys.random()
        areaBuilder.addEntity(Stairs(), staircasePosition)

        return areaBuilder.build()
    }
}