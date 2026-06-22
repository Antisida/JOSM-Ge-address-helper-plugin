package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.actions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.openstreetmap.josm.actions.JosmAction
import org.openstreetmap.josm.command.AddCommand
import org.openstreetmap.josm.command.ChangePropertyCommand
import org.openstreetmap.josm.command.Command
import org.openstreetmap.josm.command.SequenceCommand
import org.openstreetmap.josm.data.UndoRedoHandler
import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.OsmDataManager
import org.openstreetmap.josm.gui.MainApplication
import org.openstreetmap.josm.gui.Notification
import org.openstreetmap.josm.gui.PleaseWaitRunnable
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.NaprClient
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.models.BuildingDataDto
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr.parseResponse
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.settings.io.EgrnSettingsReader
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.settings.io.MassActionSettingsReader
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.tools.GeometryHelper
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.tools.TagHelper
import org.openstreetmap.josm.tools.I18n
import org.openstreetmap.josm.tools.Logging
import org.openstreetmap.josm.tools.Shortcut
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.JOptionPane

class SelectAction : JosmAction(
    ACTION_NAME, ICON_NAME, null, Shortcut.registerShortcut(
        "data:egrn_selected", I18n.tr("Data: {0}", I18n.tr(ACTION_NAME)), KeyEvent.KEY_LOCATION_UNKNOWN, Shortcut.NONE
    ), false
) {
    companion object {
        val ACTION_NAME = I18n.tr("For selected objects")
        val ICON_NAME = "select.svg"
        val defaultTagsForNode: Map<String, String> = mapOf("source:addr" to "napr.gov.ge", "fixme" to "REMOVE ME!")
    }

    override fun updateEnabledState() {
        isEnabled = MainApplication.isDisplayingMapView() && MainApplication.getMap().mapView.isActiveLayerDrawable
    }


    /*override fun actionPerformed(e: ActionEvent?) {
        val progressDialog = PleaseWaitProgressMonitor()
        progressDialog.beginTask(I18n.tr("Data download"), 5)
        progressDialog.showForegroundDialog()
        progressDialog.addCancelListener {
            progressDialog.close()
        }
        val dataSet: DataSet = OsmDataManager.getInstance().editDataSet ?: return
        var selected = dataSet.selected.toList()
        val buildingBadValues = MassActionSettingsReader.EGRN_MASS_ACTION_FILTER_LIST.get()
        selected = selected.filter {
            it !is Node &&
                    it.hasTag("building")
                    && buildingBadValues.all { (key, values) ->
                values.none { tagValue ->
                    it.hasTag(
                        key,
                        tagValue
                    ) || (it.hasKey(key) && tagValue == "*")
                }
            }
        }

        if (selected.isEmpty()) {
            val msg = I18n.tr("All selected buildings are not eligible for request!")
            Notification(msg).setIcon(JOptionPane.WARNING_MESSAGE).show()
            return
        }

        if (selected.size > EgrnSettingsReader.REQUEST_LIMIT_PER_SELECTION.get()) {
            selected = selected.dropLast(selected.size - EgrnSettingsReader.REQUEST_LIMIT_PER_SELECTION.get())
            val msg = I18n.tr("Selected more than set limit buildings, only first %s will be processed")
                .format(EgrnSettingsReader.REQUEST_LIMIT_PER_SELECTION.get().toString())
            Notification(msg).setIcon(JOptionPane.WARNING_MESSAGE).show()
        }

        val eastNorths = selected.map { b -> GeometryHelper.getPointIntoPolygon(b) }
        Logging.info("eastNorths: $eastNorths")

        CoroutineScope(Dispatchers.IO).launch {
            val semaphore = Semaphore(5) // не больше 5 одновременных запросов
            val coordinateToResult = eastNorths.map { coord ->
                async {
                    semaphore.withPermit { // Семафор придержит лишние запросы
                        try {
                            val (_, response, result) =
                                NaprApi.createRequest(coord)
                                    .responseObject<NaprBody>(jacksonDeserializerOf())
                            if (response.statusCode == 200) {
                                Pair(coord, result.get())
                            } else {
                                Logging.error("Ошибка вернулась для координаты $coord: ${response.statusCode}")
                                null
                            }
                        } catch (e: Exception) {
                            Logging.error("Не удалось обработать координату $coord: ${e.message}")
                            null
                        }
                    }
                }
            }.awaitAll().filterNotNull()

            SwingUtilities.invokeLater {
                if (OsmDataManager.getInstance().editDataSet != dataSet) {
                    Logging.warn("DataSet изменился или был закрыт. Отмена операции.")
                    return@invokeLater
                }
                val nodes = mutableSetOf<Node>()
                for (res in coordinateToResult) {
                    val tags = processResponse(res.second)
                    if (tags.isNotEmpty()) {
                        val node = createTaggedNode(res.first, tags)
                        nodes.add(node)
                    }
                }
                val cmds: MutableList<Command> = mutableListOf()
                val ds = layerManager.editDataSet
                for (node in nodes) {
                    cmds.add(AddCommand(ds, node))
                }
                if (cmds.isNotEmpty()) {
                    val c: Command = SequenceCommand(I18n.tr("Added node from RussiaAddressHelper"), cmds)
                    UndoRedoHandler.getInstance().add(c)
                }
            }
        }
    }*/

    override fun actionPerformed(e: ActionEvent?) {
        val dataSet: DataSet = OsmDataManager.getInstance().editDataSet ?: return
        var selected = dataSet.selected.toList()

        val buildingBadValues = MassActionSettingsReader.EGRN_MASS_ACTION_FILTER_LIST.get()
        selected = selected.filter {
            it !is Node &&
                    it.hasTag("building")
                    && buildingBadValues.all { (key, values) ->
                values.none { tagValue ->
                    it.hasTag(
                        key,
                        tagValue
                    ) || (it.hasKey(key) && tagValue == "*")
                }
            }
        }

        if (selected.isEmpty()) {
            val msg = I18n.tr("All selected buildings are not eligible for request!")
            Notification(msg).setIcon(JOptionPane.WARNING_MESSAGE).show()
            return
        }

        if (selected.size > EgrnSettingsReader.REQUEST_LIMIT_PER_SELECTION.get()) { //fixme
            selected = selected.dropLast(selected.size - EgrnSettingsReader.REQUEST_LIMIT_PER_SELECTION.get())
            val msg = I18n.tr("Selected more than set limit buildings, only first %s will be processed")
                .format(EgrnSettingsReader.REQUEST_LIMIT_PER_SELECTION.get().toString())
            Notification(msg).setIcon(JOptionPane.WARNING_MESSAGE).show()
        }

            //создание
        val processingBuildings: Set<BuildingDataDto> = selected
            .map { building -> BuildingDataDto(building, GeometryHelper.getPointIntoPolygon(building)) }
            .toSet()
//        val centerToBuilding: Map<EastNorth, OsmPrimitive> = selected.associateBy { osmPrimitive ->
//            GeometryHelper.getPointIntoPolygon(osmPrimitive)
//        }

//        val eastNorths = selected.map { b -> GeometryHelper.getPointIntoPolygon(b) }
//        Logging.info("eastNorths: $eastNorths")

        object : PleaseWaitRunnable(I18n.tr("Fetching data from napr.gov.ge...")) {
            override fun realRun() {
                runBlocking(Dispatchers.IO) {
                    val semaphore = Semaphore(10)
                    processingBuildings
                        .map { buildingDto ->
                            async {
                                semaphore.withPermit {
                                    buildingDto.naprResponseBody = NaprClient.executeRequest(buildingDto.center)
                                    buildingDto
                                }
                            }
                        }
                        .awaitAll()//.filter { it.naprResponseBody != null }
                }
            }

            override fun finish() {
                if (processingBuildings.map { it.naprResponseBody }.none { it != null }) {
                    Logging.info("Нет успешных данных для добавления.")
                    return@finish
                }
                val commands: MutableList<Command> = mutableListOf()
                for (buildingDto in processingBuildings) {
                    val tags = parseResponse(buildingDto.naprResponseBody)
                    if (tags.isNotEmpty()) {
                        tags.put("source:addr", "napr.gov.ge")
                        for (tag in tags.filter { it.key == "addr:street" || it.key == "addr:housenumber" ||  it.key == "source:addr" }) {
                            if (!buildingDto.building.hasTag(tag.key)) {
                                commands.add(ChangePropertyCommand(buildingDto.building, tag.key, tag.value))
                            } else {
                                if (TagHelper.isOverwriteEnabled(tag.key, buildingDto.building[tag.key], tag.value)) {
                                    commands.add(ChangePropertyCommand(buildingDto.building, tag.key, tag.value))
                                }
                            }
                        }
                        tags.putAll(defaultTagsForNode)
                        val node = createTempTaggedNode(buildingDto.center, tags)
                        commands.add(AddCommand(dataSet, node))
                    }
                }
                val command: Command = SequenceCommand(I18n.tr("Added node from GeorgiaAddressHelper"), commands)
                UndoRedoHandler.getInstance().add(command)

            }

            override fun cancel() {
                Logging.info("Запрос был отменен пользователем.")
            }
        }.run()
    }

    private fun createTempTaggedNode(eastNorth: EastNorth, tags: MutableMap<String, String>): Node {
        val node = Node(GeometryHelper.getNodePlacement(eastNorth, 0))
        if (tags.contains("addr:street") && tags.contains("addr:housenumber")) {
            node.putAll(tags.filter { it.key != "addr:housenumber" && it.key != "addr:street" })
        } else {
            node.putAll(tags)
        }
        return node
    }

}