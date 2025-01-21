package io.scalaland.chimney.internal.runtime

import io.scalaland.chimney.internal.runtime.PatcherOverrides as Overrides

sealed abstract class PatcherOverrides
object PatcherOverrides {
  final class Empty extends Overrides
  // Allow ignoring fields from patch
  final class Ignored[PatchPath <: Path, Tail <: Overrides] extends Overrides
  // Computes a value from an expr
  final class ComputedFrom[PatchPath <: Path, ObjPath <: Path, Tail <: Overrides] extends Overrides
}
