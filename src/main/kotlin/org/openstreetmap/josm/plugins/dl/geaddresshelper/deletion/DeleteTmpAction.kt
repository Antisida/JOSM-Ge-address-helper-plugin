package org.openstreetmap.josm.plugins.dl.geaddresshelper.deletion

import org.openstreetmap.josm.actions.JosmAction
import org.openstreetmap.josm.command.ChangePropertyCommand
import org.openstreetmap.josm.command.Command
import org.openstreetmap.josm.command.SequenceCommand
import org.openstreetmap.josm.data.UndoRedoHandler
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.OsmDataManager
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.gui.MainApplication
import org.openstreetmap.josm.plugins.dl.geaddresshelper.deletion.TempRemoverHelper.TEMP_TAGS
import org.openstreetmap.josm.plugins.dl.geaddresshelper.deletion.TempRemoverHelper.setToNull
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.funs.containsTmpTags
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.funs.getForDelete
import org.openstreetmap.josm.tools.I18n
import org.openstreetmap.josm.tools.Shortcut
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent

class DeleteTmpAction :
    JosmAction(
        ACTION_NAME,
        ICON_NAME,
        "Delete temporary tags and objects",
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
        const val ICON_NAME = "g_delete.svg"
    }

    override fun updateEnabledState() {
        isEnabled =
            MainApplication.isDisplayingMapView() &&
                    MainApplication.getMap().mapView.isActiveLayerDrawable
    }

    override fun actionPerformed(e: ActionEvent?) {
        val dataSet: DataSet = OsmDataManager.getInstance().editDataSet ?: return
        val forDelete: List<OsmPrimitive> = dataSet.allNonDeletedCompletePrimitives().getForDelete()
        if (forDelete.isNotEmpty()) {
            // удаляем данные помеченные к удалению, вместе со связанными, из датасета
            val (nodesToDelete, waysToDelete, relationsToDelete, nodesToNotUpload) = TempRemoverHelper.prepareData(forDelete)
//            if (forDeleteDto != null) {
            val delCommands: List<Command> =
                TempRemoverHelper.toDeleteCommands(nodesToDelete, waysToDelete, relationsToDelete, nodesToNotUpload)
            val command: Command = SequenceCommand(I18n.tr("Node deleted"), delCommands)
            UndoRedoHandler.getInstance().add(command)
        }

        if (dataSet.containsTmpTags()) {
            val removeTagsCommand =
                SequenceCommand(
                    I18n.tr("Temp tags removed"),
                    ChangePropertyCommand(dataSet.allNonDeletedCompletePrimitives(), setToNull(TEMP_TAGS))
                )
            UndoRedoHandler.getInstance().add(removeTagsCommand)
//            Logging.info("EGRN-PLUGIN Upload filter removed some unneeded tags")
        }
    }


}