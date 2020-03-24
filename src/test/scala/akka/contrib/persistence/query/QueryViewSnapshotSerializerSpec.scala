package akka.contrib.persistence.query

import java.nio.ByteBuffer

import akka.persistence.query.Sequence
import akka.serialization.SerializationExtension
import com.ovoenergy.UnitSpec
import com.ovoenergy.akka.AkkaFixture
import org.scalacheck.{Arbitrary, Gen}

import scala.util.{Failure, Try}

class QueryViewSnapshotSerializerSpec extends UnitSpec with AkkaFixture {
  import Arbitrary._

  class NonSerializable(value: String)

  implicit val arbNonSerializable: Arbitrary[NonSerializable] = Arbitrary(for {
    value <- arbitrary[String]
  } yield new NonSerializable(value))

  implicit def arbQueryViewSnapshot[T](implicit arbT: Arbitrary[T]): Arbitrary[QueryViewSnapshot[T]] =
    Arbitrary(for {
      data <- arbT.arbitrary
      offset <- Gen.choose(1L, Long.MaxValue).map(Sequence)
      sequenceNrs <- Gen.mapOf(Gen.zip(Gen.alphaStr, Gen.posNum[Long]))
    } yield QueryViewSnapshot(data, offset, sequenceNrs))

  "QueryViewSnapshotSerializer" should {
    "Serialize and deserialize any QueryViewSnapshot" in forAll { testValue: QueryViewSnapshot[String] =>
      val serialization = SerializationExtension(extendedActorSystem)

      val (resolvedSerializer, result) = (for {
        serializer <- serialization.serializerOf(classOf[QueryViewSnapshotSerializer].getName)
        serialized <- serialization.serialize(testValue)
        deserialized <- serialization.deserialize(serialized, serializer.identifier, "")
      } yield serializer -> deserialized).get

      resolvedSerializer should be(a[QueryViewSnapshotSerializer])
      result should be(testValue)
    }

    "Serialize and deserialize any QueryViewSnapshot using ByteBuffer" in forAll {
      testValue: QueryViewSnapshot[String] =>
        val serialization = SerializationExtension(extendedActorSystem)

        val (resolvedSerializer, result) = (for {
          serializer <- serialization.serializerOf(classOf[QueryViewSnapshotSerializer].getName)
          serialized <- serialization.serialize(testValue)
          deserialized <- Try(
            serialization.deserializeByteBuffer(ByteBuffer.wrap(serialized), serializer.identifier, "")
          )
        } yield serializer -> deserialized).get

        resolvedSerializer should be(a[QueryViewSnapshotSerializer])
        result should be(testValue)
    }

    "fail if the data is not serializable" in forAll { testValue: QueryViewSnapshot[NonSerializable] =>
      val serialization = SerializationExtension(extendedActorSystem)

      (for {
        serializer <- serialization.serializerOf(classOf[QueryViewSnapshotSerializer].getName)
        serialized <- serialization.serialize(testValue)
        deserialized <- serialization.deserialize(serialized, serializer.identifier, "")
      } yield deserialized) shouldBe a[Failure[_]]

    }
  }
}
