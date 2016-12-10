package akka.persistence

import akka.persistence.QueryView.RecoveringStatus
import com.quantemplate.fabric.common.{ AkkaFixture, UnitSpec }
import org.scalacheck._
import org.scalacheck.Arbitrary._

class RecoveringStatusSerializerSpec extends UnitSpec with AkkaFixture {

  "RecoveringStatusSerializer" should {
    "Serialize and deserialize any RecoveringStatus" in forAll { testValue: RecoveringStatus[String] ⇒

      val serializer = new QueryViewStatusSerializer(extendedActorSystem)
      serializer.fromBinary(serializer.toBinary(testValue)) shouldBe testValue

    }
  }

  def recoveringStatusGen[T](tGen: Gen[T]): Gen[RecoveringStatus[T]] = for {
    t ← tGen
    maxOffset ← Gen.posNum[Long]
    sequenceNrs ← arbitrary[Map[String, Long]]
  } yield RecoveringStatus(t, maxOffset, sequenceNrs)

  implicit def arbRecoveringStatus[T](implicit tArb: Arbitrary[T]): Arbitrary[RecoveringStatus[T]] = Arbitrary(recoveringStatusGen(tArb.arbitrary))

}
