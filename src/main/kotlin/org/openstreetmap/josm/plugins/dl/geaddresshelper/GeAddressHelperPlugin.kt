package org.openstreetmap.josm.plugins.dl.geaddresshelper

import javax.swing.JMenu
import org.openstreetmap.josm.actions.UploadAction
import org.openstreetmap.josm.command.AddCommand
import org.openstreetmap.josm.command.SequenceCommand
import org.openstreetmap.josm.data.UndoRedoHandler
import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.OsmDataManager
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.validation.OsmValidator
import org.openstreetmap.josm.data.validation.ValidationTask
import org.openstreetmap.josm.gui.MainApplication
import org.openstreetmap.josm.gui.MapFrame
import org.openstreetmap.josm.plugins.Plugin
import org.openstreetmap.josm.plugins.PluginInformation
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.actions.ClickAction
import org.openstreetmap.josm.plugins.dl.geaddresshelper.deletion.DeleteTmpAction
import org.openstreetmap.josm.plugins.dl.geaddresshelper.dictionary.DictionaryAction
import org.openstreetmap.josm.plugins.dl.geaddresshelper.numberstreetgenerator.dialog.NumberedStreetAction
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.actions.SelectAction
import org.openstreetmap.josm.plugins.dl.geaddresshelper.numberstreetgenerator.NumGeneratorToggleDialog
import org.openstreetmap.josm.plugins.dl.geaddresshelper.settings.io.CommonSettingsReader
import org.openstreetmap.josm.plugins.dl.geaddresshelper.settings.io.ValidationSettingsReader
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.GeometryHelper
import org.openstreetmap.josm.plugins.dl.geaddresshelper.uploadhooks.PluginCacheCleaner
import org.openstreetmap.josm.plugins.dl.geaddresshelper.deletion.UploadFilter
import org.openstreetmap.josm.plugins.dl.geaddresshelper.validation.N_ValidationCache
import org.openstreetmap.josm.plugins.dl.geaddresshelper.validation.NaprFuzzyStreetMatchingTest
import org.openstreetmap.josm.tools.Geometry
import org.openstreetmap.josm.tools.I18n
import org.openstreetmap.josm.tools.ImageProvider

class GeAddressHelperPlugin(info: PluginInformation) : Plugin(info) {
  init {
    menuInit(MainApplication.getMenu().dataMenu)
    versionInfo = info.version
    cache.initListener()
  }

  companion object {
    val ACTION_NAME = I18n.tr("Georgian address helper")!!
    val ICON_NAME = "flag_georgia.svg"

    lateinit var versionInfo: String

    val cache: N_ValidationCache = N_ValidationCache()

    val uploadFilter: UploadFilter = UploadFilter()
    val pluginCacheCleaner: PluginCacheCleaner = PluginCacheCleaner()

    var totalRequestsPerSession = 0L
    var totalSuccessRequestsPerSession = 0L

    val selectAction: SelectAction = SelectAction()
    val clickAction: ClickAction = ClickAction()
    val dictionaryAction: DictionaryAction = DictionaryAction()
    val deleteTmpAction: DeleteTmpAction = DeleteTmpAction()
    val numberedStreetAction: NumberedStreetAction = NumberedStreetAction()

    fun runEgrnValidation(selection: Collection<OsmPrimitive?>) {
      val map = MainApplication.getMap()
      if (map == null || !map.isVisible) return

      OsmValidator.initializeTests()

      // лучше бы фильтровать более надежным методом, но я его не придумал, код теста Test не
      // возвращает.
      // возможно надо унаследовать его
      val egrnTests =
        OsmValidator.getEnabledTests(false).filter { test ->
          test.name.contains("NAPR") || test.name.contains("NAPR")
        }
      if (egrnTests.isEmpty()) return
      //      Logging.info("runEgrnValidation: $egrnTests $selection")
      MainApplication.worker.submit(ValidationTask(egrnTests, selection, null))
    }

    private fun getDuplicatesByDistance(
      osmPrimitives: List<OsmPrimitive>?,
      checkList: List<OsmPrimitive>,
      isPlace: Boolean?,
    ): List<OsmPrimitive> {
      val distance = getDistanceSetting(isPlace)
      val result = mutableListOf<OsmPrimitive>()
      checkList.forEach { primitive ->
        val closestOSMObject = Geometry.getClosestPrimitive(primitive, osmPrimitives)
        if (Geometry.getDistance(primitive, closestOSMObject) < distance) {
          result.add(primitive)
        }
      }
      return result
    }

    private fun getDistanceSetting(isPlace: Boolean?): Int {
      return if (isPlace == true) {
        2 * ValidationSettingsReader.DISTANCE_FOR_PLACE_NODE_SEARCH.get()
      } else {
        return CommonSettingsReader.CLEAR_DOUBLE_DISTANCE.get()
      }
    }

    private fun getOsmAddressesMap(): Map<String, List<OsmPrimitive>> {
      val osmBuildingsWithAddress =
        OsmDataManager.getInstance().editDataSet.allNonDeletedCompletePrimitives().filter { p ->
          p !is Node &&
            p.hasKey("building") &&
            p.hasKey("addr:housenumber") &&
            (p.hasKey("addr:street") || p.hasKey("addr:place"))
        }
      val osmBuildingsAddressMap = osmBuildingsWithAddress.groupBy { getOsmInlineAddress(it) }
      return osmBuildingsAddressMap
    }

    private fun getOsmInlineAddress(p: OsmPrimitive): String {
      return if (p.hasKey("addr:street")) {
        "${p["addr:street"]}, ${p["addr:housenumber"]}"
      } else {
        "${p["addr:place"]}, ${p["addr:housenumber"]}"
      }
    }

    fun createDebugObject(coords: ArrayList<ArrayList<Double>>, requestCoord: EastNorth) {
      val map = MainApplication.getMap()
      val ds = map.mapView.layerManager.editDataSet
      val cmds = GeometryHelper.createPolygon(ds, coords, false).first
      cmds.add(AddCommand(ds, Node(requestCoord)))
      UndoRedoHandler.getInstance().add(SequenceCommand("Add debug geometry", cmds))
    }
  }

