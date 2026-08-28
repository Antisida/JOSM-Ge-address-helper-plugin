package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr

import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.GeometryHelper

object OsmPrimitiveHelper {

    fun createNode(eastNorth: EastNorth, tags: Map<String, String>): Node {
        val node = Node(GeometryHelper.Companion.getNodePlacement(eastNorth, 0))
        node.putAll(tags)
        return node
    }
}