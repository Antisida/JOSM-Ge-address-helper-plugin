package org.openstreetmap.josm.plugins.dl.geaddresshelper.actions

import org.openstreetmap.josm.actions.mapmode.MapMode
import org.openstreetmap.josm.command.AddCommand
import org.openstreetmap.josm.command.Command
import org.openstreetmap.josm.command.SequenceCommand
import org.openstreetmap.josm.data.UndoRedoHandler
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.gui.MainApplication
import org.openstreetmap.josm.gui.MapFrame
import org.openstreetmap.josm.gui.util.KeyPressReleaseListener
import org.openstreetmap.josm.plugins.dl.geaddresshelper.api.NaprClient
import org.openstreetmap.josm.plugins.dl.geaddresshelper.api.RawNaprDto
import org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers.Address
import org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers.MainParser
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.OsmPrimitiveHelper
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.TagCreator
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.TagCreator.TagType.NODE
import org.openstreetmap.josm.tools.I18n
import org.openstreetmap.josm.tools.ImageProvider
import org.openstreetmap.josm.tools.Logging
import org.openstreetmap.josm.tools.Shortcut
import java.awt.Cursor
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

class ClickAction : MapMode(
    ACTION_NAME, ICON_NAME, null, Shortcut.registerShortcut(
        "data:napr_click", I18n.tr("Data: {0}", I18n.tr(ACTION_NAME)), KeyEvent.KEY_LOCATION_UNKNOWN, Shortcut.NONE
    ), ImageProvider.getCursor("crosshair", "create_note")
), KeyPressReleaseListener {

    companion object {
        val ACTION_NAME = I18n.tr("NAPR click")
        val ICON_NAME = "g_click.svg"
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
        isEnabled = MainApplication.isDisplayingMapView() && MainApplication.getMap().mapView.isActiveLayerDrawable
    }

    override fun mouseClicked(event: MouseEvent) {
        if (!SwingUtilities.isLeftMouseButton(event)) {
            return
        }

        val map: MapFrame = MainApplication.getMap()
        map.selectMapMode(map.mapModeSelect)

        val mapView = map.mapView
        if (!mapView.isActiveLayerDrawable) {
            return
        }
        mapView.setNewCursor(Cursor(Cursor.WAIT_CURSOR), this)
        val dataSet = layerManager.editDataSet
        val commands: MutableList<Command> = mutableListOf()
        val mouseEastNorth = mapView.getEastNorth(event.x, event.y)
        val nodes: Set<Node> = setOf()
        Logging.info("Executing request")

        val naprDto: RawNaprDto? = NaprClient.executeRequest(mouseEastNorth)
        if (naprDto != null && naprDto.getUsefulString().isNotEmpty()) {
            val usefulString = naprDto.getUsefulString()
            val parsedAddressList: List<Address> = MainParser.parse(usefulString)
            if (parsedAddressList.size == 1) {
                val tags: Map<String, String> = TagCreator.create(NODE, null, parsedAddressList[0], usefulString)
                val node = OsmPrimitiveHelper.createNode(mouseEastNorth, tags)
                nodes.plus(node)
                commands.add(AddCommand(dataSet, node))
            } else {
                val tags: Map<String, String> = TagCreator.create(NODE, null, null, usefulString)
                val node = OsmPrimitiveHelper.createNode(mouseEastNorth, tags)
                nodes.plus(node)
                commands.add(AddCommand(dataSet, node))
            }
        }

        if (commands.isNotEmpty()) {
            val c: Command = SequenceCommand(I18n.tr("Added node from GeAddressHelper"), commands)
            UndoRedoHandler.getInstance().add(c)
        }

        dataSet.setSelected(nodes)
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
