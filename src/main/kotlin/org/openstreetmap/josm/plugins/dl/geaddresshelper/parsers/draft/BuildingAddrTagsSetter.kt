//package org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers
//
//import org.openstreetmap.josm.command.Command
//import org.openstreetmap.josm.data.osm.OsmPrimitive
//import org.openstreetmap.josm.data.osm.TagMap
//import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.CommandHelper
//import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.TagCreator
//import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.TagCreator.TagType.BUILDING
//
//class BuildingAddrTagsSetter(
//    val osmPrimitive: OsmPrimitive,
//    val tags: TagMap,
//    val matchedOsmStreetName: String,
//) : CommandCreator {
//
//  //  fun osmPrimitive(op: OsmPrimitive) = apply { this.osmPrimitive = op }
//  //  fun tags(tags: TagMap) = apply { this.tags = tags }
//
//  override fun create(): List<Command> {
//    // Здесь ваша логика создания команды
//    val tags: Map<String, String> =
//        TagCreator.create(
//            BUILDING,
//            matchedOsmStreetName, // ??
//            mrResult.second.parsedAddressList[0],
//            mrResult.second.usefulNaprStrings,
//        )
//    val chBuildingCommands = CommandHelper.toChangeCommands(tags, mrResult.second.osmPrimitive)
//  }
//}
