package state

import scalafx.collections.ObservableBuffer

import model.{EligibilityCheckRecord, Household}

object AppState:
  val households: ObservableBuffer[Household] = ObservableBuffer.empty[Household]
  val eligibilityHistory: ObservableBuffer[EligibilityCheckRecord] = ObservableBuffer.empty[EligibilityCheckRecord]
