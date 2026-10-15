package org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.actions

import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.JOptionPane
import kotlinx.coroutines.runBlocking
import org.openstreetmap.josm.actions.JosmAction
import org.openstreetmap.josm.command.AddCommand
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
import org.openstreetmap.josm.plugins.dl.geaddresshelper.GeAddressHelperPlugin
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.api.NaprService
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.api.RawNaprDto
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ActionType
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.Address
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.MainParser
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.dto.ParseResult
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ParsingFlags
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ResultSetAnalyzer
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.getAddrBuildings
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.getBuildings
import org.openstreetmap.josm.plugins.dl.geaddresshelper.settings.io.EgrnSettingsReader
import org.openstreetmap.josm.plugins.dl.geaddresshelper.settings.io.MassActionSettingsReader
import org.openstreetmap.josm.plugins.dl.geaddresshelper.settings.io.ValidationSettingsReader.Companion.DISTANCE_FOR_STREET_WAY_SEARCH
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.CommandHelper
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.GeometryHelper
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.OsmPrimitiveHelper
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator.FIXME_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.TagCreator.REMOVE_ME
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.ActionType.*
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.parsers.OsmStreetMatcher
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.funs.getNamedStreets
import org.openstreetmap.josm.plugins.dl.geaddresshelper.validation.N_ValidationRecord
import org.openstreetmap.josm.tools.Geometry
import org.openstreetmap.josm.tools.I18n
import org.openstreetmap.josm.tools.Logging
import org.openstreetmap.josm.tools.Shortcut

