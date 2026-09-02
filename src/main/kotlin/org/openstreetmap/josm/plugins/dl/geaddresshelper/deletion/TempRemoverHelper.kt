package org.openstreetmap.josm.plugins.dl.geaddresshelper.deletion

import org.openstreetmap.josm.command.Command
import org.openstreetmap.josm.command.DeleteCommand
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.Relation
import org.openstreetmap.josm.data.osm.Way

object TempRemoverHelper {

    data class ForDeleteDto(
        val nodesToDelete: MutableList<Node>,
        val waysToDelete: MutableList<Way>,
        val relationsToDelete: MutableList<Relation>,
        val nodesToNotUpload: MutableList<Node>
    ) {
        companion object {
            fun empty(): ForDeleteDto {
                return ForDeleteDto(mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf())
            }
        }
    }

    val TEMP_TAGS: Collection<String> = setOf(
        "warn:1",
        "napr:addr:raw:1",
        "napr:addr:raw:2",
        "napr:addr:raw:3",
        "napr:addr:raw:4",
        "napr:addr:raw:5",
        "napr:addr:raw:6",
        "napr:addr:raw:7",
        "napr:addr:raw:8",
        "addr:GE:napr",
        "napr:addr"
    )


    fun prepareData(toRemove: List<OsmPrimitive>): ForDeleteDto {
        if (toRemove.isNotEmpty()) {
            // удаляем данные помеченные к удалению, вместе со связанными, из датасета
            val nodesToDelete = toRemove.filterIsInstance<Node>().toMutableList()
            val allNodesToNotUpload = toRemove.filterIsInstance<Node>().toMutableList()
            val waysToDelete = toRemove.filterIsInstance<Way>().toMutableList()
            val relationsToDelete = toRemove.filterIsInstance<Relation>().toMutableList()

            relationsToDelete.forEach { rel ->
                rel.memberPrimitives.forEach { primitive ->
                    if (primitive is Node) nodesToDelete.add(primitive)
                    else waysToDelete.add(primitive as Way)
                }
            }
            waysToDelete.forEach { way -> allNodesToNotUpload.addAll(way.nodes.distinct()) }
            return ForDeleteDto(nodesToDelete, waysToDelete, relationsToDelete, allNodesToNotUpload)
        }
        return ForDeleteDto.empty()
    }

    fun toDeleteCommands(vararg primitives: MutableList<out OsmPrimitive>): List<Command> {
        val forDel = sequenceOf(*primitives)
            .flatten()
            .toList()
        return listOf(DeleteCommand.delete(forDel, true, false))
    }

    fun setToNull(tagKeys: Collection<String>): MutableMap<String, String?> {
        val map: MutableMap<String, String?> = HashMap()
        for (key in tagKeys) {
            map[key] = null
        }
        return map
    }

}