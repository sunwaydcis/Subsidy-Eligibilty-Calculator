package engine

import model.Household

final case class EligibilityResult(
                                    programName: String,
                                    eligible: Boolean,
                                    reason: String,
                                    requiredDocuments: List[String]
                                  )

sealed trait SubsidyProgram:
  def name: String
  def isEligible(h: Household): Boolean
  def requiredDocuments: List[String]
  def evaluate(h: Household): EligibilityResult

abstract class Program(val name: String, val incomeThreshold: BigDecimal)
  extends SubsidyProgram:

  // ai-assisted: #2
  // why: asked Claude to factor the income-threshold check out of all
  // four programs into one shared method, instead of repeating it
  private final def meetsIncomeThreshold(h: Household): Boolean =
    h.monthlyIncome <= incomeThreshold

  protected def meetsAdditionalCriteria(h: Household): Boolean

  protected def additionalCriteriaDescription: String

  protected val proofOfIncome: String = "Proof of income"
  protected val proofOfResidence: String = "Proof of residence"

  final def isEligible(h: Household): Boolean =
    meetsIncomeThreshold(h) && meetsAdditionalCriteria(h)

  final def evaluate(h: Household): EligibilityResult =
    EligibilityEngine.evaluate(h, buildResult)

  private def buildResult(h: Household): EligibilityResult =
    val incomeOk = meetsIncomeThreshold(h)
    val extraOk = meetsAdditionalCriteria(h)
    val reason =
      if incomeOk && extraOk then
        s"Income of ${h.monthlyIncome} is within the $incomeThreshold threshold, and $additionalCriteriaDescription."
      else if !incomeOk then
        s"Monthly income of ${h.monthlyIncome} exceeds the $incomeThreshold threshold for $name."
      else
        s"Income is within threshold, but the household does not meet: $additionalCriteriaDescription."
    EligibilityResult(name, incomeOk && extraOk, reason, requiredDocuments)

// ---------------------------------------------------------------------
// The four programmes
// ---------------------------------------------------------------------

case object ChildSupportGrant extends Program(name = "Child Support Grant", incomeThreshold = 3500):

  protected def meetsAdditionalCriteria(h: Household): Boolean =
    h.dependents.exists(_.age < 18)

  protected def additionalCriteriaDescription: String =
    "the household has at least one dependent under 18"

  def requiredDocuments: List[String] = List(
    proofOfIncome,
    // child grants require more granular income evidence than other programs
    "Payslips or bank statements for the last 3 months",
    "Birth certificate(s) for each qualifying child",
    proofOfResidence
  )

case object HousingSubsidy extends Program(name = "Housing Subsidy", incomeThreshold = 4000):

  private val assetCap: BigDecimal = 50000

  private def totalAssetValue(h: Household): BigDecimal =
    h.assets.map(_.estimatedValue).sum

  protected def meetsAdditionalCriteria(h: Household): Boolean =
    totalAssetValue(h) <= assetCap

  protected def additionalCriteriaDescription: String =
    s"total declared assets are at or below $assetCap"

  def requiredDocuments: List[String] = List(
    proofOfIncome,
    "Lease agreement or proof of housing costs",
    "Asset declaration"
  )

case object FoodAssistance extends Program(name = "Food Assistance", incomeThreshold = 3000):

  protected def meetsAdditionalCriteria(h: Household): Boolean = true

  protected def additionalCriteriaDescription: String =
    "there is no additional requirement beyond the income threshold"

  def requiredDocuments: List[String] = List(
    proofOfIncome,
    "Household composition declaration",
    proofOfResidence
  )

case object ElderlyPension extends Program(name = "Elderly Pension", incomeThreshold = 3200):

  private val elderlyAge = 60

  protected def meetsAdditionalCriteria(h: Household): Boolean =
    h.dependents.exists(_.age >= elderlyAge)

  protected def additionalCriteriaDescription: String =
    s"the household has at least one dependent aged $elderlyAge or older"

  def requiredDocuments: List[String] = List(
    proofOfIncome,
    "ID document for the elderly dependent",
    proofOfResidence
  )
