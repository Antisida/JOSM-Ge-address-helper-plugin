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
        "napr:warn",
        "napr:place",
        "napr:addr", //строка удачного парсинга
        //сырые данные
        "napr:raw:1",
        "napr:raw:2",
        "napr:raw:3",
        "napr:raw:4",
        "napr:raw:5",
        "napr:raw:6",
        "napr:raw:7",
        "napr:raw:8",
        "napr:raw:9",
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