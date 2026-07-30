package org.openstreetmap.josm.plugins.dl.geaddresshelper.actions

import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ItemEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.JTextArea
import javax.swing.SpinnerNumberModel

class StreetNameDialog(owner: Frame, streetsSize: Int, buildingsSize: Int) :
    JDialog(owner, "Numbered street name generator", true) {
  // элементы управления
  var isApproved: Boolean = false
    private set

  // Элементы UI
  private val mainStreetGroup = ButtonGroup()
  private val mainRadioButtons = mutableMapOf<Int, JRadioButton>()
  private val mainCustomSpinner = JSpinner(SpinnerNumberModel(11, 11, 50, 1))

  private val typeGroup = ButtonGroup()
  private val typeMainRadio = JRadioButton("Main")
  private val typeLaneRadio = JRadioButton("Lane")
  private val typeDeadEndRadio = JRadioButton("Dead End")

  private val secondaryGroup = ButtonGroup()
  private val secondaryRadioButtons = mutableMapOf<Int, JRadioButton>()
  private val secondaryCustomSpinner = JSpinner(SpinnerNumberModel(11, 11, 50, 1))

  private val tagsTextArea = JTextArea(4, 30)
  val waysCheckbox = JCheckBox("Apply to selected ways", false)

  private val addrStreetTextArea = JTextArea(1, 30)
  val buildingsCheckbox = JCheckBox("Apply to selected buildings", false)

  // Результат, который можно забрать после закрытия диалога по OK
  var resultTags: String? = null
    private set

  var resultAddrStreet: String? = null
    private set

  init {
    defaultCloseOperation = DISPOSE_ON_CLOSE
    layout = BorderLayout(2, 2)

    val mainPanel =
        JPanel().apply {
          layout = BoxLayout(this, BoxLayout.Y_AXIS)
          border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        }

    // --- 1. Main street selection ---
    mainPanel.add(JLabel("Main street:").apply { font = font.deriveFont(Font.BOLD) }.leftAligned())
    mainPanel.add(
        createNumberSelectorPanel(mainStreetGroup, mainRadioButtons, mainCustomSpinner)
            .leftAligned()
    )
//    mainPanel.add(Box.createVerticalStrut(10))

    // --- 2. Type selection ---
//    mainPanel.add(JLabel("Type:").apply { font = font.deriveFont(Font.BOLD) }.leftAligned())
    val typePanel =
        JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
            .apply {
              typeGroup.add(typeMainRadio)
              typeGroup.add(typeLaneRadio)
              typeGroup.add(typeDeadEndRadio)
              typeMainRadio.isSelected = true

              add(JLabel("Type:").apply { font = font.deriveFont(Font.BOLD) }.leftAligned())
              add(typeMainRadio)
              add(typeLaneRadio)
              add(typeDeadEndRadio)
            }
            .leftAligned()

    mainPanel.add(typePanel)
//    mainPanel.add(Box.createVerticalStrut(10))

    mainCustomSpinner.isEnabled = true

    // --- 3. Secondary number selection ---
    mainPanel.add(
        JLabel("Secondary number:").apply { font = font.deriveFont(Font.BOLD) }.leftAligned()
    )
    mainPanel.add(
        createNumberSelectorPanel(secondaryGroup, secondaryRadioButtons, secondaryCustomSpinner)
            .leftAligned()
    )
//    mainPanel.add(Box.createVerticalStrut(15))
    secondaryCustomSpinner.isEnabled = true

    // --- 4. Tags Section ---
//    mainPanel.add(JLabel("Tags").apply { font = font.deriveFont(Font.BOLD) }.leftAligned())
    tagsTextArea.isEditable = false
    mainPanel.add(JScrollPane(tagsTextArea).leftAligned())

    val copyTagsBtn =
        JButton("Copy tags")
            .apply { addActionListener { copyToClipboard(tagsTextArea.text) } }
            .leftAligned()

    waysCheckbox.isEnabled = streetsSize > 0
    val copyTagsRow =
        Box.createHorizontalBox()
            .apply {
              add(copyTagsBtn)
              add(Box.createHorizontalGlue()) // Расталкивает влево и вправо
              add(waysCheckbox)
            }
            .leftAligned()

    mainPanel.add(copyTagsRow)

//    mainPanel.add(Box.createVerticalStrut(10))

    // --- 5. addr:street Section ---
//    mainPanel.add(JLabel("addr:street").apply { font = font.deriveFont(Font.BOLD) }.leftAligned())
    addrStreetTextArea.isEditable = false
    mainPanel.add(JScrollPane(addrStreetTextArea).leftAligned())

    val copyAddrBtn =
        JButton("Copy addr")
            .apply { addActionListener { copyToClipboard(addrStreetTextArea.text) } }
            .leftAligned()

    buildingsCheckbox.isEnabled = buildingsSize > 0
    val copyAddrRow =
        Box.createHorizontalBox()
            .apply {
              add(copyAddrBtn)
              add(Box.createHorizontalGlue()) // Расталкивает влево и вправо
              add(buildingsCheckbox)
            }
            .leftAligned()

    mainPanel.add(copyAddrRow)

    add(mainPanel, BorderLayout.CENTER)

    // --- Bottom OK / Cancel Panel ---
    val buttonPanel =
        JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
          val okButton =
              JButton("OK").apply {
                addActionListener {
                  isApproved = true
                  resultTags = tagsTextArea.text
                  resultAddrStreet = addrStreetTextArea.text
                  dispose()
                }
              }
          val cancelButton =
              JButton("Cancel").apply {
                addActionListener {
                  isApproved = false
                  resultTags = null
                  resultAddrStreet = null
                  dispose()
                }
              }
          add(okButton)
          add(cancelButton)
        }
    add(buttonPanel, BorderLayout.SOUTH)

    // Подписываем слушатели событий
    setupListeners()

    // Дефолтные значения (Main Street = 1, Type = Main, Secondary = 1)
    mainRadioButtons[1]?.isSelected = true
    secondaryRadioButtons[1]?.isSelected = true

    updateGeneratedText()
    pack()
    setLocationRelativeTo(owner)
  }

  private fun createNumberSelectorPanel(
      group: ButtonGroup,
      radioMap: MutableMap<Int, JRadioButton>,
      spinner: JSpinner,
  ): JPanel {
    val panel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))

    for (i in 1..10) {
      val rb = JRadioButton(i.toString())
      group.add(rb)
      radioMap[i] = rb
      panel.add(rb)
    }

    val customRb =
        JRadioButton()
            .apply {
              // Если выбран кастомный радио-баттон, активируем спиннер
              addItemListener { e -> spinner.isEnabled = (e.stateChange == ItemEvent.SELECTED) }
            }
            .leftAligned()
    group.add(customRb)
    radioMap[0] = customRb // 0 как маркер для Custom

    spinner.isEnabled = false
