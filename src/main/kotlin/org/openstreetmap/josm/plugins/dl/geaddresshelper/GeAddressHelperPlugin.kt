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
import org.openstreetmap.josm.plugins.dl.geaddresshelper.actions.ClickAction
import org.openstreetmap.josm.plugins.dl.geaddresshelper.actions.DeleteTmpAction
import org.openstreetmap.josm.plugins.dl.geaddresshelper.actions.DictionaryAction
import org.openstreetmap.josm.plugins.dl.geaddresshelper.actions.SelectAction
import org.openstreetmap.josm.plugins.dl.geaddresshelper.settings.io.CommonSettingsReader
import org.openstreetmap.josm.plugins.dl.geaddresshelper.settings.io.ValidationSettingsReader
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.GeometryHelper
import org.openstreetmap.josm.plugins.dl.geaddresshelper.uploadhooks.EGRNCleanPluginCache
import org.openstreetmap.josm.plugins.dl.geaddresshelper.uploadhooks.EGRNUploadTagFilter
import org.openstreetmap.josm.plugins.dl.geaddresshelper.validation.N_ValidationCache
import org.openstreetmap.josm.plugins.dl.geaddresshelper.validation.NaprFuzzyStreetMatchingTest
import org.openstreetmap.josm.tools.Geometry
import org.openstreetmap.josm.tools.I18n
import org.openstreetmap.josm.tools.ImageProvider
import org.openstreetmap.josm.tools.Logging

class GeAddressHelperPlugin(info: PluginInformation) : Plugin(info) {
  //
  //    val fuel: FuelManager = FuelManager()
  init {
    menuInit(MainApplication.getMenu().dataMenu)

    versionInfo = info.version
    //
    //        FuelManager.instance.basePath = "https://example.com"
    //        FuelManager.instance.timeoutInMillisecond = 15000
    //        FuelManager.instance.baseHeaders = mapOf(
    //            "Accept" to "application/json"
    //        )

    cache.initListener()
    //            addressRegistry.initListener()
  }

  companion object {
    val ACTION_NAME = I18n.tr("Georgia address helper")!!
    val ICON_NAME = "icon.svg"

    lateinit var versionInfo: String

    val cache: N_ValidationCache = N_ValidationCache()
    //  val addressRegistry: AddressRegistryCache = AddressRegistryCache()

    val egrnUploadTagFilterHook: EGRNUploadTagFilter = EGRNUploadTagFilter()
    val cleanPluginCacheHook: EGRNCleanPluginCache = EGRNCleanPluginCache()

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
      Logging.info("runEgrnValidation: $egrnTests $selection")
      MainApplication.worker.submit(ValidationTask(egrnTests, selection, null))
    }

    fun cleanFromDoubles(primitives: MutableList<OsmPrimitive>): Set<OsmPrimitive> {
      // получает на вход мутабельный список примитивов, которым хотим присвоить адрес.
      // удаляем из него все примитивы, для которых есть дубликат предпочитаемого адреса в ОСМ или
      // среди них самих
      // возвращаем список дублей
      //            val needToAssignAddressPrimitives = primitives.filter {
      //                cache.contains(it)
      //                        && cache.get(it)?.addressInfo?.getPreferredAddress() != null
      //                        && !it.hasKey("addr:housenumber")
      //            }
      //            val needToAssignAddressPrimitivesMap = needToAssignAddressPrimitives.groupBy {
      // getParsedInlineAddress(it) }
      //
      //            val doubleAddressPrimitives: MutableSet<OsmPrimitive> = mutableSetOf()
      //            val osmBuildingsAddressMap = getOsmAddressesMap()
      //
      //            needToAssignAddressPrimitivesMap.forEach { (address, listToProcess) ->
      //                if (listToProcess.isEmpty()) return@forEach
      //                if (osmBuildingsAddressMap.containsKey(address)) {
      //                    //уже есть дубль в данных ОСМ
      //                    primitives.removeAll(listToProcess)
      //                    doubleAddressPrimitives.addAll(listToProcess)
      //                    return@forEach
      //                }
      //
      //                val assignToPrimitive = listToProcess.filterIsInstance<Way>().maxByOrNull {
      // Geometry.computeArea(it) }
      //                if (assignToPrimitive == null) {
      //                    Logging.error("EGRN PLUGIN Something went wrong when finding doubles,
      // building has no area")
      //                    primitives.removeAll(listToProcess)
      //                    return@forEach
      //                }
      //                val doubles = listToProcess.minus(assignToPrimitive)
      //                primitives.removeAll(doubles)
      //                doubleAddressPrimitives.addAll(doubles)
      //            }
      //
      //            cache.markProcessed(doubleAddressPrimitives,
      // EGRNTestCode.EGRN_ADDRESS_DOUBLE_FOUND)
      //            return doubleAddressPrimitives
      return primitives.toSet()
    }

