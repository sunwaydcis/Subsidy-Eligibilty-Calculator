package model

import java.time.LocalDateTime

final case class EligibilityCheckRecord(
                                         householdId: String,
                                         checkedAt: LocalDateTime,
                                         eligiblePrograms: List[String]
                                       )
