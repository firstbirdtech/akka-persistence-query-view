/*
 * Copyright 2016 OVO Energy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package akka.contrib.persistence.query

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer

import akka.actor.ExtendedActorSystem
import akka.contrib.persistence.query.QueryViewFormats.Payload
import akka.persistence.query.Offset
import akka.protobuf.ByteString
import akka.serialization.{BaseSerializer, ByteBufferSerializer, SerializationExtension, SerializerWithStringManifest}

import scala.jdk.CollectionConverters._

class QueryViewSnapshotSerializer(val system: ExtendedActorSystem) extends BaseSerializer with ByteBufferSerializer {

  override def includeManifest: Boolean = false

  // It MUST be lazy otherwise will deadlock during initialization.
  private lazy val serialization = SerializationExtension(system)

  override def toBinary(o: AnyRef): Array[Byte] = {
    val out = new ByteArrayOutputStream() // TODO sizeHint
    toBinary(o, out)
    out.toByteArray
  }

  override def toBinary(o: AnyRef, buf: ByteBuffer): Unit =
    toBinary(o, new ByteBufferOutputStream(buf))

  private def toBinary(o: AnyRef, out: OutputStream): Unit =
    o match {
      case qvs: QueryViewSnapshot[_] => serializeQueryViewSnapshot(qvs, out)
      case _                         => throw new IllegalArgumentException(s"Can't serialize object of type ${o.getClass}")
    }

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef =
    fromBinary(new ByteArrayInputStream(bytes))

  override def fromBinary(buf: ByteBuffer, manifest: String): AnyRef =
    fromBinary(new ByteBufferInputStream(buf))

  private def fromBinary(in: InputStream): AnyRef =
    deserializeQueryViewSnapshot(in)

  private def serializeQueryViewSnapshot(snapshot: QueryViewSnapshot[_], out: OutputStream): Unit = {

    val builder = QueryViewFormats.QueryViewSnapshot.newBuilder()
    snapshot.sequenceNrs.foreach {
      case (persistenceId, sequenceNr) =>
        builder.addSequenceNrs(
          QueryViewFormats.QueryViewSnapshot.SequenceNrEntry
            .newBuilder()
            .setPersistenceId(persistenceId)
            .setSequenceNr(sequenceNr)
            .build()
        )
    }

    builder.setMaxOffset(serializePayload(snapshot.maxOffset))
    builder.setData(serializePayload(snapshot.data.asInstanceOf[AnyRef]))
    val built = builder.build()

    built.writeTo(out)
  }

  private def deserializeQueryViewSnapshot(in: InputStream): QueryViewSnapshot[AnyRef] = {
    val parsed = QueryViewFormats.QueryViewSnapshot.parseFrom(in)

    val sequenceNrsBuilder = Map.newBuilder[String, Long]
    parsed.getSequenceNrsList.asScala.foreach { entry =>
      sequenceNrsBuilder += entry.getPersistenceId -> entry.getSequenceNr
    }

    val data      = deserializePayload(parsed.getData)
    val maxOffset = deserializePayload(parsed.getMaxOffset).asInstanceOf[Offset]

    QueryViewSnapshot(data, maxOffset, sequenceNrsBuilder.result())
  }

  private def serializePayload(payload: AnyRef): Payload = {
    val builder    = Payload.newBuilder()
    val serializer = serialization.serializerFor(payload.getClass)
    builder.setSerializerId(serializer.identifier)
    serializer match {
      case s: SerializerWithStringManifest =>
        builder.setMessageManifest(s.manifest(payload))
      case s if s.includeManifest =>
        builder.setMessageManifest(payload.getClass.getName)
      case _ =>
        builder.setMessageManifest("")
    }
    builder.setEnclosedMessage(ByteString.copyFrom(serializer.toBinary(payload)))
    builder.build()
  }

  private def deserializePayload(payload: Payload): AnyRef = {

    val data         = payload.getEnclosedMessage.toByteArray
    val serializerId = payload.getSerializerId
    val manifest     = payload.getMessageManifest

    serialization.deserialize(data, serializerId, manifest).get
  }

}
