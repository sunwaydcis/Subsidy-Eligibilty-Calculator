package ui.screens

import scalafx.Includes.*
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scala.util.{Failure, Success}

import model.{Asset, Dependent, Household, HouseholdRepository}
import state.AppState
import ui.Styles

/** The Household Profile screen: enter a household's income, build up
 * its dependents and assets in two small tables, then save it.
 */

class HouseholdProfileScreen:

  // ---------------------------------------------------------------
  // Validation helpers — each returns Either[errorMessage, parsedValue]
  // ---------------------------------------------------------------

  // ai-assisted: #4
  // why: asked for inline validation returning Either instead of
  // popping an Alert or letting bad input crash the form
  private def requireNonBlank(text: String, fieldLabel: String): Either[String, String] =
    val trimmed = text.trim
    Either.cond(trimmed.nonEmpty, trimmed, s"$fieldLabel is required.")

  private def requireDescriptive(text: String, fieldLabel: String): Either[String, String] =
    val trimmed = text.trim
    if trimmed.isEmpty then Left(s"$fieldLabel is required.")
    else if trimmed.forall(_.isDigit) then Left(s"$fieldLabel should be a description, not a number.")
    else Right(trimmed)

  private def parseNonNegativeAmount(text: String, fieldLabel: String): Either[String, BigDecimal] =
    scala.util.Try(BigDecimal(text.trim)).toOption
      .toRight(s"$fieldLabel must be a valid number.")
      .flatMap(v => Either.cond(v >= 0, v, s"$fieldLabel cannot be negative."))

  private def parseAge(text: String): Either[String, Int] =
    text.trim.toIntOption
      .toRight("Age must be a whole number.")
      .flatMap(a => Either.cond(a >= 0 && a <= 120, a, "Age must be between 0 and 120."))

  // ---------------------------------------------------------------
  // Household-level fields
  // ---------------------------------------------------------------

  private val idField = new TextField:
    promptText = "e.g. HH-0001"

  private val idError = StringProperty("")

  private val idErrorLabel = errorLabel(idError)

  private val incomeField = new TextField:
    promptText = "e.g. 2500.00"
    // Last field in the household form — Enter here submits it, same
    // as clicking Save Household, so the user never has to reach for
    // the mouse to finish this form.
    onAction = handle { saveHousehold() }

  private val incomeError = StringProperty("")

  private val incomeErrorLabel = errorLabel(incomeError)

  // ---------------------------------------------------------------
  // Dependents: input row + table + add/remove
  // ---------------------------------------------------------------

  private val dependents = ObservableBuffer.empty[Dependent]

  private val dependentNameField = new TextField:
    promptText = "Name"
  private val dependentAgeField = new TextField:
    promptText = "Age"
  private val dependentRelationshipField = new TextField:
    promptText = "Relationship"
    // Last field in the add-dependent row — Enter here adds the row,
    // same as clicking Add Dependent.
    onAction = handle { addDependent() }

  private val dependentError = StringProperty("")

  private val dependentErrorLabel = errorLabel(dependentError)

  private val dependentTable = new TableView[Dependent](dependents):
    columns ++= Seq(
      new TableColumn[Dependent, String]:
        text = "Name"
        cellValueFactory = { data => StringProperty(data.value.name) }
        prefWidth = 150
      ,
      new TableColumn[Dependent, String]:
        text = "Age"
        cellValueFactory = { data => StringProperty(data.value.age.toString) }
        prefWidth = 60
      ,
      new TableColumn[Dependent, String]:
        text = "Relationship"
        cellValueFactory = { data => StringProperty(data.value.relationship) }
        prefWidth = 150
    )
    prefHeight = 160
    placeholder = new Label("No dependents added yet.")
    columnResizePolicy = TableView.ConstrainedResizePolicy
    // This table is a read-only summary of what's already been added,
    // not an input in the add-dependent row below it — leaving it in
    // the Tab order would stop between Income and Name for no reason
    // a keyboard user filling in the form would expect.
    focusTraversable = false

  /** Validates the add-dependent row and, if valid, appends the new
   * `Dependent` and clears the row. Shared by the button click and by
   * pressing Enter in the row's last field, so both paths validate
   * and clear identically.
   */
  private def addDependent(): Unit =
    val validated =
      for
        name <- requireNonBlank(dependentNameField.text.value, "Name")
        age  <- parseAge(dependentAgeField.text.value)
        relationship <- requireDescriptive(dependentRelationshipField.text.value, "Relationship")
      yield (name, age, relationship)

    validated match
      case Right((name, age, relationship)) =>
        dependentError.value = ""
        dependents += Dependent(name, age, relationship)
        dependentNameField.clear()
        dependentAgeField.clear()
        dependentRelationshipField.clear()
      case Left(message) =>
        dependentError.value = message

  private val addDependentButton = new Button("Add Dependent"):
    styleClass += "button-secondary"
    onAction = handle { addDependent() }

  private val removeDependentButton = removeSelectedButton(dependentTable, dependents)

  private val dependentsBox = new VBox(8,
    sectionHeading("Dependents"),
    dependentTable,
    new HBox(8,
      dependentNameField, dependentAgeField, dependentRelationshipField,
      addDependentButton, removeDependentButton
    ):
      alignment = Pos.CenterLeft
    ,
    dependentErrorLabel
  )

  // ---------------------------------------------------------------
  // Assets: input row + table + add/remove
  // ---------------------------------------------------------------

  private val assets = ObservableBuffer.empty[Asset]

  private val assetDescriptionField = new TextField:
    promptText = "Description"
  private val assetValueField = new TextField:
    promptText = "Estimated value"
    // Last field in the add-asset row — Enter here adds the row, same
    // as clicking Add Asset.
    onAction = handle { addAsset() }

  private val assetError = StringProperty("")

  private val assetErrorLabel = errorLabel(assetError)

  private val assetTable = new TableView[Asset](assets):
    columns ++= Seq(
      new TableColumn[Asset, String]:
        text = "Description"
        cellValueFactory = { data => StringProperty(data.value.description) }
        prefWidth = 240
      ,
      new TableColumn[Asset, String]:
        text = "Estimated Value"
        cellValueFactory = { data => StringProperty(data.value.estimatedValue.toString) }
        prefWidth = 120
    )
    prefHeight = 160
    placeholder = new Label("No assets added yet.")
    columnResizePolicy = TableView.ConstrainedResizePolicy
    // Read-only summary, not part of the add-asset row below it — see
    // the matching comment on `dependentTable`.
    focusTraversable = false

  /** Validates the add-asset row and, if valid, appends the new
   * `Asset` and clears the row. Shared by the button click and by
   * pressing Enter in the row's last field.
   */
  private def addAsset(): Unit =
    val validated =
      for
        description <- requireNonBlank(assetDescriptionField.text.value, "Description")
        value       <- parseNonNegativeAmount(assetValueField.text.value, "Estimated value")
      yield (description, value)

    validated match
      case Right((description, value)) =>
        assetError.value = ""
        assets += Asset(description, value)
        assetDescriptionField.clear()
        assetValueField.clear()
      case Left(message) =>
        assetError.value = message

  private val addAssetButton = new Button("Add Asset"):
    styleClass += "button-secondary"
    onAction = handle { addAsset() }

  private val removeAssetButton = removeSelectedButton(assetTable, assets)

  private val assetsBox = new VBox(8,
    sectionHeading("Assets"),
    assetTable,
    new HBox(8, assetDescriptionField, assetValueField, addAssetButton, removeAssetButton):
      alignment = Pos.CenterLeft
    ,
    assetErrorLabel
  )

  // ---------------------------------------------------------------
  // Save — validates, then delegates persistence to HouseholdRepository
  // ---------------------------------------------------------------

  private val statusLabel = new Label("")

  private val saveButton = new Button("Save Household"):
    styleClass += "button-primary"
    onAction = handle { saveHousehold() }

  private def saveHousehold(): Unit =
    val validated =
      for
        id     <- requireNonBlank(idField.text.value, "Household ID")
        income <- parseNonNegativeAmount(incomeField.text.value, "Monthly income")
      yield (id, income)

    validated match
      case Left(message) =>
        // Route the message to whichever field it's actually about,
        // and leave the other field's error alone.
        if message.startsWith("Household ID") then
          idError.value = message
        else
          idError.value = ""
          incomeError.value = message

      case Right((id, income)) =>
        idError.value = ""
        incomeError.value = ""
        val household = Household(id, income, dependents.toList, assets.toList)

        HouseholdRepository.save(household) match
          case Success(_) =>
            upsertIntoAppState(household)
            statusLabel.style = Styles.successText
            statusLabel.text =
              s"Saved '$id' — ${dependents.size} dependent(s), ${assets.size} asset(s)."
          case Failure(ex) =>
            statusLabel.style = Styles.errorText
            statusLabel.text = s"Could not save '$id' to disk: ${ex.getMessage}"

  /** Replaces the existing in-memory entry with the same id (so editing
   * and re-saving a household doesn't leave a stale duplicate in the
   * list other screens read from), or appends it if it's new.
   */
  private def upsertIntoAppState(household: Household): Unit =
    val existingIndex = AppState.households.indexWhere(_.id == household.id)
    if existingIndex >= 0 then AppState.households.update(existingIndex, household)
    else AppState.households += household

  private def sectionHeading(text0: String): Label =
    new Label(text0):
      style = Styles.sectionHeading

  private def errorLabel(error: StringProperty): Label =
    new Label:
      style = Styles.errorText
      text <== error

  private def removeSelectedButton[T](table: TableView[T], buffer: ObservableBuffer[T]): Button =
    new Button("Remove Selected"):
      styleClass += "button-danger"
      onAction = handle {
        Option(table.selectionModel.value.getSelectedItem).foreach(buffer -= _)
      }
  // ---------------------------------------------------------------
  // Layout
  // ---------------------------------------------------------------
  
  private def formRow(labelText: String, field: TextField, errorLabel: Label): VBox =
    new VBox(2,
      new HBox(10,
        new Label(labelText):
          prefWidth = 130
        ,
        field
      ):
        alignment = Pos.CenterLeft
      ,
      errorLabel
    )

  private val formFields = new VBox(8,
    formRow("Household ID:", idField, idErrorLabel),
    formRow("Monthly Income:", incomeField, incomeErrorLabel)
  )

  /** The screen's root node, ready to be dropped into the app's
   * content area.
   */
  val view: ScrollPane = new ScrollPane:
    fitToWidth = true
    content = new VBox(16,
      new Label("Household Profile"):
        style = Styles.screenTitle
      ,
      formFields,
      dependentsBox,
      assetsBox,
      new HBox(10, saveButton, statusLabel):
        alignment = Pos.CenterLeft
    ):
      padding = Insets(20)
      styleClass += "card"
