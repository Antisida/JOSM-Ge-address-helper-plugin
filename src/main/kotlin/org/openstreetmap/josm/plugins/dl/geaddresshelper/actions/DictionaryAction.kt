package org.openstreetmap.josm.plugins.dl.geaddresshelper.actions

import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import org.openstreetmap.josm.actions.JosmAction
import org.openstreetmap.josm.command.Command
import org.openstreetmap.josm.command.SequenceCommand
import org.openstreetmap.josm.data.UndoRedoHandler
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.OsmDataManager
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.OsmPrimitiveType
import org.openstreetmap.josm.gui.MainApplication
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.CommandHelper
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.NAME_EN_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.NAME_KA_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.NAME_RU_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.NAME_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.vocabulary.StreetDictionary
import org.openstreetmap.josm.tools.I18n
import org.openstreetmap.josm.tools.Shortcut

class DictionaryAction :
    JosmAction(
        ACTION_NAME,
        ICON_NAME,
        "Set the 'name:ka', 'name:ru','name:en' for streets from the dictionary",
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
        const val ICON_NAME = "g_dict.svg"
    }

    override fun updateEnabledState() {
        isEnabled =
            MainApplication.isDisplayingMapView() &&
                    MainApplication.getMap().mapView.isActiveLayerDrawable
    }

    override fun actionPerformed(e: ActionEvent?) {
        val dataSet: DataSet = OsmDataManager.getInstance().editDataSet ?: return
        val selectedStreets =
            dataSet.selected.filter { p ->
                p.type == OsmPrimitiveType.WAY
                        && p.hasKey("highway")
                        && (p.hasKey(NAME_TAG) || p.hasKey(NAME_KA_TAG) || p.hasKey(NAME_RU_TAG) || p.hasKey(NAME_EN_TAG))
            }
        val commands: MutableList<Command> = mutableListOf()
        val changed: MutableList<OsmPrimitive> = mutableListOf()
        for (street in selectedStreets) {
            val name: String? = street.get(NAME_TAG)
            val nameKa: String? = street.get(NAME_KA_TAG)
            val nameEn: String? = street.get(NAME_EN_TAG)
            val nameRu: String? = street.get(NAME_RU_TAG)
            if (name == null || nameKa == null || nameEn == null || nameRu == null) {
                val tags = mutableMapOf<String, String>()
                val found = StreetDictionary.getFirstNotNullOrNull(name, nameKa, nameRu, nameEn)
                if (found != null) {
                    if (name == null) tags.put(NAME_TAG, found.name)
                    if (nameKa == null) tags.put(NAME_KA_TAG, found.nameKa)
                    if (nameEn == null) tags.put(NAME_EN_TAG, found.nameEn)
                    if (nameRu == null) tags.put(NAME_RU_TAG, found.nameRu)
                    changed.add(street)
                }
                val changeBuildingCommands = CommandHelper.toChangeCommands(tags, street)
                commands.addAll(changeBuildingCommands)
            }
        }
        if (commands.isNotEmpty()) {
            val command: Command = SequenceCommand(I18n.tr("Added by GeorgiaAddressHelper"), commands)
            UndoRedoHandler.getInstance().add(command)
            dataSet.setSelected(changed)
        }
    }
}
