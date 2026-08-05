package org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers

enum class ActionType {
  /** got one address, street matched */
  ADD_ADDR_TAGS,
  /** got one address, but street not found in osm */
  CREATE_NODE,
  /** got multiple addresses */
  CREATE_MULTIPLE_ADDR_NODE,
  /** got double address */
  CREATE_NODE_DOUBLE
}
