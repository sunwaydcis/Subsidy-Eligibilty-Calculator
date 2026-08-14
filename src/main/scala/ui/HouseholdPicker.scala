package ui

import scalafx.scene.control.{ComboBox, Label, ListCell, ListView}
import javafx.beans.binding.{Bindings as JBindings}

import model.Household
import state.AppState

/** Shared ("pick a saved household") household-dropdown builder 
 * used by 2 screens:`ChecklistScreen` and `EligibilityCheckScreen`
 */

object HouseholdPicker:

  private def format(h: Household): String =
    s"${h.id} - income ${h.monthlyIncome}, ${h.dependents.size} dependent(s)"
  
  private def cell(): ListCell[Household] =
    new ListCell[Household]:
      item.onChange { (_, _, newHousehold) =>
        text = if newHousehold == null then null else format(newHousehold)
      }
  
  def comboBox(): ComboBox[Household] = new ComboBox[Household](AppState.households):
    promptText = "Select a saved household"
    cellFactory = (_: ListView[Household]) => cell()
    buttonCell = cell()
  
  def emptyHint(): Label = new Label("Save a household on the Household Profile screen first."):
    style = Styles.mutedText
    visible <== JBindings.isEmpty(AppState.households)
    managed <== visible
