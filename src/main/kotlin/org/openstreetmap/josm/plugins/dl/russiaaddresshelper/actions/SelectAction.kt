package org.openstreetmap.josm.plugins.dl.russiaaddresshelper.actions

import kotlinx.coroutines.runBlocking
import org.openstreetmap.josm.actions.JosmAction
import org.openstreetmap.josm.command.AddCommand
import org.openstreetmap.josm.command.ChangePropertyCommand
import org.openstreetmap.josm.command.Command
import org.openstreetmap.josm.command.DeleteCommand
import org.openstreetmap.josm.command.SequenceCommand
import org.openstreetmap.josm.data.UndoRedoHandler
import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.OsmDataManager
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.gui.MainApplication
import org.openstreetmap.josm.gui.Notification
import org.openstreetmap.josm.gui.PleaseWaitRunnable
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.RussiaAddressHelperPlugin
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.N_ParsedAddresses
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.RawNaprDto
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.NaprService
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.ParsingFlags
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.ParsedHouseNumber
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr.MainParser
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr.N_ParsedAddress
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr.N_ParsedStreet
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr.dto.ParsingResult
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.napr.findBestMatchingAddress
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.settings.io.EgrnSettingsReader
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.settings.io.MassActionSettingsReader
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.tools.GeometryHelper
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.tools.TagHelper
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.validation.N_ValidationRecord
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

    private val naprService = NaprService()

    override fun actionPerformed(e: ActionEvent?) {
        val dataSet: DataSet = OsmDataManager.getInstance().editDataSet ?: return
        var selected = dataSet.selected.toList()
        val nodeForRemove = selected.filter {
            it is Node && it.hasTag("fixme", "REMOVE ME!")
        }

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

        val centerToBuilding: List<Pair<EastNorth, OsmPrimitive>> = selected.map { osmPrimitive ->
            Pair(GeometryHelper.getPointIntoPolygon(osmPrimitive), osmPrimitive)
        }

        object : PleaseWaitRunnable(I18n.tr("Fetching data from napr.gov.ge...")) {
            var naprResults: List<Triple<EastNorth, OsmPrimitive, RawNaprDto>> = emptyList()
            override fun realRun() {
                naprResults = runBlocking { naprService.fetchData(centerToBuilding) }
            }

            override fun finish() {
                val mainParser = MainParser()

                val commands: MutableList<Command> = mutableListOf()
                for (naprResult in naprResults.filter { it.third.isUseful() }) {
                    val eastNorth: EastNorth = naprResult.first
                    val osmPrimitive: OsmPrimitive = naprResult.second
                    val naprDto: RawNaprDto = naprResult.third

                    val usefulString = getUsefulString(naprDto)
                    val parsedAddressList: List<N_ParsedAddress> = mainParser.parse(usefulString)

//                    val filter = parsedAddressList.filter {
//                        findBestMatchingAddress(it.parsedStreet.extractedName, dataSet) != null
//                    }

                    if (parsedAddressList.size == 1) { //todo реализовать когда больше 1
                        val address: N_ParsedAddress = parsedAddressList.first()
                        val streetFromDataSet: String? =
                            findBestMatchingAddressXXX(address.parsedStreet.extractedName, dataSet)

                        if (streetFromDataSet != null) {
                            //если сматчилось кидаем теги на здание
                            if (streetFromDataSet != address.parsedStreet.extractedName) {
                                address.parsedStreet.flags.add(ParsingFlags.STREET_NAME_FUZZY_MATCH)
                            }
                            val tags: Map<String, String> = toBuildingTags(streetFromDataSet, address)
                            val changeBuildingCommand = createChangeBuildingCommands(tags, osmPrimitive)
                            commands.addAll(changeBuildingCommand)

                            if (address.getValidatedFlags().isNotEmpty()) {
                                val validationRecords: List<N_ValidationRecord> = createValidationRecords(eastNorth, address)
                                RussiaAddressHelperPlugin.cache.add(osmPrimitive, validationRecords)
                            }

                        } else {
                            //если не сматчилось создаем точку
                            //todo теги для точки и команда создания точки
                            val tags: Map<String, String> = toNodeTags(usefulString, address)
                            val addNodeCommand = toAddNodeCommand(eastNorth, tags, dataSet)
                            commands.addAll(listOf(addNodeCommand))
                        }
                    } else {//несколько строк
                        if (usefulString.isNotEmpty()) {
                            val tags: Map<String, String> = toNodeTagsMulti(usefulString)
                            val addNodeCommand = toAddNodeCommand(eastNorth, tags, dataSet)
                            commands.addAll(listOf(addNodeCommand))
                        }
                    }
                }

                //удаление выбранных точек
                if (nodeForRemove.isNotEmpty()) {
                    commands.add(DeleteCommand(nodeForRemove))
                }

                val primitivesToValidate = RussiaAddressHelperPlugin.cache.responses.keys.filter { !it.isDeleted }
                Logging.info("finish validate: $primitivesToValidate")
                if (primitivesToValidate.isNotEmpty()) {
                    RussiaAddressHelperPlugin.runEgrnValidation(primitivesToValidate)
                }

                if (commands.isNotEmpty()) {
                    val command: Command = SequenceCommand(I18n.tr("Added node from GeorgiaAddressHelper"), commands)
                    UndoRedoHandler.getInstance().add(command)
                }

                dataSet.clearSelection();

            }


            override fun cancel() {
                Logging.info("Запрос был отменен пользователем.")
            }
        }.run()
    }

    val statuses =
        setOf("ქუჩა", "ქ.", "გამზირი", "გამზ.", "ბულვარი", "ჩიხი", "შესახვევი", "შეს.", "გასასვლელი", "აღმართი")

    fun getUsefulString(naprBody: RawNaprDto): List<String> {
        val allAddrStrings = naprBody.result
            ?.flatMap { listOfNotNull(it.descript, it.resulttext, it.name) }
            ?.filter { it.isNotEmpty() }
            ?.filter { line -> "N" in line } //todo не теряем ли мы номера без N
            ?.filter { line -> statuses.any { status -> status in line } }
        //    ?.filter { line -> !line.contains("ნაკვეთი") } //todo участок удалять при сплите
            ?: emptyList()
        return allAddrStrings
    }

    private fun toNodeTagsMulti(
        rawString: List<String>
    ): Map<String, String> = buildMap {
        // 1. Наполняем мапу сырыми адресами
        rawString.distinct().forEachIndexed { index, string ->
            put("napr:addr:raw:${index + 1}", string)
        }
        put("fixme", "REMOVE ME!")
    }

    private fun toNodeTags(
        rawString: List<String>,
        address: N_ParsedAddress
    ): Map<String, String> = buildMap {
        // 1. Наполняем мапу сырыми адресами
        rawString.distinct().forEachIndexed { index, string ->
            put("napr:addr:raw:${index + 1}", string)
        }
        if (address.parsedStreet.flags.contains(ParsingFlags.GENITIVE_APPLIED)) {
            put("warn:1", "to genitive case")
        }


        // 2. Добавляем фиксированные теги
        put("addr:housenumber", address.parsedHouseNumber.extractedNumber)
        put("addr:street", address.parsedStreet.extractedName)
        put("fixme", "REMOVE ME!")
    }

    private fun toBuildingTags(
        streetFromDataSet: String,
        address: N_ParsedAddress
    ): Map<String, String> {
        return composeBuildingTag(address.naprFullString, streetFromDataSet, address.parsedHouseNumber.extractedNumber)
    }

    fun composeBuildingTag(
        sourceFullString: String,
        street: String,
        number: String
    ): MutableMap<String, String> {
        val tags = mutableMapOf<String, String>()
        if (number != null) {//fixme
            tags.put("addr:housenumber", number)
        }
        tags.put("addr:street", street)
        tags.put("napr:addr", sourceFullString)
        return tags
    }

    private fun createValidationRecords(
        eastNorth: EastNorth,
        address: N_ParsedAddress
    ): List<N_ValidationRecord> {
        return listOf(
            N_ValidationRecord(
                eastNorth,
                address
            )
        )
    }


    private fun createChangeBuildingCommands(tags: Map<String, String>, osmPrimitive: OsmPrimitive): List<Command> {
        val commands: MutableList<Command> = mutableListOf()
        for (tag in tags) {
            val command: Command? = createComm(tag, osmPrimitive)
            if (command != null) {
                commands.add(command)
            }
        }
        return commands
    }

    private fun createComm(tag: Map.Entry<String, String>, building: OsmPrimitive): Command? {
        if (!building.hasTag(tag.key)
            || TagHelper.isOverwriteEnabled(tag.key, building.get(tag.key), tag.value)
        ) {
            return ChangePropertyCommand(building, tag.key, tag.value)
        } else {
            return null
        }
    }

    private fun createChangeBuildingCommand1(parseResult: ParsingResult): List<Command> {
        val commands: MutableList<Command> = mutableListOf()
        val building = parseResult.osmPrimitive
        for (tag in parseResult.bulgingTags) {
            val command: Command? = createComm(tag, building)
            if (command != null) {
                commands.add(command)
            }
        }
        return commands
    }

    private fun createChangeBuildingCommands(parseResults: List<ParsingResult>): List<Command> {
        return parseResults.flatMap { createChangeBuildingCommand(it) }
    }


    private fun createChangeBuildingCommand(parseResult: ParsingResult): List<Command> {
        val commands: MutableList<Command> = mutableListOf()
        val building = parseResult.osmPrimitive
        for (tag in parseResult.bulgingTags) {
            val command: Command? = createComm(tag, building)
            if (command != null) {
                commands.add(command)
            }
        }
        return commands
    }

