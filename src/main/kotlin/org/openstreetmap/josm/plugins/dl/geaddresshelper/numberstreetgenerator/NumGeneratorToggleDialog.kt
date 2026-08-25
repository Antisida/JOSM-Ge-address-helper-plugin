package org.openstreetmap.josm.plugins.dl.geaddresshelper.numberstreetgenerator

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JScrollPane
import javax.swing.JTextArea
import org.openstreetmap.josm.actions.JosmAction
import org.openstreetmap.josm.command.Command
import org.openstreetmap.josm.command.SequenceCommand
import org.openstreetmap.josm.data.UndoRedoHandler
import org.openstreetmap.josm.data.osm.DataSelectionListener
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.OsmDataManager
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.event.SelectionEventManager
import org.openstreetmap.josm.gui.SideButton
import org.openstreetmap.josm.gui.dialogs.ToggleDialog
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.CommandHelper
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.dataset.getBuildings
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.dataset.getStreets
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.leftAligned
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.toTags
import org.openstreetmap.josm.tools.I18n
import org.openstreetmap.josm.tools.Shortcut

class NumGeneratorToggleDialog :
  ToggleDialog(
    "Numbered street generator",
    "123.svg",
    //        null,
    "Numbered street generator!",
    null,
    150,
  ),
  DataSelectionListener {

  private val typeGroup = ButtonGroup()
  private val typeMainRadio = JRadioButton("Main")
  private val typeLaneRadio = JRadioButton("Lane")
  private val typeDeadEndRadio = JRadioButton("Dead End")

  /** Для веев */
  private val namesTextArea = JTextArea(4, 30)
  /** Для зданий */
  private val addrStreetTextArea = JTextArea(1, 30)

  private val applyStreetTagsAction = ApplyStreetTagsAction()
  private val applyBuildingTagsAction = ApplyBuildingTagsAction()
  private val copyBuildingTagsAction = CopyBuildingTagsAction()
  private val copyStreetTagsAction = CopyStreetTagsAction()

  private val applyStreetTagsButton = SideButton(applyStreetTagsAction, false)
  private val applyBuildingTagsButton = SideButton(applyBuildingTagsAction, false)
  private val copyBuildingTagsButton = SideButton(copyBuildingTagsAction, false)
  private val copyStreetTagsButton = SideButton(copyStreetTagsAction, false)

  private val mainNumberSelectorPanel = ResizedNumberSelectorPanel()
  private val secondaryNumberSelectorPanel = ResizedNumberSelectorPanel()

  init {
    val mainPanel =
      JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
      }

    mainPanel.add(JLabel("Main street:").apply { font = font.deriveFont(Font.BOLD) }.leftAligned())
    mainPanel.add(mainNumberSelectorPanel.leftAligned())

    // Type selection
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
          maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }
        .leftAligned()
    mainPanel.add(typePanel)

    // --- 3. Secondary number selection ---
    mainPanel.add(secondaryNumberSelectorPanel.leftAligned())

    // --- 4. Tags Section ---
    namesTextArea.isEditable = false
    val namesScrollPane =
      JScrollPane(namesTextArea)
        .apply {
          // запрет изменять высоту
          maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }
        .leftAligned()
    mainPanel.add(namesScrollPane)

    addrStreetTextArea.isEditable = false
    val addrStreetScrollPane =
      JScrollPane(addrStreetTextArea)
        .apply {
          // запрет изменять высоту
          maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }
        .leftAligned()
    mainPanel.add(addrStreetScrollPane)

    add(mainPanel, BorderLayout.CENTER)

    // Подписываем слушатели событий
    setupListeners()

    updateGeneratedText()
    setTitle("Numbered street generator")
    createLayout(
      mainPanel,
      true,
      listOf(
        applyStreetTagsButton,
        applyBuildingTagsButton,
        copyStreetTagsButton,
        copyBuildingTagsButton,
      ),
    )
  }

  private fun setupListeners() {
    val updateAction = { updateGeneratedText() }
    // Слушатели для радио-кнопок
    mainNumberSelectorPanel.setupRadioButtonsListeners(updateAction)
    secondaryNumberSelectorPanel.setupRadioButtonsListeners(updateAction)
    typeMainRadio.addActionListener { updateAction() }
    typeLaneRadio.addActionListener { updateAction() }
    typeDeadEndRadio.addActionListener { updateAction() }

    // Слушатели для спиннеров
    mainNumberSelectorPanel.setupSpinnerListener(updateAction)
    secondaryNumberSelectorPanel.setupSpinnerListener(updateAction)
  }

  private fun updateGeneratedText() {
    val mainNum = mainNumberSelectorPanel.getSelectedNumber()
    val secNum = secondaryNumberSelectorPanel.getSelectedNumber()

    val type =
      when {
        typeMainRadio.isSelected -> GenType.MAIN
        typeLaneRadio.isSelected -> GenType.LANE
        typeDeadEndRadio.isSelected -> GenType.DEAD_END
        else -> throw IllegalArgumentException()
      }

    val kaFull = geName(mainNum, secNum, type)
    val enFull = enName(mainNum, secNum, type)
    val ruFull = ruName(mainNum, secNum, type)

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

    namesTextArea.text = tagsText
    addrStreetTextArea.text = addrText
  }

  inner class ApplyStreetTagsAction :
    JosmAction(
      "Apply name",
      "street_blue.svg",
      "Apply name to selected ways",
      Shortcut.registerShortcut(
        "data:napr_apply_addr_street",
        "Apply name to selected ways",
        KeyEvent.KEY_LOCATION_UNKNOWN,
        Shortcut.NONE,
      ),
      false,
    ) {
    override fun actionPerformed(e: ActionEvent?) {
      val dataSet: DataSet = OsmDataManager.getInstance().editDataSet ?: return
      val streets = dataSet.selected.getStreets()
      if (streets.isNotEmpty()) {
        val commands: MutableList<Command> = mutableListOf()
        commands.addAll(CommandHelper.toChangeCommandsSimple(toTags(namesTextArea.text), streets))
        val res: Command =
          SequenceCommand(I18n.tr("Added node from GeorgiaAddressHelper"), commands)
        UndoRedoHandler.getInstance().add(res)
      }
    }
  }

  inner class ApplyBuildingTagsAction :
    JosmAction(
      "Apply addr:street",
      "building_blue.svg",
      "Apply addr:street to selected buildings",
      Shortcut.registerShortcut(
        "data:napr_apply_addr_street",
        "Apply addr:street to selected buildings",
        KeyEvent.KEY_LOCATION_UNKNOWN,
        Shortcut.NONE,
      ),
      false,
    ) {
    override fun actionPerformed(e: ActionEvent?) {
      val dataSet: DataSet = OsmDataManager.getInstance().editDataSet ?: return
      val buildings = dataSet.selected.getBuildings()
      if (buildings.isNotEmpty()) {
        val commands: MutableList<Command> = mutableListOf()
        commands.addAll(
          CommandHelper.toChangeCommandsSimple(
            mapOf("addr:street" to addrStreetTextArea.text),
            buildings,
          )
        )
        val res: Command =
          SequenceCommand(I18n.tr("Added node from GeorgiaAddressHelper"), commands)
        UndoRedoHandler.getInstance().add(res)
      }
    }
  }

  inner class CopyBuildingTagsAction :
    JosmAction(
      "Copy addr:street",
      "building_grey.svg",
      "Copy addr:street",
      Shortcut.registerShortcut(
        "data:napr_copy_addr_street",
        "Copy addr:street to clipboard",
        KeyEvent.KEY_LOCATION_UNKNOWN,
        Shortcut.NONE,
      ),
      false,
    ) {
    override fun actionPerformed(e: ActionEvent?) {
      copyToClipboard(addrStreetTextArea.text)
    }
  }

  inner class CopyStreetTagsAction :
    JosmAction(
      "Copy name",
      "street_grey.svg",
      "Copy street name",
      Shortcut.registerShortcut(
        "data:napr_copy_name",
        "Copy name",
        KeyEvent.KEY_LOCATION_UNKNOWN,
        Shortcut.NONE,
      ),
      false,
    ) {
    override fun actionPerformed(e: ActionEvent?) {
      copyToClipboard(namesTextArea.text)
    }
  }

  private fun copyToClipboard(text: String) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(StringSelection(text), null)
  }

  @Override
  override fun showNotify() {
    super.showNotify()
    SelectionEventManager.getInstance().addSelectionListener(this)
  }

  @Override
  override fun hideNotify() {
    SelectionEventManager.getInstance().removeSelectionListener(this)
    super.hideNotify()
  }

  override fun destroy() {
    SelectionEventManager.getInstance().removeSelectionListener(this)
    super.destroy()
  }

  override fun selectionChanged(event: DataSelectionListener.SelectionChangeEvent) {
    updateButtonState(event.selection)
  }

  private fun updateButtonState(selection: MutableSet<OsmPrimitive>) {
    if (selection.getStreets().isNotEmpty()) applyStreetTagsAction.setEnabled(true)
    else applyStreetTagsAction.setEnabled(false)
    if (selection.getBuildings().isNotEmpty()) applyBuildingTagsAction.setEnabled(true)
    else applyBuildingTagsAction.setEnabled(false)
  }
}
