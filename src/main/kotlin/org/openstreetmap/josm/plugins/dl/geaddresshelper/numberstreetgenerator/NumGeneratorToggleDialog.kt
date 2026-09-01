package org.openstreetmap.josm.plugins.dl.geaddresshelper.numberstreetgenerator

import org.openstreetmap.josm.actions.JosmAction
import org.openstreetmap.josm.command.SequenceCommand
import org.openstreetmap.josm.data.UndoRedoHandler
import org.openstreetmap.josm.data.osm.DataSelectionListener
import org.openstreetmap.josm.data.osm.OsmDataManager
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.OsmPrimitiveType
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter
import org.openstreetmap.josm.data.osm.event.DatasetEventManager
import org.openstreetmap.josm.data.osm.event.SelectionEventManager
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent
import org.openstreetmap.josm.gui.SideButton
import org.openstreetmap.josm.gui.dialogs.ToggleDialog
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.CommandHelper
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.funs.getBuildings
import org.openstreetmap.josm.plugins.dl.geaddresshelper.tools.funs.getStreets
import org.openstreetmap.josm.tools.I18n
import org.openstreetmap.josm.tools.Shortcut
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.ItemEvent
import java.awt.event.KeyEvent
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JScrollPane
import javax.swing.JTextArea

class NumGeneratorToggleDialog : ToggleDialog(
    "Numbered street generator", "123.svg", "Numbered street generator!", null, 150
), DataSelectionListener, DataSetListenerAdapter.Listener {

    //для активации кнопок при добавлении тега highway
    private val dataChangedAdapter = DataSetListenerAdapter(this)

    private val typeGroup = ButtonGroup()
    private val typeMainRadio = JRadioButton("Main")
    private val typeLaneRadio = JRadioButton("Lane")
    private val typeDeadEndRadio = JRadioButton("Dead End")

    /** Для улиц, заполняется теги name, name:ka, name:en, name:ru*/
    private val namesTextArea = JTextArea(4, 30)

    /** Для зданий, заполняется тегом addr:street */
    private val addrStreetTextArea = JTextArea(1, 30)

    private val applyStreetTagsAction = ApplyStreetTagsAction()
    private val applyBuildingTagsAction = ApplyBuildingTagsAction()
    private val copyBuildingTagsAction = CopyBuildingTagsAction()
    private val copyStreetTagsAction = CopyStreetTagsAction()

    private val applyStreetTagsBtn = SideButton(applyStreetTagsAction, false)
    private val applyBuildingTagsBtn = SideButton(applyBuildingTagsAction, false)
    private val copyBuildingTagsBtn = SideButton(copyBuildingTagsAction, false)
    private val copyStreetTagsBtn = SideButton(copyStreetTagsAction, false)

    private val mainNumberSelectorPanel = ResizedNumberSelectorPanel()
    private val secondaryNumberSelectorPanel = ResizedNumberSelectorPanel()

    init {
        val mainPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        }

        mainPanel.add(JLabel("Main street:").apply { font = font.deriveFont(Font.BOLD) }.leftAligned())
        mainPanel.add(mainNumberSelectorPanel.leftAligned())

        // Type selection
        val typePanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            typeGroup.add(typeMainRadio)
            typeGroup.add(typeLaneRadio)
            typeGroup.add(typeDeadEndRadio)
            typeMainRadio.isSelected = true

            add(JLabel("Type:").apply { font = font.deriveFont(Font.BOLD) }.leftAligned())
            add(typeMainRadio)
            add(typeLaneRadio)
            add(typeDeadEndRadio)
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }.leftAligned()
        mainPanel.add(typePanel)

        // --- 3. Secondary number selection ---
        mainPanel.add(secondaryNumberSelectorPanel.leftAligned())

        // --- 4. Tags Section ---
        namesTextArea.isEditable = false
        val namesScrollPane =
            JScrollPane(namesTextArea).apply { maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height) } // запрет изменять высоту
                .leftAligned()
        mainPanel.add(namesScrollPane)

        addrStreetTextArea.isEditable = false
        val addrStreetScrollPane =
            JScrollPane(addrStreetTextArea).apply { maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height) }  // запрет изменять высоту
                .leftAligned()
        mainPanel.add(addrStreetScrollPane)

        add(mainPanel, BorderLayout.CENTER)

        setupListeners()

        updateGeneratedText()
        setTitle("Numbered street generator")
        createLayout(mainPanel, true, listOf(applyStreetTagsBtn, applyBuildingTagsBtn, copyStreetTagsBtn, copyBuildingTagsBtn))
    }

    private fun setupListeners() {
        val updateAction = { updateGeneratedText() }
        val udpataActionCond =
        // Слушатели для радио-кнопок
        mainNumberSelectorPanel.setupRadioButtonsListeners(updateAction)
        secondaryNumberSelectorPanel.setupRadioButtonsListeners(updateAction)

        typeMainRadio.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                updateAction()
            }
        }
        typeLaneRadio.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                updateAction()
            }
        }
        typeDeadEndRadio.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                updateAction()
            }
        }

        // Слушатели для спиннеров
        mainNumberSelectorPanel.setupSpinnerListener(updateAction)
        secondaryNumberSelectorPanel.setupSpinnerListener(updateAction)
    }

    private fun updateGeneratedText() {
        val mainNum = mainNumberSelectorPanel.getValue()
        val secNum = secondaryNumberSelectorPanel.getValue()

        val type = when {
            typeMainRadio.isSelected -> StreetType.MAIN
            typeLaneRadio.isSelected -> StreetType.LANE
            typeDeadEndRadio.isSelected -> StreetType.DEAD_END
            else -> throw IllegalArgumentException()
        }

        val kaFull = geName(mainNum, secNum, type)
        val enFull = enName(mainNum, secNum, type)
        val ruFull = ruName(mainNum, secNum, type)

        val tagsText = """
            name=$kaFull
            name:ka=$kaFull
            name:en=$enFull
            name:ru=$ruFull
        """.trimIndent()

        val addrText = "addr:street=$kaFull"

        namesTextArea.text = tagsText
        addrStreetTextArea.text = addrText
    }

    inner class ApplyStreetTagsAction : JosmAction(
        "Apply name", "street_blue.svg", "Apply name to selected ways", Shortcut.registerShortcut(
            "data:napr_apply_addr_street", "Apply name to selected ways", KeyEvent.KEY_LOCATION_UNKNOWN, Shortcut.NONE
        ), false
    ) {
        override fun actionPerformed(e: ActionEvent?) {
            val streets = OsmDataManager.getInstance().editDataSet?.selected?.getStreets() ?: return
            if (streets.isEmpty()) return

            val commands = CommandHelper.toChangeCommandsSimple(toTags(namesTextArea.text), streets)
            val sequenceCommand = SequenceCommand(I18n.tr("Added node from GeorgiaAddressHelper"), commands)
            UndoRedoHandler.getInstance().add(sequenceCommand)
        }
    }

    inner class ApplyBuildingTagsAction : JosmAction(
        "Apply addr:street", "building_blue.svg", "Apply addr:street to selected buildings", Shortcut.registerShortcut(
            "data:napr_apply_addr_street",
            "Apply addr:street to selected buildings",
            KeyEvent.KEY_LOCATION_UNKNOWN,
            Shortcut.NONE,
        ), false
    ) {
        override fun actionPerformed(e: ActionEvent?) {
            val buildings = OsmDataManager.getInstance().editDataSet?.selected?.getBuildings() ?: return
            if (buildings.isEmpty()) return

            val commands = CommandHelper.toChangeCommandsSimple(toTags(addrStreetTextArea.text), buildings)
            val sequenceCommand = SequenceCommand(I18n.tr("Added node from GeorgiaAddressHelper"), commands)
            UndoRedoHandler.getInstance().add(sequenceCommand)
        }
    }

    inner class CopyBuildingTagsAction : JosmAction(
        "Copy addr:street", "building_grey.svg", "Copy addr:street",
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

    inner class CopyStreetTagsAction : JosmAction(
        "Copy name", "street_grey.svg", "Copy street name", Shortcut.registerShortcut(
            "data:napr_copy_name",
            "Copy name",
            KeyEvent.KEY_LOCATION_UNKNOWN,
            Shortcut.NONE,
        ), false
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
        DatasetEventManager.getInstance().addDatasetListener(dataChangedAdapter, DatasetEventManager.FireMode.IMMEDIATELY)
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
        val selection: Set<OsmPrimitive> = event.selection
        updateButtonState(selection)
        updateCheckboxState(selection)
    }

    private fun updateCheckboxState(selection: Set<OsmPrimitive>) {
        val streetNames = selection.mapNotNull {
            when {
                it.hasKey("highway") -> it["name"]
                it.hasKey("building") -> it["addr:street"]
                else -> null
            }
        }.distinct()

        if (streetNames.size == 1) {
            val parseData = parseData(streetNames[0])
            if (parseData != null) {
                mainNumberSelectorPanel.setValue(parseData.mainNum)
                if (parseData.secNum != null) {
                    secondaryNumberSelectorPanel.setValue(parseData.secNum)
                } else {
                    secondaryNumberSelectorPanel.setValue(1)
                }
                when (parseData.type) {
                    StreetType.MAIN -> typeMainRadio.isSelected = true
                    StreetType.LANE -> typeLaneRadio.isSelected = true
                    StreetType.DEAD_END -> typeDeadEndRadio.isSelected = true
                }
            }
        }
    }

    private fun updateButtonState(selection: Set<OsmPrimitive>) {
        if (selection.getStreets().isNotEmpty()) applyStreetTagsAction.setEnabled(true)
        else applyStreetTagsAction.setEnabled(false)
        if (selection.getBuildings().isNotEmpty()) applyBuildingTagsAction.setEnabled(true)
        else applyBuildingTagsAction.setEnabled(false)
    }

    /**  Нужен для активации кнопок при добавлении тега highway */
    override fun processDatasetEvent(event: AbstractDatasetChangedEvent?) {
        if (event !is TagsChangedEvent) return
        val primitive = event.primitive ?: return
        if (primitive.type != OsmPrimitiveType.WAY) return

        if (event.primitive.hasKey("highway")) applyStreetTagsAction.setEnabled(true)
        // здания добавляются чаще всего с помощью режима B, который не выделяет здания после добавления
//        if (event.primitive.hasKey("building")) applyBuildingTagsAction.setEnabled(true)
    }

}