//    private fun createComm(tag: Pair<String, String>, building: OsmPrimitive): Command? {
//        return if (!building.hasTag(tag.first)) {
//            ChangePropertyCommand(building, tag.first, tag.second)
//        } else {
//            return if (TagHelper.isOverwriteEnabled(tag.first, building[tag.first], tag.second)) {
//                ChangePropertyCommand(building, tag.first, tag.second)
//            } else {
//                null
//            }
//        }
//    }

    private fun createComm(tag: Pair<String, String>, building: OsmPrimitive): Command? {
        if (!building.hasTag(tag.first)
            || TagHelper.isOverwriteEnabled(tag.first, building.get(tag.first), tag.second)
        ) {
            return ChangePropertyCommand(building, tag.first, tag.second)
        } else {
            return null
        }
    }

    private fun toAddNodeCommand(eastNorth: EastNorth, tags: Map<String, String>, dataSet: DataSet): Command {
        val node = createNode(eastNorth, tags)
        return AddCommand(dataSet, node)
    }

    private fun toAddNodeCommands(parseResults: List<ParsingResult>, dataSet: DataSet): List<Command> {
        return parseResults
            .map { createNode(it.eastNorth, it.nodeTags) }
            .map { AddCommand(dataSet, it) }
            .toList()
    }

    private fun createNode(eastNorth: EastNorth, tags: Map<String, String>): Node {
        val node = Node(GeometryHelper.getNodePlacement(eastNorth, 0))
        node.putAll(tags)
        return node
    }

    private fun createNode(eastNorth: EastNorth, tags: List<Pair<String, String>>): Node {
        val node = Node(GeometryHelper.getNodePlacement(eastNorth, 0))
        node.putAll(tags.associate { it.first to it.second })
        return node
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