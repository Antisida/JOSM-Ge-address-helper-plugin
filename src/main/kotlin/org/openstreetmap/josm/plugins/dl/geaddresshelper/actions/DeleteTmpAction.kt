package org.openstreetmap.josm.plugins.dl.geaddresshelper.actions

import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import org.openstreetmap.josm.actions.JosmAction
import org.openstreetmap.josm.command.ChangePropertyCommand
import org.openstreetmap.josm.command.Command
import org.openstreetmap.josm.command.SequenceCommand
import org.openstreetmap.josm.data.UndoRedoHandler
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.OsmDataManager
import org.openstreetmap.josm.gui.MainApplication
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.TagCreator.FIXME_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.TagCreator.REMOVE_ME
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.TempRemover
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.dataset.containsTmpTags
import org.openstreetmap.josm.tools.I18n
import org.openstreetmap.josm.tools.Logging
import org.openstreetmap.josm.tools.Shortcut

class DeleteTmpAction :
    JosmAction(
        ACTION_NAME,
        ICON_NAME,
        null,
        Shortcut.registerShortcut(
            "data:napr_delete",
            I18n.tr("Data: {0}", I18n.tr(ACTION_NAME)),
            KeyEvent.KEY_LOCATION_UNKNOWN,
            Shortcut.NONE,
        ),
        false,
    ) {
  companion object {
    val ACTION_NAME = I18n.tr("Delete temp")
    val ICON_NAME = "g_delete.svg"
  }

  override fun updateEnabledState() {
    isEnabled =
        MainApplication.isDisplayingMapView() &&
            MainApplication.getMap().mapView.isActiveLayerDrawable
  }

  override fun actionPerformed(e: ActionEvent?) {
    val dataSet: DataSet = OsmDataManager.getInstance().editDataSet ?: return
    val commands: MutableList<Command> = mutableListOf()
    val allPrimitives = dataSet.allNonDeletedCompletePrimitives().toList()
    val forRemove = allPrimitives.filter { it.hasTag(FIXME_TAG, REMOVE_ME) }
    val forDeleteDto = TempRemover.getForDelete(forRemove)
    if (forDeleteDto != null) {
      val delCommands = TempRemover.toDeleteCommands(forDeleteDto)
      commands.addAll(delCommands)
    }
    if (commands.isNotEmpty()) {
      val command: Command =
          SequenceCommand(
              I18n.tr("Deleted node from GeorgiaAddressHelper"),
              commands,
          )
      UndoRedoHandler.getInstance().add(command)
    }
    if (dataSet.containsTmpTags()) {
      val removeKeys =
          SequenceCommand(
              I18n.tr("Removed EGRN obsolete tags"),
              ChangePropertyCommand(
                  dataSet.allNonDeletedCompletePrimitives(),
                  TempRemover.getNullMap(),
              ),
          )
      UndoRedoHandler.getInstance().add(removeKeys)
      Logging.info("EGRN-PLUGIN Upload filter removed some unneeded tags")
    }
  }
}
