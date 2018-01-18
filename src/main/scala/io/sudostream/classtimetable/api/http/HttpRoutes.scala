package io.sudostream.classtimetable.api.http

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.{Route, ValidationRejection}
import akka.stream.Materializer
import akka.util.Timeout
import io.sudostream.classtimetable.api.kafka.StreamingComponents
import io.sudostream.classtimetable.config.ActorSystemWrapper
import io.sudostream.classtimetable.dao.ClassTimetableWriterDao
import io.sudostream.timetoteach.kafka.serializing.systemwide.classes.ClassDetailsDeserializer
import io.sudostream.timetoteach.kafka.serializing.systemwide.classtimetable.ClassTimetableDeserializer
import io.sudostream.timetoteach.messages.systemwide.model.classes.ClassDetails
import io.sudostream.timetoteach.messages.systemwide.model.classtimetable.ClassTimetable

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class HttpRoutes(classTimetableDao: ClassTimetableWriterDao,
                 actorSystemWrapper: ActorSystemWrapper,
                 streamingComponents: StreamingComponents)
  extends Health {

  implicit val system: ActorSystem = actorSystemWrapper.system
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: Materializer = actorSystemWrapper.materializer
  val logger: LoggingAdapter = system.log

  implicit val timeout: Timeout = Timeout(30 seconds)

  val routes: Route =
    path("api" / "classtimetables" / Segment / "upsert") { (tttUserId) =>
      post {
        decodeRequest {
          entity(as[HttpEntity]) { entity =>
            val smallTimeout = 3000.millis
            val dataFuture = entity.toStrict(smallTimeout) map {
              httpEntity =>
                httpEntity.getData()
            }

            val classTimetableExtractedFuture: Future[ClassTimetable] = dataFuture map {
              databytes =>
                val bytesAsArray = databytes.toArray
                val classTimetableDeserializer = new ClassTimetableDeserializer
                classTimetableDeserializer.deserialize("ignore", bytesAsArray)
            }

            val insertClassTimetableEventualFuture = for {
              theClassTimetable <- classTimetableExtractedFuture
              upsertClassTimetableFuture = classTimetableDao.upsertClassTimetable(theClassTimetable)
            } yield (upsertClassTimetableFuture, theClassTimetable)

            val insertFutureCompleted = {
              insertClassTimetableEventualFuture map { tuple => tuple._1 }
            }.flatMap(fut => fut)

            onComplete(insertFutureCompleted) {
              case Success(insertCompleted) =>
                // TODO: To get here the classtimetable future must be completed and successful but my composing skills are lacking!
                val classTimetable = classTimetableExtractedFuture.value.get.get
                logger.info(s"Deserialised classtimetable: ${classTimetable.toString}")
                if (classTimetable.timeToTeachId.value == tttUserId) {
                  complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"timeToTeachId=${classTimetable.timeToTeachId.value}"))
                } else {
                  reject(ValidationRejection(s"Uri Param time to teach Id '$tttUserId' does not equal id in class timetable ${classTimetable.timeToTeachId.value}"))
                }
              case Failure(ex) => logger.error(s"Failed to deserialse classtimetable, ${ex.getMessage} : ${ex.getStackTrace.toString}")
                complete(StatusCodes.InternalServerError, ex.getMessage)
            }
          }
        }
      }
    } ~ path("api" / "classes" / Segment / "upsert") { (tttUserId) =>
      post {
        decodeRequest {
          entity(as[HttpEntity]) { entity =>

            ////////////////////////////////////////////////////////////////////////////////////////////////////
            val smallTimeout = 3000.millis
            val dataFuture = entity.toStrict(smallTimeout) map {
              httpEntity =>
                httpEntity.getData()
            }

            val newClassDetailsExtractedFuture: Future[ClassDetails] = dataFuture map {
              databytes =>
                val bytesAsArray = databytes.toArray
                val classDetailsDeserializer = new ClassDetailsDeserializer
                classDetailsDeserializer.deserialize("ignore", bytesAsArray)
            }

            val insertClassDetailsEventualFuture = for {
              theClassDetails <- newClassDetailsExtractedFuture
              upsertClassDetailsFuture = classTimetableDao.upsertClass(theClassDetails)
            } yield (upsertClassDetailsFuture, theClassDetails)

            val insertFutureCompleted = {
              insertClassDetailsEventualFuture map { tuple => tuple._1 }
            }.flatMap(fut => fut)

            onComplete(insertFutureCompleted) {
              case Success(insertCompleted) =>
                // TODO: To get here the class details future must be completed and successful but my composing skills are lacking!
                val classDetails = newClassDetailsExtractedFuture.value.get.get
                logger.info(s"Deserialised class details: ${classDetails.toString}")
                if (classDetails.teacherWithWriteAccess.contains(tttUserId)) {
                  complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"timeToTeachId=${tttUserId}"))
                } else {
                  reject(ValidationRejection(s"Uri Param time to teach Id '$tttUserId' is not contained id in class details ${classDetails.toString}"))
                }
              case Failure(ex) => logger.error(s"Failed to deserialse classtimetable, ${ex.getMessage} : ${ex.getStackTrace.toString}")
                complete(StatusCodes.InternalServerError, ex.getMessage)
            }
            ////////////////////////////////////////////////////////////////////////////////////////////////////
          }
        }
      }
    } ~ health

}