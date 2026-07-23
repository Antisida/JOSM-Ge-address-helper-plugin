package org.openstreetmap.josm.plugins.dl.geaddresshelper.actions

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
import org.openstreetmap.josm.plugins.dl.geaddresshelper.api.NaprService
import org.openstreetmap.josm.plugins.dl.geaddresshelper.api.RawNaprDto
import org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers.Address
import org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers.MainParser
import org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers.ParsingFlags
import org.openstreetmap.josm.plugins.dl.geaddresshelper.settings.io.EgrnSettingsReader
import org.openstreetmap.josm.plugins.dl.geaddresshelper.settings.io.MassActionSettingsReader
import org.openstreetmap.josm.plugins.dl.geaddresshelper.settings.io.ValidationSettingsReader.Companion.DISTANCE_FOR_STREET_WAY_SEARCH
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.CommandHelper
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.GeometryHelper
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.OsmPrimitiveHelper
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.TagCreator
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.TagCreator.BUILDING_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.TagCreator.FIXME_TAG
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.TagCreator.REMOVE_ME
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.TagCreator.TagType.BUILDING
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.TagCreator.TagType.NODE
import org.openstreetmap.josm.plugins.dl.geaddresshelper.utils.OsmStreetMatcher
import org.openstreetmap.josm.plugins.dl.geaddresshelper.validation.N_ValidationRecord
import org.openstreetmap.josm.tools.I18n
import org.openstreetmap.josm.tools.Logging
import org.openstreetmap.josm.tools.Shortcut

class SelectAction :
    JosmAction(
        ACTION_NAME,
        ICON_NAME,
        null,
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
    val ICON_NAME = "g_select.svg"
  }

  override fun updateEnabledState() {
    isEnabled =
        MainApplication.isDisplayingMapView() &&
            MainApplication.getMap().mapView.isActiveLayerDrawable
  }

  private val naprService = NaprService()

  private fun List<OsmPrimitive>.getBuildings(
      ignoreTags: Map<String, List<String>>
  ): List<OsmPrimitive> {
    return this.filter {
      it !is Node &&
          it.hasTag(BUILDING_TAG) &&
          ignoreTags.all { (key, values) ->
            values.none { tagValue ->
              it.hasTag(key, tagValue) || (it.hasKey(key) && tagValue == "*")
            }
          }
    }
  }

  override fun actionPerformed(e: ActionEvent?) {
    val dataSet: DataSet = OsmDataManager.getInstance().editDataSet ?: return
    val selected = dataSet.selected.toList()
    val nodesForRemove = selected.filter { it is Node && it.hasTag(FIXME_TAG, REMOVE_ME) }

    var buildings =
        selected.getBuildings(MassActionSettingsReader.EGRN_MASS_ACTION_FILTER_LIST.get())

    if (buildings.isEmpty()) {
      val msg = I18n.tr("All selected buildings are not eligible for request!")
      Notification(msg).setIcon(JOptionPane.WARNING_MESSAGE).show()
      return
    }

    if (buildings.size > EgrnSettingsReader.REQUEST_LIMIT_PER_SELECTION.get()) {
      buildings =
          buildings.dropLast(buildings.size - EgrnSettingsReader.REQUEST_LIMIT_PER_SELECTION.get())
      val msg =
          I18n.tr("Selected more than set limit buildings, only first %s will be processed")
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

              val usefulString = naprDto.getUsefulString()
              val parsedAddressList: List<Address> = MainParser.parse(usefulString)

              if (parsedAddressList.size == 1) {
                val parsedAddress: Address = parsedAddressList.first()
                val matchedOsmStreetName: String? =
                    OsmStreetMatcher.findByNameAndDistance(
                        dataSet,
                        parsedAddress.parsedStreet.extractedName,
                        building,
                        DISTANCE_FOR_STREET_WAY_SEARCH.get(),
                    )

                if (matchedOsmStreetName != null) {
                  // если сматчилось кидаем теги на здание
                  if (matchedOsmStreetName != parsedAddress.parsedStreet.extractedName) {
                    parsedAddress.parsedStreet.flags.add(ParsingFlags.STREET_NAME_FUZZY_MATCH)
                  }
                  val tags: Map<String, String> =
                      TagCreator.create(
                          BUILDING,
                          matchedOsmStreetName,
                          parsedAddress,
                          usefulString,
                      )
                  val changeBuildingCommands =
                      CommandHelper.toChangeCommands(tags, building)
                  commands.addAll(changeBuildingCommands)

                  if (parsedAddress.getValidatedFlags().isNotEmpty()) {
                    val validationRecords: List<N_ValidationRecord> =
                        toValidationRecords(centerCoord, parsedAddress)
                      GeAddressHelperPlugin.cache.add(building, validationRecords)
                  }
                } else {
                  // если не сматчилось создаем точку
                  val tags: Map<String, String> =
                      TagCreator.create(NODE, null, parsedAddress, usefulString)
                  val node = OsmPrimitiveHelper.createNode(centerCoord, tags)
                  commands.add(AddCommand(dataSet, node))
                }
              } else { // если имеем несколько разных адресов - создаем точку с сырыми
                // данными напр
                if (usefulString.isNotEmpty()) {
                  val tags: Map<String, String> = TagCreator.create(NODE, null, null, usefulString)
                  val node = OsmPrimitiveHelper.createNode(centerCoord, tags)
                  commands.add(AddCommand(dataSet, node))
                }
              }
            }

            // удаление выбранных временных точек при повторных запросах
            if (nodesForRemove.isNotEmpty()) {
              commands.add(DeleteCommand(nodesForRemove))
            }

            val primitivesToValidate =
                GeAddressHelperPlugin.cache.responses.keys.filter { !it.isDeleted }
            if (primitivesToValidate.isNotEmpty()) {
                GeAddressHelperPlugin.runEgrnValidation(primitivesToValidate)
              Logging.info("finish validate: $primitivesToValidate")
            }

            if (commands.isNotEmpty()) {
              val command: Command =
                  SequenceCommand(
                      I18n.tr("Added node from GeorgiaAddressHelper"),
                      commands,
                  )
              UndoRedoHandler.getInstance().add(command)
            }

            if (buildings.size != 1) dataSet.clearSelection()
          }

          override fun cancel() {
            Logging.info("Запрос был отменен пользователем.")
          }
        }
        .run()
  }

  private fun toValidationRecords(
      eastNorth: EastNorth,
      address: Address,
  ): List<N_ValidationRecord> {
    return listOf(N_ValidationRecord(eastNorth, address))
  }
}
