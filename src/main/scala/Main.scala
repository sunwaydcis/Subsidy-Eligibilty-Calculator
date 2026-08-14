import scalafx.Includes.*
import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.{Alert, Button}
import scalafx.scene.layout.{BorderPane, StackPane, VBox}
import scala.util.{Failure, Success, Try}

import model.{EligibilityHistoryRepository, HouseholdRepository}
import state.AppState
import ui.Screen
import ui.screens.{HouseholdProfileScreen, EligibilityCheckScreen, ChecklistScreen, HistoryScreen}

/** App entry point / shell.
 * Builds a `BorderPane` with a left-hand nav menu (one button per
 * `Screen`) and a center content area whose children get swapped out
 * as the user navigates. Builds `Household Profile`, `Eligibility Check`,
 * `History`, and `Checklist` screens.
 */

object Main extends JFXApp3:

  override def start(): Unit =

    loadIntoBuffer(HouseholdRepository.loadAll, AppState.households, "households")
    loadIntoBuffer(EligibilityHistoryRepository.loadAll, AppState.eligibilityHistory, "eligibility check history")

    val householdProfileScreen = new HouseholdProfileScreen
    val eligibilityCheckScreen = new EligibilityCheckScreen
    val checklistScreen = new ChecklistScreen
    val historyScreen = new HistoryScreen

    val contentArea = new StackPane:
      padding = Insets(10)

    // One nav button per `Screen`, built before `showScreen` so the
    // active-highlight logic below has every button to look up by
    // screen and toggle the "nav-button-active" style class on.
    val navButtons: Seq[Button] = Screen.values.toIndexedSeq.map { screen =>
      new Button(screen.label):
        maxWidth = Double.MaxValue
        styleClass += "nav-button"
    }

    val buttonForScreen: Map[Screen, Button] = Screen.values.toIndexedSeq.zip(navButtons).toMap

    /** Swaps the visible screen content and moves the "active" style
     * class to the corresponding nav button, so the left-nav always
     * reflects exactly one highlighted (current) screen.
     */
    def showScreen(screen: Screen): Unit =
      val content = screen match
        case Screen.HouseholdProfile => householdProfileScreen.view
        case Screen.EligibilityCheck => eligibilityCheckScreen.view
        case Screen.Checklist        => checklistScreen.view
        case Screen.History          => historyScreen.view

      contentArea.children = Seq(content)

      buttonForScreen.values.foreach(_.styleClass -= "nav-button-active")
      buttonForScreen(screen).styleClass += "nav-button-active"

    navButtons.zip(Screen.values.toIndexedSeq).foreach { case (button, screen) =>
      button.onAction = handle { showScreen(screen) }
    }

    val navMenu = new VBox(8, navButtons*):
      padding = Insets(10)
      prefWidth = 190
      styleClass += "nav-menu"

    val root = new BorderPane:
      left = navMenu
      center = contentArea

    // Show the initial screen through the same path every later
    // navigation click takes, so the Household Profile nav button
    // starts out highlighted rather than the nav bar looking
    // unselected until the user first clicks something.
    showScreen(Screen.HouseholdProfile)

    stage = new PrimaryStage:
      title = "Subsidy Eligibility Calculator"
      width = 920
      height = 640
      scene = new Scene(root):
        stylesheets += getClass.getResource("/styles/design.css").toExternalForm

  /** Populates one `AppState` buffer from disk before any screen is
   * built, so screens that bind directly to that buffer see
   * previously saved data right away.
   *
   * Both `HouseholdRepository.loadAll` and
   * `EligibilityHistoryRepository.loadAll` need the exact same
   * "load, or tell the user and carry on with an empty list" handling
   * at startup — this one generic method is that handling written
   * once instead of copy-pasted per repository.
   *
   * This is a one-off startup concern rather than a form validation
   * error, so — unlike the Household Profile form's inline field
   * errors — a genuine load failure here is surfaced as a dialog
   * rather than a label, since there's no specific field to attach it to.
   */
  private def loadIntoBuffer[T](
                                 load: () => Try[List[T]],
                                 buffer: ObservableBuffer[T],
                                 subjectDescription: String
                               ): Unit =
    load() match
      case Success(items) =>
        buffer ++= items
      case Failure(ex) =>
        new Alert(Alert.AlertType.Warning):
          title = s"Could not load saved $subjectDescription"
          headerText = None
          contentText = s"Starting with an empty list. (${ex.getMessage})"
        .showAndWait()