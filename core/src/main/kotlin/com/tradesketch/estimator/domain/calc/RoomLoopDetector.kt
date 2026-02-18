package com.tradesketch.estimator.domain.calc

import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.WallSegment
import kotlin.math.roundToLong

object RoomLoopDetector {
    fun detectRooms(
        walls: List<WallSegment>,
        snapThresholdMm: Long = 25L
    ): List<Room> {
        if (walls.size < 3) return emptyList()

        val nodes = mutableListOf<MutableList<PointMm>>()
        fun resolveNode(point: PointMm): Int {
            val existing = nodes.indexOfFirst { cluster ->
                cluster.any { BlueprintSnapMath.distanceMillimeters(it, point) <= snapThresholdMm }
            }
            if (existing >= 0) {
                nodes[existing].add(point)
                return existing
            }
            nodes.add(mutableListOf(point))
            return nodes.lastIndex
        }

        data class Edge(val wall: WallSegment, val a: Int, val b: Int)
        val edges = walls.map { wall ->
            val a = resolveNode(wall.start)
            val b = resolveNode(wall.end)
            Edge(wall, a, b)
        }.filter { it.a != it.b }
        if (edges.size < 3) return emptyList()

        val nodeCentroids = nodes.map { cluster ->
            val avgX = cluster.map { it.x }.average().roundToLong()
            val avgY = cluster.map { it.y }.average().roundToLong()
            PointMm(avgX, avgY)
        }
        val adjacency = mutableMapOf<Int, MutableList<Int>>()
        val edgeIds = mutableMapOf<Pair<Int, Int>, MutableList<String>>()
        edges.forEach { edge ->
            adjacency.getOrPut(edge.a) { mutableListOf() }.add(edge.b)
            adjacency.getOrPut(edge.b) { mutableListOf() }.add(edge.a)
            edgeIds.getOrPut(edge.a to edge.b) { mutableListOf() }.add(edge.wall.id)
            edgeIds.getOrPut(edge.b to edge.a) { mutableListOf() }.add(edge.wall.id)
        }

        val visited = mutableSetOf<Int>()
        val rooms = mutableListOf<Room>()
        var roomIndex = 1

        adjacency.keys.forEach { startNode ->
            if (startNode in visited) return@forEach
            val componentNodes = mutableListOf<Int>()
            val queue = ArrayDeque<Int>()
            queue.add(startNode)
            visited.add(startNode)
            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()
                componentNodes.add(node)
                adjacency[node].orEmpty().forEach { neighbor ->
                    if (visited.add(neighbor)) {
                        queue.addLast(neighbor)
                    }
                }
            }

            if (componentNodes.size < 3) return@forEach
            val degrees = componentNodes.map { adjacency[it].orEmpty().size }
            if (degrees.any { it != 2 }) return@forEach
            val componentEdgeCount = componentNodes.sumOf { adjacency[it].orEmpty().size } / 2
            if (componentEdgeCount != componentNodes.size) return@forEach

            val orderedNodes = mutableListOf<Int>()
            val usedEdges = mutableSetOf<Pair<Int, Int>>()
            var current = componentNodes.first()
            var previous: Int? = null
            while (true) {
                orderedNodes.add(current)
                val next = adjacency[current]
                    .orEmpty()
                    .firstOrNull { neighbor ->
                        val edgeKey = if (current < neighbor) current to neighbor else neighbor to current
                        edgeKey !in usedEdges && neighbor != previous
                    } ?: break
                val edgeKey = if (current < next) current to next else next to current
                usedEdges.add(edgeKey)
                previous = current
                current = next
                if (current == orderedNodes.first()) break
                if (orderedNodes.size > componentNodes.size + 1) break
            }
            if (orderedNodes.size < 3) return@forEach

            val polygon = orderedNodes.map { nodeCentroids[it] }
            val cyclePairs = orderedNodes.zip(orderedNodes.drop(1) + orderedNodes.first())
            val wallIds = cyclePairs.mapNotNull { (a, b) ->
                edgeIds[a to b]?.firstOrNull()
            }
            rooms += Room(
                id = "room-auto-$roomIndex",
                name = "Room $roomIndex",
                polygon = polygon,
                wallSegmentIds = wallIds
            )
            roomIndex += 1
        }
        return rooms
    }
}
