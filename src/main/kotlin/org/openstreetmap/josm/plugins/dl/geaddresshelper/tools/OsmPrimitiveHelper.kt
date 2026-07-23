package org.openstreetmap.josm.plugins.dl.geaddresshelper.tools

import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.data.osm.Node

object OsmPrimitiveHelper {

    fun createNode(eastNorth: EastNorth, tags: Map<String, String>): Node {
        val node = Node(GeometryHelper.getNodePlacement(eastNorth, 0))
        node.putAll(tags)
        return node
    }
}