package org.openstreetmap.josm.plugins.dl.geaddresshelper

import org.openstreetmap.josm.actions.UploadAction
import org.openstreetmap.josm.command.AddCommand
import org.openstreetmap.josm.command.SequenceCommand
import org.openstreetmap.josm.data.UndoRedoHandler
import org.openstreetmap.josm.data.coor.EastNorth
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.validation.OsmValidator
import org.openstreetmap.josm.data.validation.ValidationTask
import org.openstreetmap.josm.gui.MainApplication
import org.openstreetmap.josm.gui.MapFrame
import org.openstreetmap.josm.plugins.Plugin
import org.openstreetmap.josm.plugins.PluginInformation
import org.openstreetmap.josm.plugins.dl.geaddresshelper.deletion.DeleteTmpAction
import org.openstreetmap.josm.plugins.dl.geaddresshelper.deletion.UploadFilter
import org.openstreetmap.josm.plugins.dl.geaddresshelper.dictionary.DictionaryAction
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.actions.ClickAction
import org.openstreetmap.josm.plugins.dl.geaddresshelper.napr.actions.SelectAction
import org.openstreetmap.josm.plugins.dl.geaddresshelper.numberstreetgenerator.NumGeneratorToggleDialog
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.GeometryHelper
import org.openstreetmap.josm.plugins.dl.geaddresshelper.uploadhooks.PluginCacheCleaner
import org.openstreetmap.josm.plugins.dl.geaddresshelper.validation.N_ValidationCache
import org.openstreetmap.josm.plugins.dl.geaddresshelper.validation.NaprFuzzyStreetMatchingTest
import org.openstreetmap.josm.tools.I18n
import org.openstreetmap.josm.tools.ImageProvider
import javax.swing.JMenu

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

    fun createDebugObject(coords: ArrayList<ArrayList<Double>>, requestCoord: EastNorth) {
      val map = MainApplication.getMap()
      val ds = map.mapView.layerManager.editDataSet
      val cmds = GeometryHelper.createPolygon(ds, coords, false).first
      cmds.add(AddCommand(ds, Node(requestCoord)))
      UndoRedoHandler.getInstance().add(SequenceCommand("Add debug geometry", cmds))
    }
  }

  override fun mapFrameInitialized(oldFrame: MapFrame?, newFrame: MapFrame?) {
    // this callback fired also everytime last layer is removed, cannot run layer listeners init
    // here
    OsmValidator.addTest(NaprFuzzyStreetMatchingTest::class.java)

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

    menu.add(subMenu)
  }
}
