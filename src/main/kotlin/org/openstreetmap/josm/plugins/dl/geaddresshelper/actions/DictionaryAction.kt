package org.openstreetmap.josm.plugins.dl.geaddresshelper.actions

import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import org.openstreetmap.josm.actions.JosmAction
import org.openstreetmap.josm.command.Command
import org.openstreetmap.josm.command.SequenceCommand
import org.openstreetmap.josm.data.UndoRedoHandler
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.OsmDataManager
import org.openstreetmap.josm.gui.MainApplication
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.CommandHelper
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.dataset.getNamedStreets
import org.openstreetmap.josm.plugins.dl.geaddresshelper.validation.vocabulary.StreetDictionary
import org.openstreetmap.josm.tools.I18n
import org.openstreetmap.josm.tools.Shortcut

class DictionaryAction :
    JosmAction(
        ACTION_NAME,
        ICON_NAME,
        null,
        Shortcut.registerShortcut(
            "data:napr_dict",
            I18n.tr("Data: {0}", I18n.tr(ACTION_NAME)),
            KeyEvent.KEY_LOCATION_UNKNOWN,
            Shortcut.NONE,
        ),
        false,
    ) {
  companion object {
    val ACTION_NAME = I18n.tr("From dictionary")
    val ICON_NAME = "g_dict.svg"
  }

  override fun updateEnabledState() {
    isEnabled =
        MainApplication.isDisplayingMapView() &&
            MainApplication.getMap().mapView.isActiveLayerDrawable
  }

  override fun actionPerformed(e: ActionEvent?) {
    val dataSet: DataSet = OsmDataManager.getInstance().editDataSet ?: return
    val namedStreet = dataSet.getNamedStreets()
    val commands: MutableList<Command> = mutableListOf()
    for (street in namedStreet) {
      val name: String? = street.get("name")
      val nameKa: String? = street.get("name:ka")
      val nameEn: String? = street.get("name:en")
      val nameRu: String? = street.get("name:ru")
      if (nameKa == null || nameEn == null || nameRu == null) {
        val tags = mutableMapOf<String, String>()
        val found = StreetDictionary.streets.get(name)
        if (found != null) {
          if (nameKa == null) tags.put("name:ka", found.nameKa)
          if (nameEn == null) tags.put("name:en", found.nameEn)
          if (nameRu == null) tags.put("name:ru", found.nameRu)
        }
        val changeBuildingCommands = CommandHelper.toChangeCommands(tags, street)
        commands.addAll(changeBuildingCommands)
      }
    }
    if (commands.isNotEmpty()) {
      val command: Command =
          SequenceCommand(
              I18n.tr("Added from GeorgiaAddressHelper"),
              commands,
          )
      UndoRedoHandler.getInstance().add(command)
    }
  }
}
