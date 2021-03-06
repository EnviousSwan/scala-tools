package com.evolutiongaming.util

import com.evolutiongaming.util.Validation._
import org.scalactic.Equality
import org.scalatest.{FunSuite, Matchers}

import scala.util.{Failure, Try}

class ValidationSpec extends FunSuite with Matchers {

  test("ok") {
    1.ok shouldEqual Right(1)
  }

  test("ko") {
    0.ko shouldEqual Left(0)
  }

  test("right") {
    1.right shouldEqual Right(1)
  }

  test("left") {
    0.left shouldEqual Left(0)
  }

  test("success") {
    1.success shouldEqual Right(1).asInstanceOf[Either[Nothing, Int]]
  }

  test("fail") {
    0.fail shouldEqual Left(0)
  }

  test("map") {
    EitherOps(1.ok) map { _ + 1 } shouldEqual 2.ok
  }

  test("foreach") {
    var x = 0
    EitherOps(1.ok) foreach { x = _ }
    x shouldEqual 1

    EitherOps(2.ko) foreach { x = _ }
    x shouldEqual 1
  }

  test("flatMap") {
    (for {x <- EitherOps(1.ok); y <- 2.ok} yield x + y) shouldEqual 3.ok
    (for {x <- EitherOps(1.ok); y <- 0.ko[Int]} yield x + y) shouldEqual 0.ko
    (for {x <- EitherOps(0.ko[Int]); y <- 2.ok} yield x + y) shouldEqual 0.ko
  }

  test("toRight") {
    0.ok.toRight shouldEqual Some(0)
    0.ko[Int].toRight shouldEqual None
  }

  test("toLeft") {
    0.ok.toLeft shouldEqual None
    0.ko[Int].toLeft shouldEqual Some(0)
  }

  test("toOption") {
    EitherOps(1.ok).toOption shouldEqual Some(1)
    EitherOps(0.ko[Int]).toOption shouldEqual None
  }

  test("toList") {
    1.ok.toList shouldEqual List(1)
    0.ko.toList shouldEqual Nil
  }

  test("toIterable") {
    1.ok.toIterable shouldEqual Iterable(1)
    0.ko.toIterable shouldEqual Nil
  }

  test("foldRight") {
    0.ok foldRight { _ => 1 } shouldEqual 0
    0.ko foldRight { _ => 1 } shouldEqual 1
  }

  test("foldLeft") {
    0.ok foldLeft { _ => 1 } shouldEqual 1
    0.ko foldLeft { _ => 1 } shouldEqual 0
  }

  test("orElse") {
    1.ok[Int] orElse 2.ok shouldEqual 1.ok
    0.ko orElse 0.ok shouldEqual 0.ok
  }

  test("getOrElse") {
    EitherOps(1.ok[Int]) getOrElse 2 shouldEqual 1
    EitherOps(0.ko) getOrElse 0 shouldEqual 0
  }

  test("onLeft") {
    var x = 0
    1.ok onLeft { x = _ }
    x shouldEqual 0
    1.ko onLeft { x = _ }
    x shouldEqual 1
  }

  test("onRight") {
    var x = 0
    1.ok[Int] onRight { x = _ }
    x shouldEqual 1
    0.ko onRight { x = _ }
    x shouldEqual 1
  }

  test("|") {
    1.ok[Int] | 2 shouldEqual 1
    0.ko | 1 shouldEqual 1
    Some("1") | "2" shouldEqual "1"
    None | "2" shouldEqual "2"
  }

  test("leftMap") {
    1.ok[Int] leftMap { _ + 1 } shouldEqual 1.ok
    0.ko leftMap { _ + 1 } shouldEqual 1.ko
  }

  test("orError") {
    1.ok.orError() shouldEqual 1
    intercept[LeftException] {
      0.ko.orError()
    }
  }

  test("isOk") {
    1.ok.isOk shouldEqual true
    0.ko.isOk shouldEqual false
  }

  test("isKo") {
    1.ok.isKo shouldEqual false
    0.ko.isKo shouldEqual true
  }

  test("exists") {
    EitherOps(1.ok) exists { _ == 1 } shouldEqual true
    EitherOps(1.ok) exists { _ == 2 } shouldEqual false
    EitherOps(0.ko) exists { _ == 1 } shouldEqual false
  }

  test("contains") {
    EitherOps(1.ok) contains 1 shouldEqual true
    EitherOps(1.ok) contains 2 shouldEqual false
    EitherOps(0.ko) contains 1 shouldEqual false
  }

  test("collect") {
    1.ok[String] collect { case x => x + 1 } shouldEqual 2.ok
    "".ko[Int] collect { case x => x + 1 } shouldEqual "".ko
  }

