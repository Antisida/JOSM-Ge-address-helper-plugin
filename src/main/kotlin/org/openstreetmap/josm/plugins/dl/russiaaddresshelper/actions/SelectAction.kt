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
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.RawNaprDto
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.api.NaprService
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.ParsingFlags
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.MainParser
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.parsers.Address
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.settings.io.EgrnSettingsReader
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.settings.io.MassActionSettingsReader
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.settings.io.ValidationSettingsReader.Companion.DISTANCE_FOR_STREET_WAY_SEARCH
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.tools.GeometryHelper
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.tools.TagHelper
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.utils.OsmStreetMatcher
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.tools.TagCreator
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.tools.TagCreator.BUILDING_TAG
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.tools.TagCreator.FIXME_TAG
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.tools.TagCreator.REMOVE_ME
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.tools.TagCreator.TagType.*
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.tools.TagCreator.STATUSES_AND_ABBR_SET
import org.openstreetmap.josm.plugins.dl.russiaaddresshelper.validation.N_ValidationRecord
import org.openstreetmap.josm.tools.I18n
import org.openstreetmap.josm.tools.Logging
import org.openstreetmap.josm.tools.Shortcut
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.JOptionPane
import kotlin.collections.component1
import kotlin.collections.component2

class SelectAction : JosmAction(
    ACTION_NAME, ICON_NAME, null, Shortcut.registerShortcut(
        "data:egrn_selected", I18n.tr("Data: {0}", I18n.tr(ACTION_NAME)), KeyEvent.KEY_LOCATION_UNKNOWN, Shortcut.NONE
    ), false
) {
    companion object {
        val ACTION_NAME = I18n.tr("For selected objects")
        val ICON_NAME = "select.svg"

    }

    override fun updateEnabledState() {
        isEnabled = MainApplication.isDisplayingMapView() && MainApplication.getMap().mapView.isActiveLayerDrawable
    }

    private val naprService = NaprService()

    private fun List<OsmPrimitive>.getBuildings(ignoreTags: Map<String, List<String>>): List<OsmPrimitive> {
        return this.filter {
            it !is Node &&
                    it.hasTag(BUILDING_TAG)
                    && ignoreTags.all { (key, values) ->
                values.none { tagValue ->
                    it.hasTag(
                        key,
                        tagValue
                    ) || (it.hasKey(key) && tagValue == "*")
                }
            }
        }
    }

    override fun actionPerformed(e: ActionEvent?) {
        val dataSet: DataSet = OsmDataManager.getInstance().editDataSet ?: return
        val selected = dataSet.selected.toList()
        val nodesForRemove = selected.filter { it is Node && it.hasTag(FIXME_TAG, REMOVE_ME) }

        var buildings = selected.getBuildings(MassActionSettingsReader.EGRN_MASS_ACTION_FILTER_LIST.get())

        if (buildings.isEmpty()) {
            val msg = I18n.tr("All selected buildings are not eligible for request!")
            Notification(msg).setIcon(JOptionPane.WARNING_MESSAGE).show()
            return
        }

        if (buildings.size > EgrnSettingsReader.REQUEST_LIMIT_PER_SELECTION.get()) {
            buildings = buildings.dropLast(buildings.size - EgrnSettingsReader.REQUEST_LIMIT_PER_SELECTION.get())
            val msg = I18n.tr("Selected more than set limit buildings, only first %s will be processed")
                .format(EgrnSettingsReader.REQUEST_LIMIT_PER_SELECTION.get().toString())
            Notification(msg).setIcon(JOptionPane.WARNING_MESSAGE).show()
        }

        val centerToBuilding: List<Pair<EastNorth, OsmPrimitive>> = buildings.map { osmPrimitive ->
            Pair(GeometryHelper.getPointIntoPolygon(osmPrimitive), osmPrimitive)
        }

        object : PleaseWaitRunnable(I18n.tr("Fetching data from napr.gov.ge...")) {
            var naprResults: List<Triple<EastNorth, OsmPrimitive, RawNaprDto>> = emptyList()
            override fun realRun() {
                naprResults = runBlocking { naprService.fetchData(centerToBuilding) }
            }

            override fun finish() {
                val commands: MutableList<Command> = mutableListOf()
                for (naprResult in naprResults.filter { it.third.isUseful() }) {
                    val centerCoord: EastNorth = naprResult.first
                    val building: OsmPrimitive = naprResult.second
                    val naprDto: RawNaprDto = naprResult.third

                    val usefulString = getUsefulString(naprDto)
                    val parsedAddressList: List<Address> = MainParser.parse(usefulString)

                    if (parsedAddressList.size == 1) {
                        val parsedAddress: Address = parsedAddressList.first()
                        val matchedOsmStreetName: String? = OsmStreetMatcher.findByNameAndDistance(
                            dataSet,
                            parsedAddress.parsedStreet.extractedName,
                            building,
                            DISTANCE_FOR_STREET_WAY_SEARCH.get()
                        )

                        if (matchedOsmStreetName != null) {
                            //если сматчилось кидаем теги на здание
                            if (matchedOsmStreetName != parsedAddress.parsedStreet.extractedName) {
                                parsedAddress.parsedStreet.flags.add(ParsingFlags.STREET_NAME_FUZZY_MATCH)
                            }
                            val tags: Map<String, String> =
                                TagCreator.create(BUILDING, matchedOsmStreetName, parsedAddress, usefulString)
                            val changeBuildingCommand = toChangeBuildingCommands(tags, building)
                            commands.addAll(changeBuildingCommand)

                            if (parsedAddress.getValidatedFlags().isNotEmpty()) {
                                val validationRecords: List<N_ValidationRecord> =
                                    toValidationRecords(centerCoord, parsedAddress)
                                RussiaAddressHelperPlugin.cache.add(building, validationRecords)
                            }
                        } else {
                            //если не сматчилось создаем точку
                            val tags: Map<String, String> = TagCreator.create(NODE, null, parsedAddress, usefulString)
                            val addNodeCommand = toAddNodeCommand(centerCoord, tags, dataSet)
                            commands.addAll(listOf(addNodeCommand))
                        }
                    } else {//если имеем несколько разных адресов - создаем точку с сырыми данными напр
                        if (usefulString.isNotEmpty()) {
                            val tags: Map<String, String> = TagCreator.create(NODE, null, null, usefulString)
                            val addNodeCommand = toAddNodeCommand(centerCoord, tags, dataSet)
                            commands.addAll(listOf(addNodeCommand))
                        }
                    }
                }

                //удаление выбранных точек
                if (nodesForRemove.isNotEmpty()) {
                    commands.add(DeleteCommand(nodesForRemove))
                }

                val primitivesToValidate = RussiaAddressHelperPlugin.cache.responses.keys.filter { !it.isDeleted }
                if (primitivesToValidate.isNotEmpty()) {
                    RussiaAddressHelperPlugin.runEgrnValidation(primitivesToValidate)
                    Logging.info("finish validate: $primitivesToValidate")
                }

                if (commands.isNotEmpty()) {
                    val command: Command = SequenceCommand(I18n.tr("Added node from GeorgiaAddressHelper"), commands)
                    UndoRedoHandler.getInstance().add(command)
                }

                if (buildings.size != 1) dataSet.clearSelection();
            }


            override fun cancel() {
                Logging.info("Запрос был отменен пользователем.")
            }
        }.run()
    }

    private fun getUsefulString(naprBody: RawNaprDto): List<String> {
        val allAddrStrings = naprBody.result
            ?.flatMap { listOfNotNull(it.descript, it.resulttext, it.name) }
            ?.filter { it.isNotEmpty() }
            ?.filter { line -> "N" in line } //todo не теряем ли мы номера без N
            ?.filter { line -> STATUSES_AND_ABBR_SET.any { status -> status in line } }
        //    ?.filter { line -> !line.contains("ნაკვეთი") } //todo участок удалять при сплите
            ?: emptyList()
        return allAddrStrings
    }

    private fun toValidationRecords(
        eastNorth: EastNorth,
        address: Address
    ): List<N_ValidationRecord> {
        return listOf(
            N_ValidationRecord(
                eastNorth,
                address
            )
        )
    }

    private fun toChangeBuildingCommands(tags: Map<String, String>, osmPrimitive: OsmPrimitive): List<Command> {
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

    private fun toAddNodeCommand(eastNorth: EastNorth, tags: Map<String, String>, dataSet: DataSet): Command {
        val node = createNode(eastNorth, tags)
        return AddCommand(dataSet, node)
    }

    private fun createNode(eastNorth: EastNorth, tags: Map<String, String>): Node {
        val node = Node(GeometryHelper.getNodePlacement(eastNorth, 0))
        node.putAll(tags)
        return node
    }

}