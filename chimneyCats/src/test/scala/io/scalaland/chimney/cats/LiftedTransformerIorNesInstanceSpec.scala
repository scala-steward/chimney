package io.scalaland.chimney.cats

import cats.data.{Ior, IorNes, NonEmptySet}
import io.scalaland.chimney.{Transformer, TransformerF}
import io.scalaland.chimney.cats.utils.ValidatedUtils.*
import io.scalaland.chimney.dsl.*
import io.scalaland.chimney.examples.*
import io.scalaland.chimney.examples.trip.*
import io.scalaland.chimney.utils.OptionUtils.*
import utest.*

import scala.collection.immutable.Queue
import scala.collection.mutable.ArrayBuffer

object LiftedTransformerIorNesInstanceSpec extends TestSuite {

  val tests: Tests = Tests {

    test("default transformation always becomes an Ior.Right") {

      Person("John", 10, 140).intoF[IorNes[String, +*], User].transform ==> Ior.right(User("John", 10, 140))
    }

    test("transformation becomes an Ior.Both if any component was converted to an Ior.Both") {

      Person("John", 10, 140)
        .intoF[IorNes[String, +*], User]
        .withFieldConstF(_.name, Ior.both(NonEmptySet.of("Name should not have dots in it"), "John.Doe"))
        .transform ==> Ior.both(NonEmptySet.of("Name should not have dots in it"), User("John.Doe", 10, 140))
    }

    test("transformation becomes an Ior.Left if any component was converted to an Ior.Left") {

      Person("", 10, 140)
        .intoF[IorNes[String, +*], User]
        .withFieldConstF(_.name, Ior.left(NonEmptySet.of("You must provide a name")))
        .transform ==> Ior.left(NonEmptySet.of("You must provide a name"))
    }

    test("transformation with field validated with .withFieldComputedF") {

      test("combines validation results") {

        val okForm = PersonForm("John", "10", "140")

        test("with 1 argument validation to Ior.Right should return Ior.Right") {

          okForm
            .into[Person]
            .withFieldConst(_.height, 200.5)
            .withFieldComputedF[IorNes[String, +*], Int, Int](
              _.age,
              _.age.parseInt.map(Ior.right).getOrElse(Ior.left(NonEmptySet.of("Invalid age entered")))
            )
            .transform ==> Ior.right(Person("John", 10, 200.5))
        }

        test("with 2 arguments validation to Ior.Both should accumulates errors in Ior.Both") {

          okForm
            .intoF[IorNes[String, +*], Person]
            .withFieldComputedF(_.age, _ => Ior.both(NonEmptySet.of("age warning"), 10))
            .withFieldComputedF(_.height, _ => Ior.both(NonEmptySet.of("height warning"), 100.0))
            .transform ==> Ior.both(NonEmptySet.of("age warning", "height warning"), Person("John", 10, 100.0))
        }

        test("with 3 argument validation to Ior.Left and Ior.Both should accumulate errors to the first Ior.Left") {

          okForm
            .intoF[IorNes[String, +*], Person]
            .withFieldComputedF(
              _.name,
              _ => Ior.both(NonEmptySet.of("Putting a dot in the name is deprecated"), "John.Doe")
            )
            .withFieldConstF(_.age, Ior.left(NonEmptySet.of("age is too low")))
            .withFieldConstF(_.height, Ior.both(NonEmptySet.of("height not available, using default"), 10.0))
            .transform ==> Ior.left(NonEmptySet.of("Putting a dot in the name is deprecated", "age is too low"))
        }

        test("failure with error handling") {
          val badForm = PersonForm("", "foo", "bar")

          badForm
            .into[Person]
            .withFieldComputedF[IorNes[String, +*], String, String](
              _.name,
              pf =>
                if (pf.name.isEmpty) Ior.left(NonEmptySet.one("empty name"))
                else Ior.right(pf.name.toUpperCase())
            )
            .withFieldComputedF(_.age, _.age.parseInt.toIorNes("bad age"))
            .withFieldComputedF(_.height, _.age.parseDouble.toIorNes("bad double"))
            .transform ==> Ior.left(NonEmptySet.of("empty name"))
        }
      }
    }

    test("recursive transform with nested validation") {

      implicit val personTransformerEithers: TransformerF[IorNes[String, +*], PersonForm, Person] =
        Transformer
          .defineF[IorNes[String, +*], PersonForm, Person]
          .withFieldComputedF(_.age, _.age.parseInt.toIorNes("bad age"))
          .withFieldComputedF(_.height, _.height.parseDouble.toIorNes("bad height"))
          .buildTransformer

      test("success") {

        val okTripForm = TripForm("100", List(PersonForm("John", "10", "140"), PersonForm("Caroline", "12", "155")))

        okTripForm
          .intoF[IorNes[String, +*], Trip]
          .withFieldComputedF(_.id, tf => tf.tripId.parseInt.toIorNes("bad id"))
          .transform ==> Ior.right(Trip(100, Vector(Person("John", 10, 140), Person("Caroline", 12, 155))))
      }

      test("failure with error handling") {

        val badTripForm = TripForm("100", List(PersonForm("John", "10", "foo"), PersonForm("Caroline", "bar", "155")))

        badTripForm
          .intoF[IorNes[String, +*], Trip]
          .withFieldComputedF(_.id, tf => tf.tripId.parseInt.toIorNes("bad id"))
          .transform ==> Ior.left(NonEmptySet.of("bad height"))
      }
    }

    test(".traverse should accumulate errors on the Left side") {

      TransformerFIorSupport[NonEmptySet[String]]
        .traverse(
          Iterator("bla", "ha", "hee", "bee"),
          (input: String) => Ior.both(NonEmptySet.of(s"Accumulating $input"), input)
        )
        .map(_.toList) ==>
        Ior.both(
          NonEmptySet.of("Accumulating bla", "Accumulating ha", "Accumulating hee", "Accumulating bee"),
          List("bla", "ha", "hee", "bee")
        )
    }

    test("wrapped subtype transformation") {

      class Foo(val x: Int)
      case class Bar(override val x: Int) extends Foo(x)

      Bar(100).intoF[IorNes[String, +*], Foo].transform.right.map(_.x) ==> Some(100)
    }

    test("wrapped value classes") {

      test("from value class") {
        addressbook.Email("abc@def.com").intoF[IorNes[String, +*], String].transform ==>
          Ior.right("abc@def.com")
      }

      test("to value class") {
        "abc@def.com".intoF[IorNes[String, +*], addressbook.Email].transform ==>
          Ior.right(addressbook.Email("abc@def.com"))
      }
    }

    test("wrapped options") {

      test("pure inner transformer") {

        implicit val intPrinter: Transformer[Int, String] = _.toString

        Option(123).intoF[IorNes[String, +*], Option[String]].transform ==> Ior.right(Some("123"))
        Option.empty[Int].intoF[IorNes[String, +*], Option[String]].transform ==> Ior.right(None)
      }

      test("wrapped inner transformer") {

        implicit val intParserValidated: TransformerF[IorNes[String, +*], String, Int] =
          _.parseInt.toIorNes("bad int")

        Option("123").intoF[IorNes[String, +*], Option[Int]].transform ==> Ior.right(Some(123))
        Option("abc").intoF[IorNes[String, +*], Option[Int]].transform ==> Ior.left(NonEmptySet.one("bad int"))
        Option.empty[String].intoF[IorNes[String, +*], Option[Int]].transform ==> Ior.right(None)
      }
    }

    test("wrapped T to Option[T]") {

      test("pure inner transformer") {

        implicit val intPrinter: Transformer[Int, String] = _.toString

        10.intoF[IorNes[String, +*], Option[String]].transform ==> Ior.right(Some("10"))
        (null: String).intoF[IorNes[String, +*], Option[String]].transform ==> Ior.right(None)
      }

      test("wrapped inner transformer") {

        implicit val intParserValidated: TransformerF[IorNes[String, +*], String, Int] =
          _.parseInt.toIorNes("bad int")

        "10".intoF[IorNes[String, +*], Option[Int]].transform ==> Ior.right(Some(10))
        (null: String).intoF[IorNes[String, +*], Option[Int]].transform ==> Ior.right(None)
        "x".intoF[IorNes[String, +*], Option[Int]].transform ==> Ior.left(NonEmptySet.one("bad int"))
      }
    }

    test("wrapped .enableUnsafeOption") {

      test("pure inner transformer") {
        implicit val intPrinter: Transformer[Int, String] = _.toString

        Option(10).intoF[IorNes[String, +*], String].enableUnsafeOption.transform ==> Ior.right("10")
        intercept[NoSuchElementException] {
          Option.empty[Int].intoF[IorNes[String, +*], String].enableUnsafeOption.transform
        }
      }

      test("wrapped inner transformer") {
        implicit val intParserValidated: TransformerF[IorNes[String, +*], String, Int] =
          _.parseInt.toIorNes("bad int")

        Option("10").intoF[IorNes[String, +*], Int].enableUnsafeOption.transform ==> Ior.right(10)
        Option("x").intoF[IorNes[String, +*], Int].enableUnsafeOption.transform ==>
          Ior.left(NonEmptySet.one("bad int"))
        intercept[NoSuchElementException] {
          Option.empty[String].intoF[IorNes[String, +*], Int].enableUnsafeOption.transform
        }
      }
    }

    test("wrapped iterables or arrays") {

      test("pure inner transformer") {

        implicit val intPrinter: Transformer[Int, String] = _.toString

        List(123, 456).intoF[IorNes[String, +*], List[String]].transform ==> Ior.right(List("123", "456"))
        Vector(123, 456).intoF[IorNes[String, +*], Queue[String]].transform ==> Ior.right(
          Queue("123", "456")
        )
        Array.empty[Int].intoF[IorNes[String, +*], Seq[String]].transform ==> Ior.right(Seq.empty[String])
      }

      test("wrapped inner transformer") {

        implicit val intParserValidated: TransformerF[IorNes[String, +*], String, Int] =
          _.parseInt.toIorNes("bad int")

        List("123", "456").intoF[IorNes[String, +*], List[Int]].transform ==> Ior.right(List(123, 456))
        Vector("123", "456").intoF[IorNes[String, +*], Queue[Int]].transform ==> Ior.right(Queue(123, 456))
        Array.empty[String].intoF[IorNes[String, +*], Seq[Int]].transform ==> Ior.right(Seq.empty[Int])
        Set("123", "456").intoF[IorNes[String, +*], Array[Int]].transform.right.get.sorted ==> Array(123, 456)

        List("abc", "456").intoF[IorNes[String, +*], List[Int]].transform ==> Ior.left(NonEmptySet.one("bad int"))
        Vector("123", "def").intoF[IorNes[String, +*], Queue[Int]].transform ==> Ior.left(NonEmptySet.one("bad int"))
        Array("abc", "def").intoF[IorNes[String, +*], Seq[Int]].transform ==> Ior.left(NonEmptySet.of("bad int"))
        Set("123", "xyz").intoF[IorNes[String, +*], Array[Int]].transform ==> Ior.left(NonEmptySet.one("bad int"))
      }
    }

    test("wrapped maps") {

      test("pure inner transformer") {

        implicit val intPrinter: Transformer[Int, String] = _.toString

        Map(1 -> 10, 2 -> 20).intoF[IorNes[String, +*], Map[String, String]].transform ==>
          Ior.right(Map("1" -> "10", "2" -> "20"))
        Map(1 -> 10, 2 -> 20).intoF[IorNes[String, +*], Map[String, Int]].transform ==>
          Ior.right(Map("1" -> 10, "2" -> 20))
        Seq(1 -> 10, 2 -> 20).intoF[IorNes[String, +*], Map[String, String]].transform ==>
          Ior.right(Map("1" -> "10", "2" -> "20"))
        ArrayBuffer(1 -> 10, 2 -> 20).intoF[IorNes[String, +*], Map[Int, String]].transform ==>
          Ior.right(Map(1 -> "10", 2 -> "20"))
        Map(1 -> 10, 2 -> 20).intoF[IorNes[String, +*], List[(String, String)]].transform ==>
          Ior.right(List("1" -> "10", "2" -> "20"))
        Map(1 -> 10, 2 -> 20).intoF[IorNes[String, +*], Vector[(String, Int)]].transform ==>
          Ior.right(Vector("1" -> 10, "2" -> 20))
        Array(1 -> 10, 2 -> 20).intoF[IorNes[String, +*], Map[String, String]].transform ==>
          Ior.right(Map("1" -> "10", "2" -> "20"))
        Array(1 -> 10, 2 -> 20).intoF[IorNes[String, +*], Map[Int, String]].transform ==>
          Ior.right(Map(1 -> "10", 2 -> "20"))
        Map(1 -> 10, 2 -> 20).intoF[IorNes[String, +*], Array[(String, String)]].transform.right.get ==>
          Array("1" -> "10", "2" -> "20")
        Map(1 -> 10, 2 -> 20).intoF[IorNes[String, +*], Array[(String, Int)]].transform.right.get ==>
          Array("1" -> 10, "2" -> 20)
      }

      test("wrapped inner transformer") {

        implicit val intParserValidated: TransformerF[IorNes[String, +*], String, Int] =
          _.parseInt.toIorNes("bad int")

        Map("1" -> "10", "2" -> "20").intoF[IorNes[String, +*], Map[Int, Int]].transform ==>
          Ior.right(Map(1 -> 10, 2 -> 20))
        Map("1" -> "10", "2" -> "20").intoF[IorNes[String, +*], Map[Int, String]].transform ==>
          Ior.right(Map(1 -> "10", 2 -> "20"))
        Seq("1" -> "10", "2" -> "20").intoF[IorNes[String, +*], Map[Int, Int]].transform ==>
          Ior.right(Map(1 -> 10, 2 -> 20))
        ArrayBuffer("1" -> "10", "2" -> "20").intoF[IorNes[String, +*], Map[String, Int]].transform ==>
          Ior.right(Map("1" -> 10, "2" -> 20))
        Map("1" -> "10", "2" -> "20").intoF[IorNes[String, +*], List[(Int, Int)]].transform ==>
          Ior.right(List(1 -> 10, 2 -> 20))
        Map("1" -> "10", "2" -> "20").intoF[IorNes[String, +*], Vector[(Int, String)]].transform ==>
          Ior.right(Vector(1 -> "10", 2 -> "20"))
        Array("1" -> "10", "2" -> "20").intoF[IorNes[String, +*], Map[Int, Int]].transform ==>
          Ior.right(Map(1 -> 10, 2 -> 20))
        Array("1" -> "10", "2" -> "20").intoF[IorNes[String, +*], Map[String, Int]].transform ==>
          Ior.right(Map("1" -> 10, "2" -> 20))
        Map("1" -> "10", "2" -> "20").intoF[IorNes[String, +*], Array[(Int, Int)]].transform.right.get ==>
          Array(1 -> 10, 2 -> 20)
        Map("1" -> "10", "2" -> "20").intoF[IorNes[String, +*], Array[(Int, String)]].transform.right.get ==>
          Array(1 -> "10", 2 -> "20")

        Map("1" -> "x", "y" -> "20").intoF[IorNes[String, +*], Map[Int, Int]].transform ==>
          Ior.left(NonEmptySet.of("bad int"))
        Map("x" -> "10", "2" -> "20").intoF[IorNes[String, +*], Map[Int, String]].transform ==>
          Ior.left(NonEmptySet.one("bad int"))
        Seq("1" -> "10", "2" -> "x").intoF[IorNes[String, +*], Map[Int, Int]].transform ==>
          Ior.left(NonEmptySet.one("bad int"))
        ArrayBuffer("1" -> "x", "2" -> "y").intoF[IorNes[String, +*], Map[String, Int]].transform ==>
          Ior.left(NonEmptySet.of("bad int"))
        Map("x" -> "10", "y" -> "z").intoF[IorNes[String, +*], ArrayBuffer[(Int, Int)]].transform ==>
          Ior.left(NonEmptySet.of("bad int"))
        Map("1" -> "10", "x" -> "20").intoF[IorNes[String, +*], Vector[(Int, String)]].transform ==>
          Ior.left(NonEmptySet.one("bad int"))
        Array("x" -> "y", "z" -> "v").intoF[IorNes[String, +*], Map[Int, Int]].transform ==>
          Ior.left(NonEmptySet.of("bad int"))
        Array("1" -> "x", "2" -> "y").intoF[IorNes[String, +*], Map[String, Int]].transform ==>
          Ior.left(NonEmptySet.of("bad int"))
        Map("1" -> "10", "x" -> "20").intoF[IorNes[String, +*], Array[(Int, Int)]].transform ==>
          Ior.left(NonEmptySet.one("bad int"))
        Map("x" -> "10", "y" -> "20").intoF[IorNes[String, +*], Array[(Int, String)]].transform ==>
          Ior.left(NonEmptySet.of("bad int"))
      }
    }

    test("wrapped eithers") {

      test("pure inner transformer") {

        implicit val intPrinter: Transformer[Int, String] = _.toString

        (Left(1): Either[Int, Int]).intoF[IorNes[String, +*], Either[String, String]].transform ==>
          Ior.right(Left("1"))
        (Right(1): Either[Int, Int]).intoF[IorNes[String, +*], Either[String, String]].transform ==>
          Ior.right(Right("1"))
        Left(1).intoF[IorNes[String, +*], Either[String, String]].transform ==> Ior.right(Left("1"))
        Right(1).intoF[IorNes[String, +*], Either[String, String]].transform ==> Ior.right(Right("1"))
        Left(1).intoF[IorNes[String, +*], Left[String, String]].transform ==> Ior.right(Left("1"))
        Right(1).intoF[IorNes[String, +*], Right[String, String]].transform ==> Ior.right(Right("1"))
      }

      test("wrapped inner transformer") {

        implicit val intParserValidated: TransformerF[IorNes[String, +*], String, Int] =
          _.parseInt.toIorNes("bad int")

        (Left("1"): Either[String, String]).intoF[IorNes[String, +*], Either[Int, Int]].transform ==>
          Ior.right(Left(1))
        (Right("1"): Either[String, String]).intoF[IorNes[String, +*], Either[Int, Int]].transform ==>
          Ior.right(Right(1))
        Left("1").intoF[IorNes[String, +*], Either[Int, Int]].transform ==> Ior.right(Left(1))
        Right("1").intoF[IorNes[String, +*], Either[Int, Int]].transform ==> Ior.right(Right(1))
        Left("1").intoF[IorNes[String, +*], Either[Int, Int]].transform ==> Ior.right(Left(1))
        Right("1").intoF[IorNes[String, +*], Either[Int, Int]].transform ==> Ior.right(Right(1))

        (Left("x"): Either[String, String]).intoF[IorNes[String, +*], Either[Int, Int]].transform ==>
          Ior.left(NonEmptySet.one("bad int"))
        (Right("x"): Either[String, String]).intoF[IorNes[String, +*], Either[Int, Int]].transform ==>
          Ior.left(NonEmptySet.one("bad int"))
        Left("x").intoF[IorNes[String, +*], Either[Int, Int]].transform ==> Ior.left(NonEmptySet.one("bad int"))
        Right("x").intoF[IorNes[String, +*], Either[Int, Int]].transform ==> Ior.left(NonEmptySet.one("bad int"))
        Left("x").intoF[IorNes[String, +*], Either[Int, Int]].transform ==> Ior.left(NonEmptySet.one("bad int"))
        Right("x").intoF[IorNes[String, +*], Either[Int, Int]].transform ==> Ior.left(NonEmptySet.one("bad int"))
      }

      test("mixed inner transformer") {

        implicit val intPrinter: Transformer[Int, String] = _.toString
        implicit val intParserValidated: TransformerF[IorNes[String, +*], String, Int] =
          _.parseInt.toIorNes("bad int")

        (Left("1"): Either[String, Int]).intoF[IorNes[String, +*], Either[Int, String]].transform ==>
          Ior.right(Left(1))
        (Left("x"): Either[String, Int]).intoF[IorNes[String, +*], Either[Int, String]].transform ==>
          Ior.left(NonEmptySet.one("bad int"))
        (Right(100): Either[String, Int]).intoF[IorNes[String, +*], Either[Int, String]].transform ==>
          Ior.right(Right("100"))

        (Left(100): Either[Int, String]).intoF[IorNes[String, +*], Either[String, Int]].transform ==>
          Ior.right(Left("100"))
        (Right("1"): Either[Int, String]).intoF[IorNes[String, +*], Either[String, Int]].transform ==>
          Ior.right(Right(1))
        (Right("x"): Either[Int, String]).intoF[IorNes[String, +*], Either[String, Int]].transform ==>
          Ior.left(NonEmptySet.one("bad int"))
      }
    }

    test("wrapped sealed families") {
      import numbers.*

      test("pure inner transformer") {

        implicit val intPrinter: Transformer[Int, String] = _.toString

        import ScalesTransformerF.shortToLongPureInner

        (short.Zero: short.NumScale[Int, Nothing])
          .intoF[IorNes[String, +*], long.NumScale[String]]
          .transform ==> Ior.right(long.Zero)
        (short.Million(4): short.NumScale[Int, Nothing])
          .intoF[IorNes[String, +*], long.NumScale[String]]
          .transform ==> Ior.right(long.Million("4"))
        (short.Billion(2): short.NumScale[Int, Nothing])
          .intoF[IorNes[String, +*], long.NumScale[String]]
          .transform ==> Ior.right(long.Milliard("2"))
        (short.Trillion(100): short.NumScale[Int, Nothing])
          .intoF[IorNes[String, +*], long.NumScale[String]]
          .transform ==> Ior.right(long.Billion("100"))
      }

      test("wrapped inner transformer") {

        implicit val intParserValidated: TransformerF[IorNes[String, +*], String, Int] =
          _.parseInt.toIorNes("bad int")

        import ScalesTransformerF.shortToLongWrappedInner

        (short.Zero: short.NumScale[String, Nothing])
          .intoF[IorNes[String, +*], long.NumScale[Int]]
          .transform ==> Ior.right(long.Zero)
        (short.Million("4"): short.NumScale[String, Nothing])
          .intoF[IorNes[String, +*], long.NumScale[Int]]
          .transform ==> Ior.right(long.Million(4))
        (short.Billion("2"): short.NumScale[String, Nothing])
          .intoF[IorNes[String, +*], long.NumScale[Int]]
          .transform ==> Ior.right(long.Milliard(2))
        (short.Trillion("100"): short.NumScale[String, Nothing])
          .intoF[IorNes[String, +*], long.NumScale[Int]]
          .transform ==> Ior.right(long.Billion(100))

        (short.Million("x"): short.NumScale[String, Nothing])
          .intoF[IorNes[String, +*], long.NumScale[Int]]
          .transform ==> Ior.left(NonEmptySet.one("bad int"))
        (short.Billion("x"): short.NumScale[String, Nothing])
          .intoF[IorNes[String, +*], long.NumScale[Int]]
          .transform ==> Ior.left(NonEmptySet.one("bad int"))
        (short.Trillion("x"): short.NumScale[String, Nothing])
          .intoF[IorNes[String, +*], long.NumScale[Int]]
          .transform ==> Ior.left(NonEmptySet.one("bad int"))
      }
    }
  }
}