  test("flatten") {
    val either: Either[Int, Either[Int, String]] = "ok".ok.ok
    either.flatten shouldEqual "ok".ok
  }

  test("?>>") {
    1.ok[Int] ?>> 2 shouldEqual 1.ok
    0.ko ?>> 1 shouldEqual 1.ko
    None ?>> "ko" shouldEqual "ko".ko
    Some("ok") ?>> "ko" shouldEqual "ok".ok
    (null: String) ?>> "ko" shouldEqual "ko".ko

    Try(1) ?>> "1" shouldEqual 1.ok
    Failure(new RuntimeException) ?>> "1" shouldEqual "1".ko
  }

  test("?") {
    (null: String).? shouldEqual None
    "".? shouldEqual Some("")
  }

  test("trueOr") {
    true trueOr 1 shouldEqual ().ok
    false trueOr 1 shouldEqual 1.ko
  }

  test("falseOr") {
    true falseOr 1 shouldEqual 1.ko
    false falseOr 1 shouldEqual ().ok
  }

  test("allValid") {
    Nil allValid { _: Int => "ko".ko } shouldBe a[Right[_, _]]
    Set(1) allValid { _ == 1 trueOr "ko" } shouldEqual Vector(()).ok
    Iterable(1) allValid { _ == 1 trueOr "ko" } shouldEqual Vector(()).ok
    Iterable(0) allValid { _ == 1 trueOr "ko" } shouldEqual "ko".ko
    Iterable(0, 2, 3, 4, 5) allValid { x => x % 2 == 0 trueOr s"$x.ko" } shouldEqual "3.ko".ko
    List(0, 1, 2) allValid[Unit, Int, List[Int]] (_.ok) shouldEqual List(0, 1, 2).ok
    Set(0, 1) allValid[Unit, Int, Set[Int]] (_.ok) shouldEqual Set(1, 0).ok
    Map("one" -> 1, "two" -> 2) allValid[Unit, (Int, String), Map[Int, String]] (_.swap.ok) shouldEqual Map(1 -> "one", 2 -> "two").ok
  }

  test("recover") {
    1.ok[String] recover { case "2" => 2 } shouldEqual 1.ok
    "2".ko[Int] recover { case "2" => 2 } shouldEqual 2.ok
    "3".ko[Int] recover { case "2" => 2 } shouldEqual "3".ko
  }

  test("recoverWith") {
    1.ok[String] recoverWith { case "2" => 2.ok } shouldEqual 1.ok
    "2".ko[Int] recoverWith { case "2" => 2.ok } shouldEqual 2.ok
    "2".ko[Int] recoverWith { case "2" => "1".ko } shouldEqual "1".ko
    "3".ko[Int] recoverWith { case "2" => 2.ok } shouldEqual "3".ko
  }

  test("takeValid") {
    List[Either[String, Int]]("1".ko, "2".ko, 1.ok).takeValid shouldEqual Iterable(1).ok
    List[Either[String, Int]](1.ok).takeValid shouldEqual Iterable(1).ok
    List[Either[String, Int]]().takeValid shouldEqual Iterable().ok
    List[Either[String, Int]]("1".ko).takeValid shouldEqual "1".ko
  }

  test("separate") {
    List[Either[String, Int]]("1".ko, 1.ok, "2".ko, 2.ok).separate shouldEqual (List("1", "2") -> List(1, 2))
    List[Either[String, Int]]().separate shouldEqual (Nil -> Nil)
  }

  test("fallbackTo") {
    "1".ok.fallbackTo("2".ok) shouldEqual "1".ok
    "1".ok.fallbackTo("2".ko) shouldEqual "1".ok
    "1".ko.fallbackTo("2".ok) shouldEqual "2".ok
    "1".ko.fallbackTo("2".ko) shouldEqual "1".ko
  }

  test("forall") {
    EitherOps(1.ok) forall (_ == 1) shouldEqual true
    EitherOps(1.ok) forall (_ == 2) shouldEqual false
    EitherOps(1.ko) forall (_ == 1) shouldEqual true
  }

  test("asInstanceV") {
    (1: Any).asInstanceV[Int] shouldEqual 1.ok
    ("1": Any).asInstanceV[Int] shouldEqual "type mismatch, expected: int, actual: String".ko
  }

  test("leftMapToEither") {
    Try(1) leftMapToEither { _ => "1" } shouldEqual 1.ok
    Failure(new RuntimeException) leftMapToEither { _ => "1" } shouldEqual "1".ko
  }

  implicit def eitherEquality[L, R]: Equality[Either[L, R]] = new Equality[Either[L, R]] {
    def areEqual(a: Either[L, R], b: Any) = a == b
  }
}
