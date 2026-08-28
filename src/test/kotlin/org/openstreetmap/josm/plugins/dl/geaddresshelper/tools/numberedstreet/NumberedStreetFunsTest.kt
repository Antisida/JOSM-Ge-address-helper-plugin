package org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.numberedstreet

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.openstreetmap.josm.plugins.dl.geaddresshelper.numberstreetgenerator.StreetType
import org.openstreetmap.josm.plugins.dl.geaddresshelper.numberstreetgenerator.geName

class NumberedStreetFunsTest {

  @ParameterizedTest(name = "\"{3}\"")
  @CsvSource(
      "1, 1, LANE, 1-ლი ქუჩის I შესახვევი",
      "1, 1, MAIN, 1-ლი ქუჩა",
      "2, 1, MAIN, მე-2 ქუჩა",
      "21, 1, MAIN, 21-ე ქუჩა",
      "2, 2, LANE, მე-2 ქუჩის II შესახვევი",
      "22, 22, LANE, 22-ე ქუჩის XXII შესახვევი",
      "22, 22, DEAD_END, 22-ე ქუჩის XXII ჩიხი",
      "2, 2, DEAD_END, მე-2 ქუჩის II ჩიხი",
  )
  fun testGeName(first: Int, second: Int, typeName: String, expected: String) {
    val actual = geName(first, second, StreetType.valueOf(typeName))
    assertEquals(expected, actual)
  }

  @Test fun enName() {}

  @Test fun ruName() {}
}
