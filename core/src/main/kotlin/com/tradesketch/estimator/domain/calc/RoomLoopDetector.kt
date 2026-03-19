package com.tradesketch.estimator.domain.calc

import com.tradesketch.estimator.domain.model.BlueprintDocument
import com.tradesketch.estimator.domain.model.PointMm
import com.tradesketch.estimator.domain.model.Room
import com.tradesketch.estimator.domain.model.WallSegment
import kotlin.math.abs
import kotlin.math.atan2
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

        val adjacencyDistinct = adjacency.mapValues { (_, neighbors) ->
            neighbors.distinct()
        }
        val sortedNeighbors = adjacencyDistinct.mapValues { (node, neighbors) ->
            neighbors.sortedBy { neighbor ->
                atan2(
                    (nodeCentroids[neighbor].y - nodeCentroids[node].y).toDouble(),
                    (nodeCentroids[neighbor].x - nodeCentroids[node].x).toDouble()
                )
            }
        }

        val componentByNode = mutableMapOf<Int, Int>()
        val visitedNodes = mutableSetOf<Int>()
        var componentIndex = 0
        adjacencyDistinct.keys.forEach { startNode ->
            if (!visitedNodes.add(startNode)) return@forEach
            val queue = mutableListOf<Int>()
            queue.add(startNode)
            componentByNode[startNode] = componentIndex
            while (queue.isNotEmpty()) {
                val node = queue.removeAt(0)
                adjacencyDistinct[node].orEmpty().forEach { neighbor ->
                    if (visitedNodes.add(neighbor)) {
                        componentByNode[neighbor] = componentIndex
                        queue.add(neighbor)
                    }
                }
            }
            componentIndex += 1
        }

        data class TracedFace(
            val nodes: List<Int>,
            val directedEdges: List<Pair<Int, Int>>
        )

        fun traceFace(startDirected: Pair<Int, Int>): TracedFace? {
            val nodesTrace = mutableListOf<Int>()
            val localEdges = mutableListOf<Pair<Int, Int>>()
            val localEdgeSet = mutableSetOf<Pair<Int, Int>>()
            var from = startDirected.first
            var to = startDirected.second
            val hardLimit = (edges.size * 2).coerceAtLeast(8)
            while (localEdges.size <= hardLimit) {
                val directed = from to to
                if (!localEdgeSet.add(directed)) return null
                localEdges += directed
                if (nodesTrace.isEmpty()) {
                    nodesTrace += from
                }
                nodesTrace += to

                val neighbors = sortedNeighbors[to].orEmpty()
                if (neighbors.size < 2) return null
                val incomingIndex = neighbors.indexOf(from)
                if (incomingIndex < 0) return null
                val next = neighbors[(incomingIndex - 1 + neighbors.size) % neighbors.size]
                val nextDirected = to to next
                if (nextDirected == startDirected) {
                    val cycleNodes = nodesTrace.dropLast(1)
                    if (cycleNodes.size < 3) return null
                    return TracedFace(nodes = cycleNodes, directedEdges = localEdges)
                }
                from = to
                to = next
            }
            return null
        }

        val directedEdges = edges
            .flatMap { edge -> listOf(edge.a to edge.b, edge.b to edge.a) }
            .distinct()
        val visitedDirectedEdges = mutableSetOf<Pair<Int, Int>>()
        val seenCycles = mutableSetOf<String>()
        val rooms = mutableListOf<Room>()
        var roomIndex = 1

        directedEdges.forEach { directedStart ->
            if (directedStart in visitedDirectedEdges) return@forEach
            val traced = traceFace(directedStart) ?: return@forEach
            visitedDirectedEdges += traced.directedEdges

            val polygon = traced.nodes.map { nodeCentroids[it] }
            val area = polygonSignedArea(polygon)
            if (area <= 0.0 || area < 20_000.0) return@forEach

            val componentId = componentByNode[traced.nodes.first()] ?: return@forEach
            val cycleKey = "$componentId:${canonicalCycleKey(traced.nodes)}"
            if (!seenCycles.add(cycleKey)) return@forEach

            val cyclePairs = traced.nodes.zip(traced.nodes.drop(1) + traced.nodes.first())
            val wallIds = cyclePairs.mapNotNull { (a, b) ->
                edgeIds[a to b]?.firstOrNull() ?: edgeIds[b to a]?.firstOrNull()
            }.distinct()
            if (wallIds.size < 3) return@forEach

            rooms += Room(
                id = "room-auto-$roomIndex",
                name = "Room $roomIndex",
                polygon = polygon,
                wallSegmentIds = wallIds,
                wallLoopRef = wallIds
            )
            roomIndex += 1
        }
        return rooms
    }

    fun detectRooms(document: BlueprintDocument, snapThresholdMm: Long = 25L): List<Room> {
        return detectRooms(walls = document.walls, snapThresholdMm = snapThresholdMm)
    }

    fun detectAndMerge(document: BlueprintDocument, snapThresholdMm: Long = 25L): BlueprintDocument {
        val detected = detectRooms(document, snapThresholdMm)
        if (detected.isEmpty()) return document
        val merged = mergeUniqueRooms(document.rooms, detected)
        return document.copy(rooms = merged)
    }

    private fun mergeUniqueRooms(existing: List<Room>, detected: List<Room>): List<Room> {
        if (existing.isEmpty()) return detected
        val merged = existing.toMutableList()
        detected.forEach { candidate ->
            val duplicate = existing.any { known ->
                sameLoop(known.polygon, candidate.polygon)
            }
            if (!duplicate) {
                merged += candidate
            }
        }
        return merged
    }

    private fun sameLoop(a: List<PointMm>, b: List<PointMm>): Boolean {
        if (a.size != b.size || a.isEmpty()) return false
        val normalizedA = a.sortedWith(compareBy<PointMm> { it.x }.thenBy { it.y })
        val normalizedB = b.sortedWith(compareBy<PointMm> { it.x }.thenBy { it.y })
        return normalizedA.zip(normalizedB).all { (left, right) ->
            abs(left.x - right.x) <= 2L && abs(left.y - right.y) <= 2L
        }
    }

    private fun polygonSignedArea(points: List<PointMm>): Double {
        if (points.size < 3) return 0.0
        var area = 0.0
        points.indices.forEach { i ->
            val a = points[i]
            val b = points[(i + 1) % points.size]
            area += (a.x.toDouble() * b.y.toDouble()) - (b.x.toDouble() * a.y.toDouble())
        }
        return area / 2.0
    }

    private fun canonicalCycleKey(nodes: List<Int>): String {
        if (nodes.isEmpty()) return ""
        val forward = canonicalRotationKey(nodes)
        val reversed = canonicalRotationKey(nodes.reversed())
        return if (forward <= reversed) forward else reversed
    }

    private fun canonicalRotationKey(nodes: List<Int>): String {
        val n = nodes.size
        var best: String? = null
        for (shift in 0 until n) {
            val key = buildString {
                for (index in 0 until n) {
                    if (index > 0) append('-')
                    append(nodes[(index + shift) % n])
                }
            }
            if (best == null || key < best) {
                best = key
            }
        }
        return best ?: ""
    }
}
