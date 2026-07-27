package org.openstreetmap.josm.plugins.dl.geaddresshelper.actions

import org.openstreetmap.josm.actions.JosmAction
import org.openstreetmap.josm.command.Command
import org.openstreetmap.josm.command.SequenceCommand
import org.openstreetmap.josm.data.UndoRedoHandler
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.OsmDataManager
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.OsmPrimitiveType
import org.openstreetmap.josm.gui.MainApplication
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.CommandHelper
import org.openstreetmap.josm.tools.I18n
import org.openstreetmap.josm.tools.Shortcut
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent

class NumberedStreetAction :
    JosmAction(
        ACTION_NAME,
        ICON_NAME,
        "Numbered street name generator",
        Shortcut.registerShortcut(
            "data:napr_num_street",
            I18n.tr("Data: {0}", I18n.tr(ACTION_NAME)),
            KeyEvent.KEY_LOCATION_UNKNOWN,
            Shortcut.NONE,
        ),
        false,
    ) {
  companion object {
    val ACTION_NAME = I18n.tr("NumberedStreetAction")
    val ICON_NAME = "123.svg"
  }

  override fun updateEnabledState() {
    isEnabled =
        MainApplication.isDisplayingMapView() &&
            MainApplication.getMap().mapView.isActiveLayerDrawable
  }

  override fun actionPerformed(e: ActionEvent?) {
    val dataSet: DataSet = OsmDataManager.getInstance().editDataSet ?: return
    showAndApply(dataSet.selected)
  }

  /** @return Список highway, у которых заполнен тег name */
  fun MutableCollection<OsmPrimitive>.getStreets(): List<OsmPrimitive> {
    return this.filter { p ->
      p.hasKey("highway") && p.type == OsmPrimitiveType.WAY
    }
  }

  /** @return Список highway, у которых заполнен тег name */
  fun MutableCollection<OsmPrimitive>.getBuildings(): List<OsmPrimitive> {
    return this.filter { p ->
      p !is Node && p.hasKey("building")
    }
  }

  fun showDialog(streetsSize: Int, buildingsSize: Int): Pair<Int, Map<String, String>> {
    val streetNameDialog =
        StreetNameDialog(MainApplication.getMainFrame(), streetsSize, buildingsSize)
    streetNameDialog.isVisible = true
    if (streetNameDialog.isApproved) {
      if (streetNameDialog.waysCheckbox.isSelected && streetNameDialog.buildingsCheckbox.isSelected)
          return Pair(30, toTags(streetNameDialog.resultTags))
      if (streetNameDialog.waysCheckbox.isSelected)
          return Pair(20, toTags(streetNameDialog.resultTags))
      if (streetNameDialog.buildingsCheckbox.isSelected)
          return Pair(10, toTags(streetNameDialog.resultTags))
    }
    return Pair(0, mapOf())
  }

  // todo переписать логики, чтобы из диалога возвращалась сразу мапа
  fun toTags(tagsText: String?): Map<String, String> {
    if (tagsText == null) return mapOf()
    return tagsText
        .trimIndent()
        .lineSequence() // Построчный разбор
        .map { it.trim() }
        .filter { it.isNotEmpty() && it.contains("=") }
        .associate { line ->
          val (key, value) = line.split("=", limit = 2)
          key.trim() to value.trim()
        }
  }

  fun showAndApply(primitives: MutableCollection<OsmPrimitive>) {
    val streets = primitives.getStreets()
    val buildings = primitives.getBuildings()

    val answer: Pair<Int, Map<String, String>> = showDialog(streets.size, buildings.size)
    val commands: MutableList<Command> = mutableListOf()
    val result = answer.first
    val tags = answer.second
    when (result) {
      // todo переделать на enum
      30 -> {
        val name = tags.get("name")
        commands.addAll(
            CommandHelper.toChangeCommandsSimple(mapOf("addr:street" to name!!), buildings)
        )
        commands.addAll(CommandHelper.toChangeCommandsSimple(tags, streets))
      }
      20 -> {
        commands.addAll(CommandHelper.toChangeCommandsSimple(tags, streets))
      }
      10 -> {
        val name = tags.get("name")
        commands.addAll(
            CommandHelper.toChangeCommandsSimple(mapOf("addr:street" to name!!), buildings)
        )
      }
      else -> {}
    }

    if (commands.isNotEmpty()) {
      val command: Command =
          SequenceCommand(
              I18n.tr("Added node from GeorgiaAddressHelper"),
              commands,
          )
      UndoRedoHandler.getInstance().add(command)
    }
  }
}
