package org.openstreetmap.josm.plugins.dl.geaddresshelper.tools

import org.openstreetmap.josm.command.Command
import org.openstreetmap.josm.command.DeleteCommand
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.Relation
import org.openstreetmap.josm.data.osm.Way
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.dataset.TEMP_TAGS

object TempRemover {


  // val needsToRemove = apiDataSet.primitivesToAdd.filter {
  //    (it.hasTag("fixme", "REMOVE_ME!") || it.hasTag("fixme", "REMOVE ME!") ||
  //            (it.hasTag("fixme", "yes") && it.hasTag("source:addr", "ЕГРН"))) && it.isNew
  // }

  data class ForDeleteDto(
      val nodesToDelete: MutableList<Node>,
      val waysToDelete: MutableList<Way>,
      val relationsToDelete: MutableList<Relation>,
      val nodesToNotUpload: MutableList<Node>,
  )

  fun getForDelete(toRemove: List<OsmPrimitive>): ForDeleteDto? {
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
    return null
  }

  fun toDeleteCommands(forDelete: ForDeleteDto): List<Command> {
    val commands = mutableListOf<Command>()
    if (forDelete.relationsToDelete.isNotEmpty()) {
      val removeRelationsCommand = DeleteCommand.delete(forDelete.relationsToDelete)
      commands.add(removeRelationsCommand)
    }
    if (forDelete.waysToDelete.isNotEmpty()) {
      val removeWaysCommand = DeleteCommand.delete(forDelete.waysToDelete, true, false)
      commands.add(removeWaysCommand)
    }
    if (forDelete.nodesToDelete.isNotEmpty()) {
      val removeNodesCommand = DeleteCommand.delete(forDelete.nodesToDelete)
      commands.add(removeNodesCommand)
    }
    return commands
  }


//  val needsChange = apiDataSet.primitives.stream()
//    .flatMap { obj: OsmPrimitive -> obj.keys() }
//    .anyMatch { o: String -> discardableKeys.contains(o) }
//  if (needsChange) {
//    val map: MutableMap<String, String?> = HashMap()
//    for (key in discardableKeys) {
//      map[key] = null
//    }
//    val removeKeys = SequenceCommand(
//      I18n.tr("Removed EGRN obsolete tags"),
//      ChangePropertyCommand(apiDataSet.primitives, map)
//    )
//    UndoRedoHandler.getInstance().add(removeKeys)
//    Logging.info("EGRN-PLUGIN Upload filter removed some unneeded tags")
//  }

  fun getNullMap () : MutableMap<String, String?> {
    val map: MutableMap<String, String?> = HashMap()
    for (key in TEMP_TAGS) {
      map[key] = null
    }
    return map
  }


}
