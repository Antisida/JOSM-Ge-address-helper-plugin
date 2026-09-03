package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.actions

import org.openstreetmap.josm.actions.mapmode.MapMode
import org.openstreetmap.josm.command.AddCommand
import org.openstreetmap.josm.command.SequenceCommand
import org.openstreetmap.josm.data.UndoRedoHandler
import org.openstreetmap.josm.gui.MainApplication
import org.openstreetmap.josm.gui.MapFrame
import org.openstreetmap.josm.gui.util.KeyPressReleaseListener
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.api.NaprClient
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.api.RawNaprDto
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Address
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.MainParser
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.OsmPrimitiveHelper
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator.TagType.NODE
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
            val parsedAddress : Address? = MainParser.parse(usefulString).singleOrNull()
            val tags = TagCreator.create(
                type = NODE,
                osmStreet = null,
                rawNaprString = dataString,
                address = parsedAddress,
                additionalTags = emptyMap()
            )
            val node = OsmPrimitiveHelper.createNode(mouseEastNorth, tags)

            val sequenceCommand = SequenceCommand(I18n.tr("Node added"), AddCommand(dataSet, node))
            UndoRedoHandler.getInstance().add(sequenceCommand)

            dataSet.setSelected(node)
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