//    panel.add(JLabel(" >7"))
    panel.add(spinner)

    return panel
  }

  private fun setupListeners() {
    val updateAction = { updateGeneratedText() }

    // Слушатели для радио-кнопок
    mainRadioButtons.values.forEach { it.addActionListener { updateAction() } }
    secondaryRadioButtons.values.forEach { it.addActionListener { updateAction() } }

    typeMainRadio.addActionListener { updateAction() }
    typeLaneRadio.addActionListener { updateAction() }
    typeDeadEndRadio.addActionListener { updateAction() }

    // Слушатели для спиннеров
    mainCustomSpinner.addChangeListener {
      // Если значение изменилось, снимаем выбор со всех радио-кнопок
      mainStreetGroup.clearSelection()
      updateGeneratedText()
    }
    secondaryCustomSpinner.addChangeListener {
      // Если значение изменилось, снимаем выбор со всех радио-кнопок
      secondaryGroup.clearSelection()
      updateGeneratedText()
    }
  }

  private fun getSelectedNumber(radioMap: Map<Int, JRadioButton>, spinner: JSpinner): Int {
    for ((num, rb) in radioMap) {
      if (rb.isSelected) {
        return num
      }
    }
    return spinner.value as Int
  }

  private fun updateGeneratedText() {
    val mainNum = getSelectedNumber(mainRadioButtons, mainCustomSpinner)
    val secNum = getSelectedNumber(secondaryRadioButtons, secondaryCustomSpinner)

    val isMainType = typeMainRadio.isSelected
    val isLaneType = typeLaneRadio.isSelected
    val isDeadEndType = typeDeadEndRadio.isSelected

    // --- 1. Georgian ---
    val kaMainStr = if (mainNum == 1) "1-ლი ქუჩა" else "მე-$mainNum ქუჩის"
    val kaFull =
        when {
          isMainType -> if (mainNum == 1) "1-ლი ქუჩა" else "მე-$mainNum ქუჩა"
          isLaneType -> "$kaMainStr ${toRoman(secNum)} შესახვევი"
          isDeadEndType -> "$kaMainStr ${toRoman(secNum)} ჩიხი"
          else -> ""
        }

    // --- 2. English ---
    val enMainSuffix = getEnglishOrdinalSuffix(mainNum)
    val enSecSuffix = getEnglishOrdinalSuffix(secNum)
    val enFull =
        when {
          isMainType -> "$mainNum$enMainSuffix Street"
          isLaneType -> "$mainNum$enMainSuffix Street's $secNum$enSecSuffix Lane"
          isDeadEndType -> "$mainNum$enMainSuffix Street's $secNum$enSecSuffix Dead End"
          else -> ""
        }

    // --- 3. Russian ---
    val ruMainStr = "$mainNum-я улица"
    val ruMainGenitive = "$mainNum-й улицы"
    val ruFull =
        when {
          isMainType -> ruMainStr
          isLaneType -> "$secNum-й переулок $ruMainGenitive"
          isDeadEndType -> "$secNum-й тупик $ruMainGenitive"
          else -> ""
        }

    // Сборка текста
    val tagsText =
        """
            name=$kaFull
            name:ka=$kaFull
            name:en=$enFull
            name:ru=$ruFull
        """
            .trimIndent()

    val addrText = "addr:street=$kaFull"

    tagsTextArea.text = tagsText
    addrStreetTextArea.text = addrText
  }

  private fun getEnglishOrdinalSuffix(n: Int): String {
    if (n % 100 in 11..13) return "th"
    return when (n % 10) {
      1 -> "st"
      2 -> "nd"
      3 -> "rd"
      else -> "th"
    }
  }

  private fun toRoman(number: Int): String {
    val romanNumerals =
        listOf(
//            1000 to "M",
//            900 to "CM",
//            500 to "D",
//            400 to "CD",
//            100 to "C",
//            90 to "XC",
            50 to "L",
            40 to "XL",
            10 to "X",
            9 to "IX",
            5 to "V",
            4 to "IV",
            1 to "I",
        )
    var n = number
    val result = StringBuilder()
    for ((value, numeral) in romanNumerals) {
      while (n >= value) {
        result.append(numeral)
        n -= value
      }
    }
    return result.toString()
  }

  private fun copyToClipboard(text: String) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(StringSelection(text), null)
  }
}

private fun <T : JComponent> T.leftAligned(): T {
  this.alignmentX = Component.LEFT_ALIGNMENT
  return this
}

private fun <T : JComponent> T.rightAligned(): T {
  this.alignmentX = Component.RIGHT_ALIGNMENT
  return this
}
