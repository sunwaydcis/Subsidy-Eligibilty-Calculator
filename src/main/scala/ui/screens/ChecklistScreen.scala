package ui.screens

import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.layout.{HBox, VBox}
import scalafx.stage.FileChooser

import scala.util.{Failure, Success}

import engine.{EligibilityEngine, SubsidyProgram}
import model.ChecklistExporter
import ui.Styles
import ui.HouseholdPicker

/** Checklist screen: pick a saved household, pick one of the
 * programs it's currently eligible for, and tick off that
 * program's required documents as they're gathered — then export
 * the checklist to a plain text file
 */

class ChecklistScreen:

  // ---------------------------------------------------------------
  // Household picker
  // ---------------------------------------------------------------
  
  private val householdCombo = HouseholdPicker.comboBox()

  private val noHouseholdsHint = HouseholdPicker.emptyHint()

  // ---------------------------------------------------------------
  // Program picker — only programs the selected household is
  // currently eligible for
  // ---------------------------------------------------------------

  private val eligiblePrograms = ObservableBuffer.empty[SubsidyProgram]

  private def programCell(): ListCell[SubsidyProgram] =
    new ListCell[SubsidyProgram]:
      item.onChange { (_, _, newProgram) =>
        text = if newProgram == null then null else newProgram.name
      }

  private val programCombo = new ComboBox[SubsidyProgram](eligiblePrograms):
    promptText = "Select a program"
    cellFactory = (_: ListView[SubsidyProgram]) => programCell()
    buttonCell = programCell()

  private val noEligibleProgramsHint =
    new Label("This household isn't currently eligible for any program."):
      style = Styles.mutedText
      visible = false
      managed <== visible

  private def refreshEligiblePrograms(): Unit =
    val selectedHousehold = Option(householdCombo.value.value)
    eligiblePrograms.clear()
    selectedHousehold.foreach { household =>
      eligiblePrograms ++= EligibilityEngine.allPrograms.filter(_.isEligible(household))
    }
    noEligibleProgramsHint.visible = selectedHousehold.isDefined && eligiblePrograms.isEmpty
    exportStatusLabel.text = ""
    programCombo.value = null

  householdCombo.value.onChange { (_, _, _) => refreshEligiblePrograms() }

  // ---------------------------------------------------------------
  // The checklist itself: one CheckBox per required document
  // ---------------------------------------------------------------

  private val currentChecklist = ObservableBuffer.empty[(String, CheckBox)]

  private val checklistBox = new VBox(8)

  private def clearChecklist(): Unit =
    currentChecklist.clear()
    checklistBox.children.clear()

  private def showChecklistFor(program: SubsidyProgram): Unit =
    val rows = program.requiredDocuments.map(document => (document, new CheckBox(document)))
    currentChecklist.clear()
    currentChecklist ++= rows
    checklistBox.children = rows.map(_._2)

  programCombo.value.onChange { (_, _, newProgram) =>
    exportStatusLabel.text = ""
    if newProgram == null then clearChecklist() else showChecklistFor(newProgram)
  }

  // ---------------------------------------------------------------
  // Print / Export
  // ---------------------------------------------------------------

  private def sanitizeForFileName(s: String): String =
    s.replaceAll("[^A-Za-z0-9_-]+", "-")

  private val exportStatusLabel = new Label("")

  private val exportButton = new Button("Print / Export"):
    styleClass += "button-primary"
    // Reactive, not manually toggled: there's nothing to export until
    // a program (and so a checklist) is selected.
    disable <== programCombo.value.isNull
    onAction = handle { exportChecklist() }

  private def exportChecklist(): Unit =
    val household = householdCombo.value.value
    val program = programCombo.value.value
    if household != null && program != null then
      val defaultFileName =
        s"${sanitizeForFileName(household.id)}-${sanitizeForFileName(program.name)}-checklist.txt"

      val chooser = new FileChooser:
        title = "Save Checklist"
        initialFileName = defaultFileName
        extensionFilters += new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt")
      
      val ownerWindow = exportButton.scene.value.window.value
      Option(chooser.showSaveDialog(ownerWindow)) match
        case Some(file) =>
          val documentsWithState =
            currentChecklist.toList.map { case (document, checkBox) => (document, checkBox.selected.value) }

          ChecklistExporter.exportFile(file, household.id, program.name, documentsWithState) match
            case Success(_) =>
              exportStatusLabel.style = Styles.successText
              exportStatusLabel.text = s"Saved checklist to ${file.getName}."
            case Failure(ex) =>
              exportStatusLabel.style = Styles.errorText
              exportStatusLabel.text = s"Could not save checklist: ${ex.getMessage}"
        case None =>
          () // user canceled the save dialog — nothing to report

  // ---------------------------------------------------------------
  // Layout
  // ---------------------------------------------------------------

  val view: VBox = new VBox(16,
    new Label("Checklist"):
      style = Styles.screenTitle
    ,
    new HBox(10, householdCombo, programCombo, exportButton):
      alignment = Pos.CenterLeft
    ,
    noHouseholdsHint,
    noEligibleProgramsHint,
    checklistBox,
    exportStatusLabel
  ):
    padding = Insets(20)
    styleClass += "card"
