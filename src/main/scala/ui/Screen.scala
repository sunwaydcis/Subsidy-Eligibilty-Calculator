package ui

enum Screen(val label: String):
  case HouseholdProfile extends Screen("Household Profile")
  case EligibilityCheck extends Screen("Eligibility Check")
  case Checklist        extends Screen("Checklist")
  case History          extends Screen("History")