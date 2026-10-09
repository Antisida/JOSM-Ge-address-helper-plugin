package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.actions

import org.openstreetmap.josm.actions.mapmode.MapMode
import org.openstreetmap.josm.command.AddCommand
import org.openstreetmap.josm.command.Command
import org.openstreetmap.josm.command.SequenceCommand
import org.openstreetmap.josm.data.UndoRedoHandler
import org.openstreetmap.josm.gui.MainApplication
import org.openstreetmap.josm.gui.MapFrame
import org.openstreetmap.josm.gui.util.KeyPressReleaseListener
import org.openstreetmap.josm.plugins.dl.geaddresshelper.dictionary.StreetDictionary
import org.openstreetmap.josm.plugins.dl.geaddresshelper.dictionary.StreetTranslation
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.OsmPrimitiveHelper
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator.TagType.NODE
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator.TagType.STREET
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.api.NaprClient
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.api.RawNaprDto
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.MainParser
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Address
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.CommandHelper
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.NAME_EN_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.NAME_KA_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.NAME_RU_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.funs.containsStreetsOnly
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.funs.getStreets
import org.openstreetmap.josm.tools.I18n
import org.openstreetmap.josm.tools.ImageProvider
import org.openstreetmap.josm.tools.Shortcut
import java.awt.Cursor
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

class ClickAction :
    MapMode(
        ACTION_NAME,
        ICON_NAME,
        "Get NAPR data for click location",
        Shortcut.registerShortcut(
            "data:napr_click",
            I18n.tr("Data: {0}", I18n.tr(ACTION_NAME)),
            KeyEvent.KEY_LOCATION_UNKNOWN,
            Shortcut.NONE,
        ),
        ImageProvider.getCursor("crosshair", "create_note"),
    ),
    KeyPressReleaseListener {

    companion object {
        val ACTION_NAME = I18n.tr("NAPR click")
        const val ICON_NAME = "g_click.svg"
    }

    override fun enterMode() {
        super.enterMode()
        val map = MainApplication.getMap()
        map.mapView.addMouseListener(this)
        map.keyDetector.addKeyListener(this)
    }

    override fun exitMode() {
        super.exitMode()
        val map = MainApplication.getMap()
        map.mapView.removeMouseListener(this)
        map.keyDetector.removeKeyListener(this)
    }

    override fun updateEnabledState() {
        isEnabled =
            MainApplication.isDisplayingMapView() &&
                    MainApplication.getMap().mapView.isActiveLayerDrawable
    }

    override fun mouseClicked(event: MouseEvent) {
        if (!SwingUtilities.isLeftMouseButton(event)) return

        val map: MapFrame = MainApplication.getMap()
        map.selectMapMode(map.mapModeSelect)

        val mapView = map.mapView
        if (!mapView.isActiveLayerDrawable) return

        mapView.setNewCursor(Cursor(Cursor.WAIT_CURSOR), this)

        val dataSet = layerManager.editDataSet
        val mouseEastNorth = mapView.getEastNorth(event.x, event.y)

        val naprDto: RawNaprDto? = NaprClient.executeRequest(mouseEastNorth)

        val dataString = naprDto?.getDataString()
        if (!dataString.isNullOrEmpty()) {
            val usefulString = naprDto.getUsefulString()
            val parsedAddresses: List<Address> = MainParser.parse(usefulString)//.singleOrNull()
            //todo тут приходят несколько адресов и их надо обрабатывать все, только нахрена?
            val commands: MutableList<Command> = mutableListOf()

            //create node
            val fullAddresses = parsedAddresses.filter { it.isSuccess }
            val tags = TagCreator.create(
                type = NODE,
                osmStreet = null,
                rawNaprString = naprDto.getDataString(),
                address = fullAddresses.singleOrNull(),
                additionalTags = emptyMap()
            )
            val node = OsmPrimitiveHelper.createNode(mouseEastNorth, tags)
            val sequenceCommand = SequenceCommand(I18n.tr("Node added"), AddCommand(dataSet, node))
            commands.add(sequenceCommand)

            //пытаемся присвоить улице имя
            var streetSuccess = false
            if (dataSet.selected.containsStreetsOnly()) {
                val distinctByStreet = parsedAddresses.distinctBy { it.street }
                if (distinctByStreet.size == 1) {
                    val found: StreetTranslation? = StreetDictionary.getByNameOrNull(distinctByStreet[0].street.extractedName)
                    val nameTags = mutableMapOf<String, String>()
                    if (found != null) {
                        nameTags.put(NAME_KA_TAG, found.nameKa)
                        nameTags.put(NAME_EN_TAG, found.nameEn)
                        nameTags.put(NAME_RU_TAG, found.nameRu)
                    }
                    val tags = TagCreator.create(
                        type = STREET,
                        osmStreet = null,
                        rawNaprString = naprDto.getDataString(),
                        address = distinctByStreet[0],
                        additionalTags = nameTags
                    )
                    val streets = dataSet.selected.getStreets()

                    val chStreetCommands = CommandHelper.toChangeCommandsSimple(tags, streets)
                    commands.addAll(chStreetCommands)

                    streetSuccess = true
                }
            }

            if (commands.isNotEmpty()) {
                val command: Command = SequenceCommand(I18n.tr("Added by GeorgiaAddressHelper"), commands)
                UndoRedoHandler.getInstance().add(command)
            }

            if (streetSuccess) dataSet.setSelected(dataSet.selected.getStreets())
            else dataSet.setSelected(node)
        }

        mapView.setNewCursor(Cursor(Cursor.DEFAULT_CURSOR), this)
    }

    override fun doKeyPressed(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_ESCAPE) {
            val map = MainApplication.getMap()
            map.selectMapMode(map.mapModeSelect)
        }
    }

    override fun doKeyReleased(e: KeyEvent?) {
        // Do nothing
    }
}
