package model

final case class Household(
                            id: String,
                            monthlyIncome: BigDecimal,
                            dependents: List[Dependent],
                            assets: List[Asset]
                          )
