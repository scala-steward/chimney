package io.scalaland.chimney.internal.compiletime.derivation.transformer

import io.scalaland.chimney.internal.compiletime.ChimneyDefinitionsPlatform
import io.scalaland.chimney.internal.compiletime.datatypes

abstract private[compiletime] class DerivationPlatform(q: scala.quoted.Quotes)
    extends ChimneyDefinitionsPlatform(q)
    with Derivation
    with datatypes.IterableOrArraysPlatform
    with datatypes.ProductTypesPlatform
    with datatypes.SealedHierarchiesPlatform
    with datatypes.ValueClassesPlatform
    with rules.TransformImplicitRuleModule
    with rules.TransformImplicitOuterTransformerRuleModule
    with rules.TransformSubtypesRuleModule
    with rules.TransformToSingletonRuleModule
    with rules.TransformOptionToOptionRuleModule
    with rules.TransformPartialOptionToNonOptionRuleModule
    with rules.TransformToOptionRuleModule
    with rules.TransformValueClassToValueClassRuleModule
    with rules.TransformValueClassToTypeRuleModule
    with rules.TransformTypeToValueClassRuleModule
    with rules.TransformEitherToEitherRuleModule
    with rules.TransformMapToMapRuleModule
    with rules.TransformIterableToIterableRuleModule
    with rules.TransformProductToProductRuleModule
    with rules.TransformSealedHierarchyToSealedHierarchyRuleModule {

  override protected val rulesAvailableForPlatform: List[Rule] = List(
    TransformImplicitRule,
    TransformImplicitOuterTransformerRule,
    TransformSubtypesRule,
    TransformToSingletonRule,
    TransformOptionToOptionRule,
    TransformPartialOptionToNonOptionRule,
    TransformToOptionRule,
    TransformValueClassToValueClassRule,
    TransformValueClassToTypeRule,
    TransformTypeToValueClassRule,
    TransformEitherToEitherRule,
    TransformMapToMapRule,
    TransformIterableToIterableRule,
    TransformProductToProductRule,
    TransformSealedHierarchyToSealedHierarchyRule
  )
}
