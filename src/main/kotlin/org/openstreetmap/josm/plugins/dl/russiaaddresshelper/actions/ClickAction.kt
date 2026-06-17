package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.actions

import com.github.kittinunf.fuel.jackson.jacksonDeserializerOf
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import org.openstreetmap.josm.actions.mapmode.MapMode
import org.openstreetmap.josm.command.AddCommand
import org.openstreetmap.josm.command.Command
import org.openstreetmap.josm.command.SequenceCommand
import org.openstreetmap.josm.data.UndoRedoHandler
import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.data.osm.*
import org.openstreetmap.josm.gui.MainApplication
import org.openstreetmap.josm.gui.Notification
import org.openstreetmap.josm.gui.util.KeyPressReleaseListener
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.RussiaAddressHelperPlugin
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.*
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr.processResponse
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.settings.io.ClickActionSettingsReader
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.tools.GeometryHelper
import org.openstreetmap.josm.tools.*
import java.awt.Cursor
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import kotlin.Pair

class ClickAction : MapMode(
    ACTION_NAME, ICON_NAME, null, Shortcut.registerShortcut(
        "data:egrn_click", I18n.tr("Data: {0}", I18n.tr(ACTION_NAME)), KeyEvent.KEY_LOCATION_UNKNOWN, Shortcut.NONE
    ), ImageProvider.getCursor("crosshair", "create_note")
), KeyPressReleaseListener {

    companion object {
        val ACTION_NAME = I18n.tr("By click")
        val ICON_NAME = "click.svg"
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

    override fun mouseClicked(e: MouseEvent) {
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return
        }
        val exportGeometry = ClickActionSettingsReader.EGRN_CLICK_ENABLE_GEOMETRY_IMPORT.get()

        val map = MainApplication.getMap()
        map.selectMapMode(map.mapModeSelect)

        val mapView = map.mapView
        if (!mapView.isActiveLayerDrawable) {
            return
        }
        val placeBoundariesMode = e.isAltDown
        mapView.setNewCursor(Cursor(Cursor.WAIT_CURSOR), this)
        val defaultTagsForNode: Map<String, String> = mapOf("source:addr" to "napr.gov.geЛ", "fixme" to "REMOVE ME!")
        val ds = layerManager.editDataSet
        val cmds: MutableList<Command> = mutableListOf()
        val mouseEN = mapView.getEastNorth(e.x, e.y)
        var index = 0
        var nodes: Set<Node> = setOf()
        val layersToRequest = setOf(NSPDLayer.PLACES_BOUNDARIES)
//            if (placeBoundariesMode) setOf(NSPDLayer.PLACES_BOUNDARIES) else LayerFilterSettingsReader.getClickActionEnabledLayers()
        val projectionBounds = MainApplication.getMap().mapView.state.viewClipRectangle.cornerBounds
        val requestBBox = if (placeBoundariesMode) projectionBounds.toBBox() else null
        val errorMessages: MutableSet<String> = mutableSetOf()
        var buildingPrimitive: OsmPrimitive? = null
        val fullResponse = NSPDResponse()
        val primitivesToValidate = mutableListOf<OsmPrimitive>()
        val mergeDataOnSingleNode = ClickActionSettingsReader.EGRN_CLICK_MERGE_FEATURES.get()
        val nodeTags: MutableMap<Pair<NSPDLayer, Int>, MutableMap<String, String>> = mutableMapOf()
        val tagsForNode: MutableMap<String, String> = mutableMapOf()
        var repeatsExhausted = false
        if (repeatsExhausted) {
            val notification =
                Notification(I18n.tr("Data downloading failed, reason: too much request errors, interrupting")).setIcon(
                    JOptionPane.WARNING_MESSAGE
                )
            notification.duration = Notification.TIME_LONG
            notification.show()
//            return@forEach
        }
        var needToRepeat = true
        val clickRetries = 1
        val clickDelay = 1000L
        var retries = clickRetries
        Logging.info("Executing request")
        val (request, response, result) = NaprClient
            .createRequest(mouseEN)
            .responseObject<NaprBody>(jacksonDeserializerOf())
        RussiaAddressHelperPlugin.totalRequestsPerSession++
        if (response.statusCode == 200) {
            tagsForNode.putAll(processResponse(result))
            needToRepeat = true

            result.failure {
                needToRepeat = false
                retries = 0
                val errorMessage = (result as Result.Failure).error.message
                errorMessages.plusAssign(errorMessage ?: "")
                Logging.error("EGRN-Plugin $errorMessage")
            }
        } else {
            result.failure {
                needToRepeat = if (retries > 0) {
                    true
                } else {
                    repeatsExhausted = true
                    val errorMessage = (result as Result.Failure).error.message
                    errorMessages.plusAssign(errorMessage ?: "")
                    Logging.error("EGRN-Plugin $errorMessage")
                    false
                }
                if (response.statusCode == -1) {
                    Logging.warn("EGRN-Plugin Error connection refused, retries $retries")
                } else {
                    Logging.warn("EGRN-Plugin Error on request: ${response.statusCode}")
                }
                retries--
                Thread.sleep(clickDelay)
            }
        }
        index++

        //generate nodes
        if (tagsForNode.isNotEmpty()) {
            nodes = nodes.plus(getAllNodesWithTags1(mouseEN, tagsForNode))
        }

        nodes.forEach { node -> cmds.add(AddCommand(ds, node)) }

        errorMessages.forEach { err ->
            val msg = I18n.tr("Data downloading failed, reason:")
            val notification = Notification("$msg $err").setIcon(JOptionPane.WARNING_MESSAGE)
            notification.duration = Notification.TIME_LONG
            notification.show()
        }

        if (cmds.isNotEmpty()) {
            val c: Command =
                SequenceCommand(I18n.tr("Added node from RussiaAddressHelper"), cmds)
            UndoRedoHandler.getInstance().add(c)
        }

        if (buildingPrimitive != null) {
            ds.setSelected(buildingPrimitive)
        } else {
            ds.setSelected(nodes)
        }
    }

    //выглядит очень неэффективно, нужен рефакторинг
    private fun getMergedTags(nodeTags: MutableMap<Pair<NSPDLayer, Int>, MutableMap<String, String>>): MutableMap<String, String> {
        val result = mutableMapOf<String, String>()
        val tagsByKeyMap = mutableMapOf<String, MutableSet<Pair<String, Pair<NSPDLayer, Int>>>>()
        nodeTags.forEach { (info, tags) ->
            tags.forEach { (key, value) ->
                if (value.isNotBlank()) {
                    if (tagsByKeyMap.containsKey(key)) {
                        tagsByKeyMap[key]?.add(Pair(value, info))
                    } else {
                        tagsByKeyMap[key] = mutableSetOf(Pair(value, info))
                    }
                }
            }
        }

        tagsByKeyMap.forEach { (key, setOfValues) ->
            if (setOfValues.size == 1 || setOfValues.distinctBy { it.first }.size == 1) {
                result[key] = setOfValues.first().first
            } else {
                if (setOfValues.distinctBy { it.second.second }.size > 1) {
                    setOfValues.forEach { entry ->
                        result["$key:${entry.second.first.name.lowercase()}:${entry.second.second}"] = entry.first
                    }
                } else {
                    setOfValues.forEach { entry ->
                        result["$key:${entry.second.first.name.lowercase()}"] = entry.first
                    }
                }
            }
        }

        return result
    }

    private fun getAllNodesWithTags1(
        mouseEN: EastNorth,
        nodeTags: MutableMap<String, String>
    ): Node {
        val node = Node(GeometryHelper.getNodePlacement(mouseEN, 0))
        node.putAll(nodeTags)
        return node
    }

//    private fun getAllNodesWithTags(
//        mouseEN: EastNorth,
//        nodeTags: MutableMap<Pair<NSPDLayer, Int>, MutableMap<String, String>>
//    ): List<Node> {
//        val result = mutableListOf<Node>()
//        var index = 0
//        nodeTags.forEach { (info, tags) ->
//            val n = Node(GeometryHelper.getNodePlacement(mouseEN, index))
//            n.putAll(tags)
//            n.put("addr:RU:layer", info.first.name)
//            result.add(n)
//            index++
//        }
//
//        return result
//    }

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