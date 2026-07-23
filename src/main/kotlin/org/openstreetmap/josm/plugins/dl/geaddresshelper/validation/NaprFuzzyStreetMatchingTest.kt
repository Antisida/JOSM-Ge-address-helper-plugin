package org.openstreetmap.josm.plugins.dl.geaddresshelper.validation

import org.openstreetmap.josm.command.Command
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.Relation
import org.openstreetmap.josm.data.osm.Way
import org.openstreetmap.josm.data.validation.Severity
import org.openstreetmap.josm.data.validation.Test
import org.openstreetmap.josm.data.validation.TestError
import org.openstreetmap.josm.gui.widgets.JosmTextField
import org.openstreetmap.josm.plugins.dl.geaddresshelper.GeAddressHelperPlugin
import org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers.Address
import org.openstreetmap.josm.plugins.dl.geaddresshelper.parsers.ParsingFlags
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.GeometryHelper
import org.openstreetmap.josm.tools.I18n
import org.openstreetmap.josm.tools.Logging

/**
 * ЕГРН нечеткое совпадение. Плагин сопоставил адрес с улицей или местом по алгоритму нечеткого совпадения,
 * и теперь надо убедиться, что это не ложное срабатывание.
 */

class NaprFuzzyStreetMatchingTest : Test(
    I18n.tr("NAPR fuzzy street match"),
    I18n.tr("NAPR test for parsed street name fuzzy match with OSM")
) {

    private var parsedStreetToPrimitiveMap: Map<String, Pair<Set<OsmPrimitive>, String>> = mutableMapOf()
    private var editedOsmStreetName: String = ""
    private val osmStreetNameEditBox = JosmTextField("")

    override fun visit(w: Way) {
        visitForPrimitive(w)
    }

    override fun visit(r: Relation) {
        visitForPrimitive(r)
    }

    fun visitForPrimitive(p: OsmPrimitive) {
        Logging.info("EGRNFuzzyStreetMatchingTest: $p")
        if (!p.isUsable) return
        if (parsedStreetToPrimitiveMap.isNotEmpty()) return
        GeAddressHelperPlugin.cache.responses.forEach { entry ->
            Logging.info("EGRNFuzzyStreetMatchingTest entry: $entry")
            val primitive: OsmPrimitive = entry.key
            val address: Address = entry.value.address
            Logging.info("address: $address")
            if (address.getValidatedFlags().contains(ParsingFlags.STREET_NAME_FUZZY_MATCH)
//                && !primitive.hasTag("addr:street")
                && !entry.value.isIgnored(EGRNTestCode.EGRN_STREET_FUZZY_MATCHING)
            ) {
                Logging.info("XXXXX: $entry")
                val parsedStreetName = address.parsedStreet.extractedName
                val mostRelevantOsmName = "address.parsedStreet.mostRelevantOsmName"
//                val mostRelevantOsmName = address.parsedStreet.mostRelevantOsmName
                var affectedPrimitives =
                    parsedStreetToPrimitiveMap.getOrDefault(
                        parsedStreetName,
                        Pair(mutableSetOf(), mostRelevantOsmName)
                    ).first
                affectedPrimitives = affectedPrimitives.plus(primitive)
//                affectedPrimitives = affectedPrimitives.plus(address.parsedStreet.matchingPrimitives.toSet())
                parsedStreetToPrimitiveMap = parsedStreetToPrimitiveMap.plus(
                    Pair(
                        parsedStreetName,
                        Pair(affectedPrimitives, mostRelevantOsmName)
                    )
                )
            }
//                }
//            }
        }



        parsedStreetToPrimitiveMap.forEach { entry ->
            val errorPrimitives = entry.value.first
            errorPrimitives.forEach {
                GeAddressHelperPlugin.cache.markProcessed(
                    it,
                    EGRNTestCode.EGRN_STREET_FUZZY_MATCHING
                )
            }
            val highlightPrimitives: List<OsmPrimitive> = errorPrimitives.mapNotNull { p ->
                GeometryHelper.getBiggestPoly(p)
            }
            errors.add(
                TestError.builder(
                    this, Severity.ERROR,
                    EGRNTestCode.EGRN_STREET_FUZZY_MATCHING.code
                )
                    .message(I18n.tr(EGRNTestCode.EGRN_STREET_FUZZY_MATCHING.message) + ": ${entry.key} " + " -> " + entry.value.second)
                    .primitives(errorPrimitives)
                    .highlight(highlightPrimitives)
                    .build()
            )
        }
    }

    override fun fixError(testError: TestError): Command? {
        return null
    }

//    override fun fixError(testError: TestError): Command? {
//
//        val affectedHousenumbers = mutableSetOf<String>()
//        val affectedHighways = mutableSetOf<OsmPrimitive>()
//        var egrnStreetName = ""
//        var osmStreetName = ""
//        val affectedAddresses = mutableListOf<N_ParsedAddress>()
//        testError.primitives.forEach {
//            if (geaddresshelperPlugin.cache.contains(it)) {
//                val nParsedAddresses: N_ParsedAddresses? = geaddresshelperPlugin.cache.get(it)?.address
//                val prefferedAddress = nParsedAddresses?.addresses?.first()
//                affectedAddresses.add(prefferedAddress!!)
//                egrnStreetName = prefferedAddress.parsedStreet.naprStreetName
//                // "${prefferedAddress.parsedStreet.extractedType?.name} ${prefferedAddress.parsedStreet.extractedName}"
//                osmStreetName = prefferedAddress.parsedStreet.mostRelevantOsmName
//                prefferedAddress.parsedHouseNumber.extractedNumber.let { it1 -> affectedHousenumbers.add(it1) }
//            } else {
//                affectedHighways.add(it)
//            }
//        }
//
//        val p = JPanel(GridBagLayout())
//        val label1 = JMultilineLabel(description)
//        label1.setMaxWidth(800)
//        p.add(label1, GBC.eop().anchor(GBC.CENTER).fill(GBC.HORIZONTAL))
//        val infoLabel = JMultilineLabel(
//            "Несколько (${affectedHousenumbers.size}) зданий (номера ${affectedHousenumbers.joinToString(", ")})<br> получили из ЕГРН адрес с именем улицы:<br> <b>${egrnStreetName}</b>, <br>" +
//                    "который был нечетко сопоставлен с именем улицы, существующей в данных ОСМ:<br><b>${osmStreetName}</b> (${affectedHighways.size} линии).<br>" +
//                    "Для разрешения ошибки вы можете присвоить зданиям распознанный адрес,<br> ИЛИ <b>(НЕ РЕКОМЕНДУЕТСЯ)</b>" +
//                    "<br> переименовать улицу соответственно полученными из ЕГРН данным и " +
//                    "<a href =https://wiki.openstreetmap.org/wiki/RU:%D0%A0%D0%BE%D1%81%D1%81%D0%B8%D1%8F/%D0%A1%D0%BE%D0%B3%D0%BB%D0%B0%D1%88%D0%B5%D0%BD%D0%B8%D0%B5_%D0%BE%D0%B1_%D0%B8%D0%BC%D0%B5%D0%BD%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B8_%D0%B4%D0%BE%D1%80%D0%BE%D0%B3>правилам именования улиц в ОСМ</a>" +
//                    "<br>В случае переименования улицы убедитесь в правильности наименования улицы по другим источникам!" +
//                    "<br>Валидными источниками являются постановления местных органов власти о присвоении наименований улицам." +
//                    "<br>(Адрес зданий будет исправлен соответственно.)"
//        )
//        infoLabel.setMaxWidth(600)
//
//        p.add(infoLabel, GBC.eop().anchor(GBC.CENTER).fill(GBC.HORIZONTAL))
//
//        var labelText = "Полученные из ЕГРН адреса: <br>"
//
////        affectedAddresses.forEach {
////            labelText += "${it.egrnAddress},<b> тип: ${if (it.isBuildingAddress()) "здание" else "участок"}</b><br>"
////        }
//
//        val egrnAddressesLabel = JMultilineLabel(labelText, false, true)
//        egrnAddressesLabel.setMaxWidth(800)
//        p.add(egrnAddressesLabel, GBC.eop().anchor(GBC.CENTER).fill(GBC.HORIZONTAL))
//
//        osmStreetNameEditBox.text = egrnStreetName
//        p.add(JLabel(I18n.tr("Переименовать улицу в:")), GBC.std())
//        p.add(osmStreetNameEditBox, GBC.eop().fill(GBC.HORIZONTAL))
//        editedOsmStreetName = osmStreetNameEditBox.text
//
//        val buttonTexts = arrayOf(
//            I18n.tr("Assign address by street") + ": $osmStreetName",
//            I18n.tr("Rename street"),
//            I18n.tr("Cancel")
//        )
//        val dialog = ExtendedDialog(
//            MainApplication.getMainFrame(),
//            I18n.tr("Исправление ошибки нечеткого сопоставления"),
//            *buttonTexts
//        )
//        dialog.setContent(p, false)
//        dialog.setButtonIcons("dialogs/edit", "dialogs/edit", "cancel")
//        dialog.showDialog()
//
//        val answer = dialog.value
//        if (answer == 3) {
//            return null
//        }
//        val cmds: MutableList<Command> = mutableListOf()
//        if (answer == 1) {
//            val filteredPrimitives =
//                testError.primitives.filter { geaddresshelperPlugin.cache.contains(it) }.toMutableList()
//            val doubled = geaddresshelperPlugin.cleanFromDoubles(filteredPrimitives)
//            geaddresshelperPlugin.cache.ignoreValidator(doubled, EGRNTestCode.EGRN_STREET_FUZZY_MATCHING)
//            filteredPrimitives.forEach {
//                val prefferedAddress = geaddresshelperPlugin.cache.get(it)!!.address?.addresses?.first()
////                var tags = prefferedAddress!!.getOsmAddress().getBaseAddressTagsWithSource()
//                var tags = mapOf<String, String>()
////                tags = tags.plus(Pair("addr:RU:egrn", prefferedAddress.egrnAddress))
//                tags = tags.plus(Pair("addr:RU:egrn", "prefferedAddress.egrnAddress"))
//                cmds.add(ChangePropertyCommand(listOf(it), tags))
//            }
//        }
//
////        if (answer == 2) {
////            //переименование улицы.
////            editedOsmStreetName = osmStreetNameEditBox.text
////            val highways = testError.primitives.filter { it.hasTag("highway") }
////            highways.forEach {
////                cmds.add(ChangePropertyCommand(it, "name", editedOsmStreetName))
////                cmds.add(ChangePropertyCommand(it, "source:name", "ЕГРН"))
////            }
////
////            val filteredPrimitives =
////                testError.primitives.filter { geaddresshelperPlugin.cache.contains(it) }.toMutableList()
////            val doubled = geaddresshelperPlugin.cleanFromDoubles(filteredPrimitives)
////            geaddresshelperPlugin.cache.ignoreValidator(doubled, EGRNTestCode.EGRN_STREET_FUZZY_MATCHING)
////            filteredPrimitives.forEach {
////                val preferredAddress = geaddresshelperPlugin.cache.get(it)!!.addressInfo?.getPreferredAddress()
////                var tags = preferredAddress!!.getOsmAddress().getBaseAddressTagsWithSource()
////                tags = tags.plus(Pair("addr:street", editedOsmStreetName))
////                tags = tags.plus(Pair("addr:RU:egrn", preferredAddress.egrnAddress))
////                cmds.add(ChangePropertyCommand(listOf(it), tags))
////            }
////        }
//
//        if (cmds.isNotEmpty()) {
//            val c: Command = SequenceCommand(I18n.tr("Added tags from geaddresshelper FuzzyMatch validator"), cmds)
//            return c
//        }
//
//        return null
//    }

    override fun endTest() {
        parsedStreetToPrimitiveMap = mutableMapOf()
        super.endTest()
    }

    override fun isFixable(testError: TestError): Boolean {
        return testError.tester is NaprFuzzyStreetMatchingTest
//        return false
    }

}
