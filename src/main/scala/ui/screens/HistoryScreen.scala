package ui.screens

import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.VBox
import javafx.beans.binding.{Bindings as JBindings}
import java.time.format.DateTimeFormatter

import model.EligibilityCheckRecord
import state.AppState
import ui.Styles

/** History screen: every past eligibility check, most recent first,
 * with a search box above the table
 */

class HistoryScreen:

  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  private val visibleRecords = ObservableBuffer.empty[EligibilityCheckRecord]

  private def matches(record: EligibilityCheckRecord, query: String): Boolean =
    val normalizedQuery = query.trim.toLowerCase
    normalizedQuery.isEmpty ||
      record.householdId.toLowerCase.contains(normalizedQuery) ||
      record.eligiblePrograms.exists(_.toLowerCase.contains(normalizedQuery))

  private def refreshVisible(): Unit =
    val query = searchField.text.value
    val matching = AppState.eligibilityHistory
      .filter(matches(_, query))
      .sortWith((a, b) => a.checkedAt.isAfter(b.checkedAt)) // most recent first
    visibleRecords.clear()
    visibleRecords ++= matching

  private val searchField = new TextField:
    promptText = "Search by household ID or programme..."
    text.onChange { (_, _, _) => refreshVisible() }

  AppState.eligibilityHistory.onChange { (_, _) => refreshVisible() }

  private val recordsTable = new TableView[EligibilityCheckRecord](visibleRecords):
    columns ++= Seq(
      new TableColumn[EligibilityCheckRecord, String]:
        text = "Household ID"
        cellValueFactory = { data => StringProperty(data.value.householdId) }
        prefWidth = 140
      ,
      new TableColumn[EligibilityCheckRecord, String]:
        text = "Date"
        cellValueFactory = { data => StringProperty(data.value.checkedAt.format(dateFormatter)) }
        prefWidth = 150
      ,
      new TableColumn[EligibilityCheckRecord, String]:
        text = "Eligible For"
        cellValueFactory = { data =>
          val names = data.value.eligiblePrograms
          StringProperty(if names.isEmpty then "None" else names.mkString(", "))
        }
        prefWidth = 380
    )
    prefHeight = 320
    placeholder = new Label("No eligibility checks yet.")
    columnResizePolicy = TableView.ConstrainedResizePolicy

  private val emptyHint = new Label("Run a check from the Eligibility Check screen to see it here."):
    style = Styles.mutedText
    visible <== JBindings.isEmpty(AppState.eligibilityHistory)
    managed <== visible

  refreshVisible() // populate from whatever's already in AppState.eligibilityHistory

  val view: VBox = new VBox(16,
    new Label("History"):
      style = Styles.screenTitle
    ,
    searchField,
    emptyHint,
    recordsTable
  ):
    padding = Insets(20)
    styleClass += "card"
