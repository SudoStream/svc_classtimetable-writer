package io.sudostream.classtimetable.dao

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream.Materializer
import io.sudostream.classtimetable.config.ActorSystemWrapper
import io.sudostream.timetoteach.messages.systemwide.model.classes.ClassDetails
import io.sudostream.timetoteach.messages.systemwide.model.classtimetable.{ClassTimetable, TimeToTeachId}
import org.mongodb.scala.result.UpdateResult

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class MongoDbClassTimetableWriterDao(mongoFindQueriesProxy: MongoInserterProxy,
                                     actorSystemWrapper: ActorSystemWrapper) extends ClassTimetableWriterDao {

  implicit val system: ActorSystem = actorSystemWrapper.system
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: Materializer = actorSystemWrapper.materializer
  val logger: LoggingAdapter = system.log

  override def upsertClassTimetable(classTimetableToInsert: ClassTimetable): Future[UpdateResult] = {
    logger.info(s"Updating Class Timetable to Database: ${classTimetableToInsert.toString}")
    val updateCompleted = mongoFindQueriesProxy.upsertClassTimetable(classTimetableToInsert)

    updateCompleted.onComplete {
      case Success(completed) =>
        logger.info(s"Successfully updated class timetable ${classTimetableToInsert.toString}")
      case Failure(t) =>
        val errorMsg = s"Failed to inserted class timetable ${classTimetableToInsert.toString}" +
          s" with error ${t.getMessage}. Full stack trace .... ${t.getStackTrace.toString}"
        logger.error(errorMsg)
    }

    updateCompleted
  }

  override def upsertClass(classDetails: ClassDetails): Future[UpdateResult] = {
    logger.info(s"Updating Class Details to Database: ${classDetails.toString}")
    val updateCompleted = mongoFindQueriesProxy.upsertClass(classDetails)

    updateCompleted.onComplete {
      case Success(completed) =>
        logger.info(s"Successfully updated class details ${classDetails.toString}")
      case Failure(t) =>
        val errorMsg = s"Failed to inserted class details ${classDetails.toString}" +
          s" with error ${t.getMessage}. Full stack trace .... ${t.getStackTrace.toString}"
        logger.error(errorMsg)
    }

    updateCompleted
  }

  override def deleteClass(tttUserId: TimeToTeachId, classIdToDelete: String): Future[UpdateResult] = {
    logger.info(s"Deleting Class Id $classIdToDelete for user id $tttUserId")
    mongoFindQueriesProxy.deleteClass(tttUserId, classIdToDelete)
  }

}