  //    override fun getPreferenceSetting(): PreferenceSetting {
  //        return PluginSetting()
  //    }

  override fun mapFrameInitialized(oldFrame: MapFrame?, newFrame: MapFrame?) {
    // this callback fired also everytime last layer is removed, cannot run layer listeners init
    // here
    OsmValidator.addTest(NaprFuzzyStreetMatchingTest::class.java)
    //        OsmValidator.addTest(EGRNEmptyResponseTest::class.java)
    //        OsmValidator.addTest(EGRNFuzzyStreetMatchingTest::class.java)
    //        OsmValidator.addTest(EGRN___FuzzyStreetMatchingTest::class.java)
    //        OsmValidator.addTest(EGRNInitialsStreetMatchingTest::class.java)
    //        OsmValidator.addTest(EGRNMultipleValidAddressTest::class.java)
    //        OsmValidator.addTest(EGRNStreetNotFoundTest::class.java)
    //        OsmValidator.addTest(EGRNAddressAddedTest::class.java)
    //        OsmValidator.addTest(EGRNCantParseAddressTest::class.java)
    //        OsmValidator.addTest(EGRNFlatsInAddressTest::class.java)
    //        OsmValidator.addTest(EGRNPlaceNotFoundTest::class.java)
    //        OsmValidator.addTest(EGRNFuzzyOrInitialsPlaceMatchTest::class.java)
    //        OsmValidator.addTest(EGRNDuplicateAddressesTest::class.java)
    //        OsmValidator.addTest(EGRNStreetOrPlaceTooFarTest::class.java)
    //        OsmValidator.addTest(EGRNConflictedDataTest::class.java)

    UploadAction.registerUploadHook(pluginCacheCleaner, true)
    UploadAction.registerUploadHook(uploadFilter, true)

    if (newFrame != null) {
      val numGenerator = NumGeneratorToggleDialog()
      newFrame.addToggleDialog(numGenerator)
    }
  }

  private fun menuInit(menu: JMenu) {
    menu.isVisible = true

    if (menu.itemCount > 0) {
      menu.addSeparator()
    }

    val subMenu = JMenu(ACTION_NAME)
    subMenu.icon =
      ImageProvider(ICON_NAME)
        .resource
        .getPaddedIcon(ImageProvider.ImageSizes.SMALLICON.imageDimension)

    subMenu.add(selectAction)
    subMenu.add(clickAction)
    subMenu.add(dictionaryAction)
    subMenu.add(deleteTmpAction)
    subMenu.add(numberedStreetAction)

    menu.add(subMenu)
  }
}
