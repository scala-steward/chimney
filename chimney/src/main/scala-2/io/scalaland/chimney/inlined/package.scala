package io.scalaland.chimney

import io.scalaland.chimney.dsl.{
  PartialTransformerDefinition,
  PartialTransformerInto,
  PatcherDefinition,
  PatcherDefinitionCommons,
  PatcherUsing,
  TransformerDefinition,
  TransformerDefinitionCommons,
  TransformerInto
}
import io.scalaland.chimney.internal.runtime.{PatcherFlags, PatcherOverrides, TransformerFlags, TransformerOverrides}

/** Imports only extension methods for inlined derivation
  *
  * @since 0.8.0
  */
package object inlined {

  /** Provides transformer operations on values of any type.
    *
    * @tparam From
    *   type of source value
    * @param source
    *   wrapped source value
    *
    * @since 0.4.0
    */
  implicit class TransformationOps[From](private val source: From) extends AnyVal {

    /** Allows to customize transformer generation to your target type.
      *
      * @tparam To
      *   target type
      * @return
      *   [[io.scalaland.chimney.dsl.TransformerInto]]
      *
      * @since 0.1.0
      */
    final def into[To]: TransformerInto[From, To, TransformerOverrides.Empty, TransformerFlags.Default] =
      new TransformerInto(source, new TransformerDefinition(TransformerDefinitionCommons.emptyRuntimeDataStore))
  }

  /** Provides partial transformer operations on values of any type.
    *
    * @tparam From
    *   type of source value
    * @param source
    *   wrapped source value
    *
    * @since 0.7.0
    */
  implicit class PartialTransformationOps[From](private val source: From) extends AnyVal {

    /** Allows to customize partial transformer generation to your target type.
      *
      * @tparam To
      *   target success type
      * @return
      *   [[io.scalaland.chimney.dsl.PartialTransformerInto]]
      *
      * @since 0.7.0
      */
    final def intoPartial[To]: PartialTransformerInto[From, To, TransformerOverrides.Empty, TransformerFlags.Default] =
      new PartialTransformerInto(
        source,
        new PartialTransformerDefinition(TransformerDefinitionCommons.emptyRuntimeDataStore)
      )
  }

  /** Provides patcher operations on values of any type
    *
    * @param obj
    *   wrapped object to patch
    * @tparam A
    *   type of object to patch
    *
    * @since 0.1.3
    */
  implicit class PatchingOps[A](private val obj: A) extends AnyVal {

    /** Allows to customize patcher generation
      *
      * @tparam Patch
      *   type of patch object
      * @param patch
      *   patch object value
      * @return
      *   [[io.scalaland.chimney.dsl.PatcherUsing]]
      *
      * @since 0.4.0
      */
    final def using[Patch](patch: Patch): PatcherUsing[A, Patch, PatcherOverrides.Empty, PatcherFlags.Default] =
      new PatcherUsing[A, Patch, PatcherOverrides.Empty, PatcherFlags.Default](
        obj,
        patch,
        new PatcherDefinition(PatcherDefinitionCommons.emptyRuntimeDataStore)
      )
  }
}
