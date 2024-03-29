package io.sudostream.classtimetable.dao

import io.sudostream.timetoteach.messages.systemwide.model.classes.ClassDetails
import io.sudostream.timetoteach.messages.systemwide.model.classtimetable.{ClassTimetable, TimeToTeachId}
import org.mongodb.scala.result.UpdateResult

import scala.concurrent.Future

trait ClassTimetableWriterDao {

  def upsertClassTimetable(classTimetableToInsert: ClassTimetable): Future[UpdateResult]

  def upsertClass(classDetails: ClassDetails): Future[UpdateResult]

  def deleteClass(tttUserId: TimeToTeachId, classIdToDelete: String): Future[UpdateResult]

}
