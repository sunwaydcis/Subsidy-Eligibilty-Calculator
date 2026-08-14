package ui.screens

import scalafx.Includes.*
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.layout.*
import java.time.LocalDateTime
import scala.util.{Failure, Success}

import engine.{EligibilityEngine, EligibilityResult}
import model.{EligibilityCheckRecord, EligibilityHistoryRepository}
import state.AppState
import ui.Styles
import ui.HouseholdPicker

/** Eligibility Check screen: pick a saved household, click Check, and
 * see it evaluated against all four subsidy programs.
 */

class EligibilityCheckScreen:

  private val householdCombo = HouseholdPicker.comboBox()
    // This screen's only field — committing a selection (Enter on a
    // highlighted item, same as a click) runs the check immediately,
    // matching the "Enter submits the form" behavior on the other
    // screens' text fields.
    //
    // Known side effect, considered and kept deliberately: since
    // runCheck() both displays results AND records a history entry,
    // simply browsing through households in the dropdown also logs
    // each one to History — not just an explicit "Check" click. I
    // weighed splitting this into "compute+display" vs. "compute+
    // display+record" as two separate paths, but kept the simpler
    // single path: every eligibility check performed, however it was
    // triggered, is a real check on real data and is fine to log.
      householdCombo.onAction = handle { if householdCombo.value.value != null then runCheck() }

  private val results = ObservableBuffer.empty[EligibilityResult]

  private def eligibilityCell(): TableCell[EligibilityResult, String] =
    new TableCell[EligibilityResult, String]:
      item.onChange { (_, _, value) =>
        text = value
        style =
          if value == "Eligible" then "-fx-text-fill: #2B6B46; -fx-font-weight: bold;"
          else if value == "Not Eligible" then "-fx-text-fill: #A3401C; -fx-font-weight: bold;"
          else ""
      }

  private val resultsTable = new TableView[EligibilityResult](results):
    columns ++= Seq(
      new TableColumn[EligibilityResult, String]:
        text = "Program"
        cellValueFactory = { data => StringProperty(data.value.programName) }
        prefWidth = 170
      ,
      new TableColumn[EligibilityResult, String]:
        text = "Eligible"
        cellValueFactory = { data =>
          StringProperty(if data.value.eligible then "Eligible" else "Not Eligible")
        }
        prefWidth = 100
        cellFactory = (_: TableColumn[EligibilityResult, String]) => eligibilityCell()
      ,
      new TableColumn[EligibilityResult, String]:
        text = "Reason"
        cellValueFactory = { data => StringProperty(data.value.reason) }
        prefWidth = 340
      ,
      new TableColumn[EligibilityResult, String]:
        text = "Required Documents"
        cellValueFactory = { data =>
          StringProperty(data.value.requiredDocuments.mkString("; "))
        }
        prefWidth = 340
    )
    prefHeight = 260
    placeholder = new Label("No results yet — pick a household and click Check.")
    columnResizePolicy = TableView.ConstrainedResizePolicy

  private val statusLabel = new Label("")

  private val checkButton = new Button("Check"):
    styleClass += "button-primary"
    // Reactive, not manually toggled: disabled exactly when there is no
    // household selected, recomputed automatically as the selection changes.
    disable <== householdCombo.value.isNull
    onAction = handle { runCheck() }

  private def runCheck(): Unit =
    val household = householdCombo.value.value
    val evaluated = EligibilityEngine.evaluateAll(household)
    results.clear()
    results ++= evaluated

    val eligibleCount = evaluated.count(_.eligible)
    val record = EligibilityCheckRecord(
      householdId = household.id,
      checkedAt = LocalDateTime.now(),
      eligiblePrograms = evaluated.filter(_.eligible).map(_.programName)
    )

    EligibilityHistoryRepository.recordCheck(record) match
      case Success(_) =>
        AppState.eligibilityHistory += record
        statusLabel.style = Styles.successText
        statusLabel.text =
          s"${household.id}: eligible for $eligibleCount of ${evaluated.size} program(s)."
      case Failure(ex) =>
        // The check itself already ran and is shown in the table above;
        // only recording it to history failed to persist.
        statusLabel.style = Styles.errorText
        statusLabel.text =
          s"${household.id}: eligible for $eligibleCount of ${evaluated.size} program(s), " +
            s"but this result could not be saved to history (${ex.getMessage})."

  private val noHouseholdsHint = HouseholdPicker.emptyHint()

  val view: VBox = new VBox(16,
    new Label("Eligibility Check"):
      style = Styles.screenTitle
    ,
    new HBox(10, householdCombo, checkButton):
      alignment = Pos.CenterLeft
    ,
    noHouseholdsHint,
    resultsTable,
    statusLabel
  ):
    padding = Insets(20)
    styleClass += "card"
