package io.scalaland.chimney

import io.scalaland.chimney.internal.compiletime.derivation.patcher.PatcherMacros

import scala.language.experimental.macros

private[chimney] trait PatcherCompanionPlatform { this: Patcher.type =>

  /** Provides [[io.scalaland.chimney.Patcher]] instance for arbitrary types.
    *
    * @tparam A
    *   type of object to apply patch to
    * @tparam Patch
    *   type of patch object
    * @return
    *   [[io.scalaland.chimney.Patcher]] type class instance
    *
    * @since 0.2.0
    */
  def derive[A, Patch]: Patcher[A, Patch] =
    macro PatcherMacros.derivePatcherWithDefaults[A, Patch]
}

private[chimney] trait PatcherAutoDerivedCompanionPlatform { this: Patcher.AutoDerived.type =>

  /** Provides [[io.scalaland.chimney.Patcher]] derived with the default settings.
    *
    * This instance WILL NOT be visible for recursive derivation (automatic, semiautomatic, inlined), which is how it
    * differs from [[io.scalaland.chimney.auto#deriveAutomaticPatcher]].
    *
    * @tparam A
    *   type of object to apply patch to
    * @tparam Patch
    *   type of patch object
    * @return
    *   [[io.scalaland.chimney.Patcher]] type class instance
    *
    * @since 0.8.0
    */
  implicit def deriveAutomatic[A, Patch]: Patcher.AutoDerived[A, Patch] =
    macro PatcherMacros.derivePatcherWithDefaults[A, Patch]
}
