package org.openstreetmap.josm.plugins.dl.geaddresshelper.tools

import org.openstreetmap.josm.command.ChangePropertyCommand
import org.openstreetmap.josm.command.Command
import org.openstreetmap.josm.data.osm.OsmPrimitive

object CommandHelper {

    fun toChangeCommands(tags: Map<String, String>, osmPrimitive: OsmPrimitive): List<Command> {
        val commands: MutableList<Command> = mutableListOf()
        for (tag in tags) {
            if (!osmPrimitive.hasTag(tag.key) || TagHelper.isOverwriteEnabled(tag.key, osmPrimitive.get(tag.key))) {
                commands.add(ChangePropertyCommand(osmPrimitive, tag.key, tag.value))
            }
        }
        return commands
    }

    fun toChangeCommandsSimple(tags: Map<String, String>, osmPrimitives: List<OsmPrimitive>): List<Command> {
        val commands: MutableList<Command> = mutableListOf()
        for (osmPrimitive in osmPrimitives) {
            toChangeCommandsSimple(tags, osmPrimitive).forEach { commands.add(it) }
        }
        return commands
    }

    fun toChangeCommandsSimple(tags: Map<String, String>, osmPrimitive: OsmPrimitive): List<Command> {
        val commands: MutableList<Command> = mutableListOf()
        for (tag in tags) {
                commands.add(ChangePropertyCommand(osmPrimitive, tag.key, tag.value))
        }
        return commands
    }
}