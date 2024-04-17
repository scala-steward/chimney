package io.scalaland.chimney.integrations

import scala.collection.compat.*

trait TotallyBuildIterable[Collection, Item] {

  def totalFactory: Factory[Item, Collection]

  def iterator(collection: Collection): Iterator[Item]

  def to[Collection2](collection: Collection, factory: Factory[Item, Collection2]): Collection2 = {
    // Scala 2.12 expects Factory[Item, Coll[Item]] and we have to use silly tricks to put arbitrary Collection2 there
    def scala2_12workaround[Coll[_]](factoryButDifferentType: Factory[Item, Coll[Item]]): Coll[Item] =
      iterator(collection).to(factoryButDifferentType)
    type Coll2[A] = Collection2
    scala2_12workaround[Coll2](factory)
  }
}
