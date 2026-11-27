package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.result.strategy

import org.openstreetmap.josm.command.AddCommand
import org.openstreetmap.josm.command.Command
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.OsmPrimitiveHelper
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Address
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.ParseResult

class CreateNodeStrategy : BaseResultStrategy {
    override fun createCommand(dataSet: DataSet, result: ParseResult): List<Command> {
        val tags = createTags(result)
        val node = OsmPrimitiveHelper.createNode(result.eastNorth, tags)
        return listOf(AddCommand(dataSet, node))
    }

    override fun executeAdditionally(result: ParseResult) {}        //do nothing

    override val additionalTagsFunction: (String?) -> Map<String, String> = { _ -> emptyMap() }

    override val tagCreationFunction: (List<String>, Address?, Map<String, String>) -> Map<String, String> = forNodeFunction

}