class SelectAction :
    JosmAction(
        ACTION_NAME,
        ICON_NAME,
        "Get NAPR data for selected buildings",
        Shortcut.registerShortcut(
            "data:napr_selected",
            I18n.tr("Data: {0}", I18n.tr(ACTION_NAME)),
            KeyEvent.KEY_LOCATION_UNKNOWN,
            Shortcut.NONE,
        ),
        false,
    ) {
    companion object {
        val ACTION_NAME = I18n.tr("For selected")
        const val ICON_NAME = "g_select.svg"
    }

    override fun updateEnabledState() {
        isEnabled =
            MainApplication.isDisplayingMapView() &&
                    MainApplication.getMap().mapView.isActiveLayerDrawable
    }

    private val naprService = NaprService()

    override fun actionPerformed(e: ActionEvent?) {
        val dataSet: DataSet = OsmDataManager.getInstance().editDataSet ?: return
        val selected = dataSet.selected
        val nodesForRemove = selected.filter { it is Node && it.hasTag(FIXME_TAG, REMOVE_ME) }

        var selectedBuildings = selected.getBuildings(MassActionSettingsReader.EGRN_MASS_ACTION_FILTER_LIST.get())

        if (selectedBuildings.isEmpty()) {
            val msg = I18n.tr("All selected buildings are not eligible for request!")
            Notification(msg).setIcon(JOptionPane.WARNING_MESSAGE).show()
            return
        }

        if (selectedBuildings.size > EgrnSettingsReader.REQUEST_LIMIT_PER_SELECTION.get()) {
            selectedBuildings =
                selectedBuildings.dropLast(selectedBuildings.size - EgrnSettingsReader.REQUEST_LIMIT_PER_SELECTION.get())
            val msg =
                I18n.tr("Selected more than set limit buildings, only first %s will be processed")
                    .format(EgrnSettingsReader.REQUEST_LIMIT_PER_SELECTION.get().toString())
            Notification(msg).setIcon(JOptionPane.WARNING_MESSAGE).show()
        }

        val centerToBuilding: List<Pair<EastNorth, OsmPrimitive>> =
            selectedBuildings.map { osmPrimitive -> Pair(GeometryHelper.getPointIntoPolygon(osmPrimitive), osmPrimitive) }

        object : PleaseWaitRunnable(I18n.tr("Fetching data from napr.gov.ge...")) {
            var naprResults: List<Triple<EastNorth, OsmPrimitive, RawNaprDto>> = emptyList()

            override fun realRun() {
                naprResults = runBlocking { naprService.fetchData(centerToBuilding) }
            }

            override fun finish() {
                val commands: MutableList<Command> = mutableListOf()
                val parsingResults: MutableList<ParseResult> = mutableListOf()
                for (naprResult in naprResults.filter { it.third.isUseful() }) {
                    val rawNaprDto: RawNaprDto = naprResult.third
                    val usefulNaprStrings = rawNaprDto.getUsefulString()
                    val parsedAddressList: List<Address> = MainParser.parse(usefulNaprStrings)
                    val fullAddresses = parsedAddressList.filter { it.isSuccess }
                    var matchedOsmStreetName: String? = null
                    if (fullAddresses.size == 1) {
                        val filterByDistance: List<OsmPrimitive> = dataSet.getNamedStreets()
                            .filter { Geometry.getDistance(naprResult.second, it) < DISTANCE_FOR_STREET_WAY_SEARCH.get() }
                        val extractedName = fullAddresses.first().street.extractedName
                        matchedOsmStreetName = OsmStreetMatcher.findBestOrNull(filterByDistance,extractedName)
                    }
                    parsingResults.add(
                        ParseResult(
                            naprResult.first,
                            naprResult.second,
                            fullAddresses,
                            rawNaprDto.getDataString(),
                            matchedOsmStreetName,
                        )
                    )
                }

                val analyzer = ResultSetAnalyzer(
                    dataSet.allPrimitives().getAddrBuildings(MassActionSettingsReader.FILTER_LIST_ADDR.get()),
                    parsingResults.filter { it.parsedAddressList.size == 1 }
                )

                for (result: ParseResult in parsingResults) {
                    val resultAction = analyzer.defineAction(result)

                    //если адрес не один - не добавляем адресные теги
                    val address: Address? = if (result.parsedAddressList.size == 1) result.parsedAddressList[0] else null
                    val additionalTags = getAdditionalTags(resultAction, result.matchStreet)
                    val tegCreationFunction = TagCreator.getTegCreationFunction(resultAction)
                    val tags: Map<String, String> = tegCreationFunction.invoke(result.rawNaprStrings, address, additionalTags)
                    when (resultAction) {
                        ADD_ADDR_TAGS -> {
                            val chBuildingCommands = CommandHelper.toChangeCommands(tags, result.osmPrimitive)
                            commands.addAll(chBuildingCommands)

                            requireNotNull(address) { "ParsedAddress cannot be null while creating building's tags" }
                            if (result.matchStreet != address.street.extractedName) {
                                address.street.flags.add(ParsingFlags.STREET_NAME_FUZZY_MATCH)
                            }
                            if (address.getValidatedFlags().isNotEmpty()) {
                                val validationRecords: List<N_ValidationRecord> =
                                    toValidationRecords(result.eastNorth, result)
                                GeAddressHelperPlugin.cache.add(result.osmPrimitive, validationRecords)
                            }
                        }

                        else -> {
                            val node = OsmPrimitiveHelper.createNode(result.eastNorth, tags)
                            commands.add(AddCommand(dataSet, node))
                        }

                    }
                }
                // удаление выбранных временных точек при повторных запросах
                if (nodesForRemove.isNotEmpty()) {
                    commands.add(DeleteCommand(nodesForRemove))
                }

                val primitivesToValidate = GeAddressHelperPlugin.cache.responses.keys.filter { !it.isDeleted }
                if (primitivesToValidate.isNotEmpty()) {
                    GeAddressHelperPlugin.runEgrnValidation(primitivesToValidate)
                    Logging.info("finish validate: $primitivesToValidate")
                }

                if (commands.isNotEmpty()) {
                    val command: Command = SequenceCommand(I18n.tr("Added node from GeorgiaAddressHelper"), commands)
                    UndoRedoHandler.getInstance().add(command)
                }

                if (selectedBuildings.size != 1) dataSet.clearSelection()
            }

            override fun cancel() {
                Logging.info("Запрос был отменен пользователем.")
            }
        }.run()
    }

    private fun toValidationRecords(
        eastNorth: EastNorth,
        parseResult: ParseResult
    ): List<N_ValidationRecord> {
        return listOf(N_ValidationRecord(eastNorth, parseResult))
    }

    private fun getAdditionalTags(actionType: ActionType, matchStreet: String?): Map<String, String> {
        return when (actionType) {
            ADD_ADDR_TAGS -> if (matchStreet != null) mapOf("addr:street" to matchStreet) else emptyMap()
            CREATE_NODE_DOUBLE -> mapOf("napr:warn" to "DUPLICATE ADDRESS")
            CREATE_MULTIPLE_ADDR_NODE, CREATE_NODE -> emptyMap()
        }
    }
}
