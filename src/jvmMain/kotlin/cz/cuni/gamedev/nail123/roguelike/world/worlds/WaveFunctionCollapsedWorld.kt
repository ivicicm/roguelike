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
    // if type is room, position is one position in the entire room, same for corridor
    data class HelperMapTile(val type: HelperMapTileType, var centerPoint: Position3D? = null)
    data class GetSameNeighbouringTilesResult(val tiles: List<Position3D>, val neighboursOfTiles: List<Position3D>)
    data class Edge(val pointFromCenter: Position3D, val edgeCenter: Position3D, val pointToCenter: Position3D)


    // First result are all the tiles with same type that can be reached from position by tiles with same type, next result is list of neighbours of the first tiles
    private fun getSameNeighbouringTiles(helperMap: Map<Position3D, HelperMapTile>, position: Position3D): GetSameNeighbouringTilesResult {
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

    private fun makeHelperMap(area: WFCAreaBuilder): MutableMap<Position3D, HelperMapTile> {
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

    private fun removeCycles(helperMap: MutableMap<Position3D, HelperMapTile>, edges: MutableMap<Position3D, MutableList<Edge>>) {
        // not the best complexity but should be enough for 3 cycles

        var paths = edges.values.flatten().map{ listOf(it) }
        val maxCycleLength = 3

        val extendsPathsByOne = { paths: List<List<Edge>> ->
            paths.map {
                edges[it.last().pointFromCenter]!!.map { x -> listOf(it, listOf(x)).flatten() }
            }.flatten()
        }

        for (i in 0 until maxCycleLength-1) {
            paths = extendsPathsByOne(paths)
        }
        paths = paths.map { (2..it.size).map {x -> it.slice(0 until x)} }.flatten()
        val shuffledPaths = paths.toMutableList()
        shuffledPaths.shuffle()

        val edgesToRemove = mutableSetOf<Position3D>()
        val edgesToRemoveData = mutableSetOf<Edge>()
        shuffledPaths.forEach {
            val pathPoints = listOf(it[0].pointFromCenter) + it.map {x -> x.pointToCenter }
            if(pathPoints.distinct().size < pathPoints.size) {
                val edgePoints = it.map { x -> x.edgeCenter }
                if(edgesToRemove.intersect(edgePoints.toSet()).isEmpty()) {
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
        val emptyRooms = edges.filter { it.value.size == 0 }.keys
        val directions = listOf(
            Position3D.create(0, 1, 0),
            Position3D.create(1, 0, 0),
            Position3D.create(0, -1, 0),
            Position3D.create(-1, 0, 0),
        )

        emptyRooms.forEach  emptyRooms@{ room ->
            val (tiles, _) = getSameNeighbouringTiles(helperMap, room)
            var tileConnected = false
            tiles.forEach { tile ->
                if(tileConnected)
                    return@forEach
                val wallDirections = directions.filter { helperMap[tile + it]?.type == HelperMapTileType.Wall }
                if(wallDirections.size != 1)
                    return@forEach

                // ensure wall can be made
                val direction = wallDirections[0]
                var position = tile + direction
                while(helperMap[position]?.type == HelperMapTileType.Wall) {
                    position += direction
                }
                if(helperMap[position]?.type != HelperMapTileType.Room || emptyRooms.contains(helperMap[position]?.centerPoint))
                    return@forEach

                // safe to make wall
                position = tile + direction
                val firstPosition = position
                while(helperMap[position]?.type == HelperMapTileType.Wall) {
                    helperMap[position] = HelperMapTile(HelperMapTileType.Corridor, firstPosition)
                    position += direction
                }
                edges[room]!!.add(Edge(room, firstPosition, helperMap[position]!!.centerPoint!!))
                tileConnected = true
            }
            if(!tileConnected) {
                // room can't be connected, remove it
                tiles.forEach {
                    helperMap[it] = HelperMapTile(HelperMapTileType.Wall)
                    edges.remove(room)
                }
            }
        }
    }

    override fun buildLevel(floor: Int): Area {
        val areaBuilder = WFCAreaBuilder(GameConfig.AREA_SIZE).create()

        val helperMap = makeHelperMap(areaBuilder)
        addCenterPoints(helperMap)
        val roomGraph = removeComplicatedCorridorsAndConstructRoomGraph(helperMap)
        removeCycles(helperMap, roomGraph)
//        connectEmptyRooms(helperMap, roomGraph)

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