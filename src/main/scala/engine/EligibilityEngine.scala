package engine

import model.Household

object EligibilityEngine:
  
  // ai-assisted: #3
  // why: this generic method existed but wasn't actually being used by
  // the 4 program checks - asked Claude to fix that specifically
  def evaluate[T](input: T, rule: T => EligibilityResult): EligibilityResult = {
    // it calls whatever function it was given - apply this 'rule' to this 'input'
    rule(input)
  }
  
  val allPrograms: List[SubsidyProgram] =
    List(ChildSupportGrant, HousingSubsidy, FoodAssistance, ElderlyPension)
  
  def evaluateAll(household: Household): List[EligibilityResult] =
    allPrograms.map(_.evaluate(household))
