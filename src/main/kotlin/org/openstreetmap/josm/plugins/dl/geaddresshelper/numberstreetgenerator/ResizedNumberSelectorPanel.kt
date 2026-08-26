package org.openstreetmap.josm.plugins.dl.geaddresshelper.numberstreetgenerator

import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.ButtonGroup
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.UIManager
import org.openstreetmap.josm.tools.Logging

class ResizedNumberSelectorPanel() : JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {
    private val buttonGroup = ButtonGroup()
    private val radioButtons = mutableMapOf<Int, JRadioButton>()
    private val spinner = JSpinner(SpinnerNumberModel(11, 3, 50, 1))
    private val maxButtonsCount = 30
    private var isProgrammaticChange = false
    private val pixelsPerCheckbox = calculatePixelsPerCheckbox()

    init {
        alignmentX = LEFT_ALIGNMENT

        // генерация ряда радио-батонов
        for (i in 1..maxButtonsCount) {
            val rb = JRadioButton(i.toString())
            buttonGroup.add(rb)
            radioButtons[i] = rb
            this.add(rb)
        }
        radioButtons[1]?.isSelected = true

        spinner.isEnabled = true
        this.add(spinner)

        this.addComponentListener(
            object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent) {
                    updateCheckboxCount()
                }
            })
    }

    fun getSelectedNumber(): Int {
        for ((num, rb) in radioButtons) {
            if (rb.isSelected) {
                return num
            }
        }
        return spinner.value as Int
    }

    fun setupRadioButtonsListeners(updateAction: () -> Unit) {
        radioButtons.values.forEach { it.addActionListener { updateAction() } }
    }

    fun setupSpinnerListener(updateAction: () -> Unit) {
        spinner.addChangeListener {
            if (!isProgrammaticChange) {
                buttonGroup.clearSelection()
                updateAction()
            }
        }
    }

    private fun updateCheckboxCount() {
        isProgrammaticChange = true

        val selectedNumber = getSelectedNumber()

        val w = this.width
        if (w <= 0) return

        var targetCount = w / pixelsPerCheckbox
        targetCount = Math.max(1, Math.min(maxButtonsCount, targetCount))

        spinner.value = targetCount

        var changed = false
        for (i in 1..maxButtonsCount) {
            val rb = radioButtons[i]
            val shouldBeVisible = i <= targetCount
            if (rb != null && rb.isVisible != shouldBeVisible) {
                rb.isVisible = shouldBeVisible
                changed = true
            }
        }

        radioButtons[selectedNumber]?.setSelected(true)

        // Перерисовываем контейнер
        if (changed) {
            this.revalidate()
            this.repaint()
        }
        isProgrammaticChange = false
    }

    // Разрешаем панели растягиваться по горизонтали без ограничений чтобы лисенер изменения размера
    // работал
    override fun getMaximumSize(): Dimension {
        return Dimension(Int.MAX_VALUE, preferredSize.height)
    }

    /**
     * Рассчитывает точную ширину одной радиокнопки с учетом текущего шрифта и размера системной
     * иконки переключателя.
     */
    private fun calculatePixelsPerCheckbox(): Int {
        val metrics = getFontMetrics(font)

        // 1. Ширина самого длинного текста ("50") под текущий шрифт
        val textWidth = metrics.stringWidth("50")
        Logging.info("textWidth: $textWidth")

        // 2. Ширина иконки радиокнопки из текущей темы (или 16px по умолчанию)
        val iconWidth = UIManager.getIcon("RadioButton.icon")?.iconWidth ?: 16
        Logging.info("iconWidth: $iconWidth")

        // 3. Отступ между иконкой и текстом (обычно 4px) + внешние поля
        val gap = UIManager.getInt("RadioButton.iconTextGap").let { if (it > 0) it else 4 }
        Logging.info("gap: $gap")
        val padding = 8 // левый и правый отступы кнопки

        return textWidth + iconWidth + gap + padding
    }
}