    fun cleanFromDoublesWithRespectForDistance(
        primitives: MutableList<OsmPrimitive>
    ): Set<OsmPrimitive> {
      // этот подход к удалению дубликатов не лучший
      // цель - не допустить создания дубликатов адресов если они получены для зданий в пределах
      // одного НП,
      // но при этом мы никак не проверяем наличие НП, его границы или размеры.
      // получает на вход мутабельный список примитивов, которым хотим присвоить адрес.
      // удаляем из него все примитивы, для которых есть дубликат предпочитаемого адреса в ОСМ
      // или среди них самих
      // с учетом расстояния поиска дублей. Адрес считается дублем,
      // если есть обьект с таким же адресом на расстоянии меньше заданного в настройках
      // возвращаем список дублей
      //            val needToAssignAddressPrimitives = primitives.filter {
      //                cache.contains(it)
      //                        && cache.get(it)?.addressInfo?.getPreferredAddress() != null
      //                        && !it.hasKey("addr:housenumber")
      //            }
      //            val needToAssignAddressPrimitivesMap = needToAssignAddressPrimitives.groupBy {
      // getParsedInlineAddress(it) }
      //            val isAddressByPlace =
      //                needToAssignAddressPrimitives.associate { Pair(getParsedInlineAddress(it),
      // isPlaceAddress(it)) }
      //
      //            val doubleAddressPrimitives: MutableSet<OsmPrimitive> = mutableSetOf()
      //            val osmBuildingsAddressMap = getOsmAddressesMap()
      //
      //            needToAssignAddressPrimitivesMap.forEach { (address, listToProcess) ->
      //                if (listToProcess.isEmpty()) return@forEach
      //                if (osmBuildingsAddressMap.containsKey(address)) {
      //                    //уже есть дубль в данных ОСМ
      //                    //ищем примитивы которые слишком близко
      //                    val realDoubles = getDuplicatesByDistance(
      //                        osmBuildingsAddressMap[address], listToProcess,
      //                        isAddressByPlace[address]
      //                    )
      //                    primitives.removeAll(realDoubles)
      //                    doubleAddressPrimitives.addAll(realDoubles)
      //                    return@forEach
      //                }
      //
      //                val assignToPrimitive = listToProcess.filterIsInstance<Way>().maxByOrNull {
      // Geometry.computeArea(it) }
      //                if (assignToPrimitive == null) {
      //                    Logging.error("EGRN PLUGIN Something went wrong when finding doubles,
      // building has no area")
      //                    primitives.removeAll(listToProcess)
      //                    return@forEach
      //                }
      //                val doubles = listToProcess.minus(assignToPrimitive)
      //                primitives.removeAll(doubles)
      //                doubleAddressPrimitives.addAll(doubles)
      //            }
      //            cache.markProcessed(doubleAddressPrimitives,
      // EGRNTestCode.EGRN_ADDRESS_DOUBLE_FOUND)
      //            return doubleAddressPrimitives
      return primitives.toSet()
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

    /*  fun findDoubledAddresses(addresses: MutableList<ParsedAddress>): Set<ParsedAddress> {
        //получает на вход мутабельный список адресов, которым хотим проверить на дубликаты среди данных ОСМ.
        //удаляем из него все примитивы, для которых есть дубликат предпочитаемого адреса в ОСМ
        //возвращаем список дублей

        val doubleAddresses: MutableSet<ParsedAddress> = mutableSetOf()
        val osmBuildingsAddressMap = getOsmAddressesMap()

        addresses.forEach {
            val inlineAddress = it.getOsmAddress().getInlineAddress(",", true)
            if (osmBuildingsAddressMap.containsKey(inlineAddress)) {
                //уже есть дубль в данных ОСМ
                doubleAddresses.add(it)
            }
        }
        return doubleAddresses
    }*/

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

    //        private fun getParsedInlineAddress(primitive: OsmPrimitive): String {
    //            val prefAddress = cache.get(primitive)?.addressInfo?.getPreferredAddress() ?:
    // return ""
    //            return prefAddress.getOsmAddress().getInlineAddress(",") ?: ""
    //        }
    //
    //        private fun isPlaceAddress(primitive: OsmPrimitive): Boolean {
    //            val prefAddress = cache.get(primitive)?.addressInfo?.getPreferredAddress() ?:
    // return false
    //            return if (prefAddress.isMatchedByStreet()) {
    //                false
    //            } else prefAddress.isMatchedByPlace()
    //        }

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

    UploadAction.registerUploadHook(cleanPluginCacheHook, true)
    UploadAction.registerUploadHook(egrnUploadTagFilterHook, true)
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
