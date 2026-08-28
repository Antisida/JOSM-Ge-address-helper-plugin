package org.openstreetmap.josm.plugins.dl.geaddresshelper.numberstreetgenerator

import java.awt.Component
import javax.swing.JComponent

fun <T : JComponent> T.leftAligned(): T {
  this.alignmentX = Component.LEFT_ALIGNMENT
  return this
}

fun <T : JComponent> T.rightAligned(): T {
  this.alignmentX = Component.RIGHT_ALIGNMENT
  return this
}
