package org.openstreetmap.josm.plugins.dl.geaddresshelper.uploadhooks

import org.openstreetmap.josm.actions.upload.UploadHook
import org.openstreetmap.josm.command.ChangePropertyCommand
import org.openstreetmap.josm.command.Command
import org.openstreetmap.josm.command.SequenceCommand
import org.openstreetmap.josm.data.APIDataSet
import org.openstreetmap.josm.data.UndoRedoHandler
import org.openstreetmap.josm.data.osm.IPrimitive
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.TempRemoverHelper
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.TempRemoverHelper.TEMP_TAGS
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.TempRemoverHelper.setToNull
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.dataset.containsTmpTags
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.dataset.getForDelete
import org.openstreetmap.josm.tools.I18n

class UploadFilter : UploadHook {
    override fun checkUpload(apiDataSet: APIDataSet): Boolean {

        val forDelete = apiDataSet.primitivesToAdd.getForDelete()

        if (forDelete.isNotEmpty()) {
            // удаляем данные помеченные к удалению, вместе со связанными, из датасета
            val (nodesToDelete, waysToDelete, relationsToDelete, nodesToNotUpload) = TempRemoverHelper.prepareData(forDelete)
            val delCommands: List<Command> =
                TempRemoverHelper.toDeleteCommands(nodesToDelete, waysToDelete, relationsToDelete, nodesToNotUpload)
            val command: Command = SequenceCommand(I18n.tr("Node deleted"), delCommands)
            UndoRedoHandler.getInstance().add(command)

            // remove from upload data set
            apiDataSet.removeProcessed(nodesToNotUpload as Collection<IPrimitive>?)
            apiDataSet.removeProcessed(waysToDelete as Collection<IPrimitive>?)
            apiDataSet.removeProcessed(relationsToDelete as Collection<IPrimitive>?)

//          Logging.info(
//            "EGRN-PLUGIN Upload filter removed some unneeded objects (nodes: ${nodesToDelete.size}, ways: ${waysToDelete.size}, relations: ${relationsToDelete.size})"
//          )
        }

        if (apiDataSet.containsTmpTags()) {
            val removeTagsCommand =
                SequenceCommand(
                    I18n.tr("Temp tags removed"),
                    ChangePropertyCommand(apiDataSet.primitives, setToNull(TEMP_TAGS))
                )
            UndoRedoHandler.getInstance().add(removeTagsCommand)
//          Logging.info("EGRN-PLUGIN Upload filter removed some unneeded tags")
        }

        return true
    }
}
