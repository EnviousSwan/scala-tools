package com.evolutiongaming.util

import com.evolutiongaming.util.Validation._
import org.scalactic.Equality
import org.scalatest.{FunSuite, Matchers}

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
    1.ok map { _ + 1 } shouldEqual 2.ok
  }

  test("foreach") {
    var x = 0
    1.ok foreach { x = _ }
    x shouldEqual 1
  }

  test("flatMap") {
    (for {x <- 1.ok; y <- 2.ok} yield x + y) shouldEqual 3.ok
    (for {x <- 1.ok; y <- 0.ko[Int]} yield x + y) shouldEqual 0.ko
    (for {x <- 0.ko[Int]; y <- 2.ok} yield x + y) shouldEqual 0.ko
  }

  test("toOption") {
    1.ok.toOption shouldEqual Some(1)
    0.ko.toOption shouldEqual None
  }

  test("toList") {
    1.ok.toList shouldEqual List(1)
    0.ko.toList shouldEqual Nil
  }

  test("toIterable") {
    1.ok.toIterable shouldEqual Iterable(1)
    0.ko.toIterable shouldEqual Nil
  }

  test("orElse") {
    1.ok[Int] orElse { _ => 2.ok } shouldEqual 1.ok
    0.ko orElse { _.ok } shouldEqual 0.ok
  }

  test("getOrElse") {
    1.ok[Int] getOrElse { _ => 2 } shouldEqual 1
    0.ko getOrElse { identity } shouldEqual 0
  }

  test("orFailure") {
    1.ok orFailure 2 shouldEqual 1
    0.ko orFailure 2 shouldEqual 2
  }

  test("onFailure") {
    var x = 0
    1.ok[Int] onFailure { x = _ }
    x shouldEqual 0
    1.ko onFailure { x = _ }
    x shouldEqual 1
  }

  test("toFailure") {
    1.ok.toFailure shouldEqual None
    0.ko.toFailure shouldEqual Some(0)
  }

  test("|") {
    1.ok[Int] | 2 shouldEqual 1
    0.ko | 1 shouldEqual 1
  }

  test("leftMap") {
    1.ok[Int] leftMap { _ + 1 } shouldEqual 1.ok
    0.ko leftMap { _ + 1 } shouldEqual 1.ko
  }

  test("orError") {
    1.ok.orError shouldEqual 1
    an[RuntimeException] should be thrownBy 0.ko.orError
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
    1.ok exists { _ == 1 } shouldEqual true
    1.ok exists { _ == 2 } shouldEqual false
    0.ko[Int] exists { _ == 1 } shouldEqual false
  }

  test("contains") {
    1.ok contains 1 shouldEqual true
    1.ok contains 2 shouldEqual false
    0.ko contains 1 shouldEqual false
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
  }

  test("?") {
    (null: String).? shouldEqual None
    "".? shouldEqual Some("")
  }

  test("allValid") {
    Nil allValid { _: Int => "ko".ko } shouldEqual ().ok
    Iterable(1) allValid { _ == 1 trueOr "ko" } shouldEqual ().ok
    Iterable(0) allValid { _ == 1 trueOr "ko" } shouldEqual "ko".ko
    Iterable(2, 4, 5, 6, 7) allValid { x =>
      x % 2 == 0 trueOr s"$x.ko"
    } shouldEqual "5.ko".ko
  }

  implicit def eitherEquality[L, R]: Equality[Either[L, R]] = new Equality[Either[L, R]] {
    def areEqual(a: Either[L, R], b: Any) = a == b
  }
}
