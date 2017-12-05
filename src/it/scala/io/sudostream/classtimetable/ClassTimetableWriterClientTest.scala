package io.sudostream.classtimetable

import akka.actor.ActorSystem
import akka.http.javadsl.model.RequestEntity
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import io.sudostream.timetoteach.kafka.serializing.systemwide.classtimetable.ClassTimetableSerializer
import io.sudostream.timetoteach.kafka.serializing.systemwide.model.UserPreferencesSerializer
import io.sudostream.timetoteach.messages.systemwide.model.classtimetable.ClassTimetable
import io.sudostream.timetoteach.messages.systemwide.model.classtimetable.sessions._
import io.sudostream.timetoteach.messages.systemwide.model.classtimetable.subjectdetail.{SubjectDetail, SubjectDetailAdditionalInfo, SubjectDetailWrapper, SubjectName}
import io.sudostream.timetoteach.messages.systemwide.model.classtimetable.time.{ClassTimetableSchoolTimes, DayOfTheWeek, EndTime, StartTime}
import org.scalatest._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ClassTimetableWriterClientTest extends FlatSpec with Matchers {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  println("ClassTimetableWriter Client Tester")

  def classTimeTableServiceIsRunning: Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(uri = "http://localhost:9047/health"))
  }

  "Posting 'Class Timetable' to class timetable writer service" should "return 200" in {
    val classTimetableSerializer = new ClassTimetableSerializer
    val futureHttpPostResponse = for {
      healthy <- classTimeTableServiceIsRunning
      if healthy.status.isSuccess()
      classTimetableBytes = classTimetableSerializer.serialize("ignore", createClassTimetable)
      postFutureResponse <- Http().singleRequest(
        HttpRequest(
          method = HttpMethods.POST,
          uri = "http://localhost:9047/api/classtimetables",
          entity = classTimetableBytes
        ))
    } yield postFutureResponse.status.isSuccess()

//    val eventualTerminated = system.terminate()
//    Await.result(eventualTerminated, 3.seconds)


    Await.result(futureHttpPostResponse, 10.seconds)
  }


  def createAllSessionsOfTheWeek(): scala.List[SessionOfTheDayWrapper] = {
    SessionOfTheDayWrapper(SessionOfTheDay(
      sessionName = SessionName("EarlyMorningSession"),
      dayOfTheWeek = DayOfTheWeek.MONDAY,
      startTime = StartTime("09:00"),
      endTime = EndTime("10:30"),
      subjects = List(SubjectDetailWrapper(SubjectDetail(
        SubjectName.EMPTY,
        StartTime("09:00"),
        EndTime("10:30"),
        SubjectDetailAdditionalInfo("")
      )))
    )) ::
      SessionOfTheDayWrapper(SessionOfTheDay(
        sessionName = SessionName("LateMorningSession"),
        dayOfTheWeek = DayOfTheWeek.MONDAY,
        startTime = StartTime("10:45"),
        endTime = EndTime("12:05"),
        subjects = List(SubjectDetailWrapper(SubjectDetail(
          SubjectName.EMPTY,
          StartTime("10:45"),
          EndTime("12:05"),
          SubjectDetailAdditionalInfo("")
        )))
      )) ::
      SessionOfTheDayWrapper(SessionOfTheDay(
        sessionName = SessionName("AfternoonSession"),
        dayOfTheWeek = DayOfTheWeek.MONDAY,
        startTime = StartTime("13:00"),
        endTime = EndTime("15:00"),
        subjects = List(SubjectDetailWrapper(SubjectDetail(
          SubjectName.EMPTY,
          StartTime("13:00"),
          EndTime("15:00"),
          SubjectDetailAdditionalInfo("")
        )))
      )) :: Nil
  }

  def createSessionBoundaries(): List[SessionBoundaryWrapper] = {
    SessionBoundaryWrapper(SessionBoundary(
      sessionBoundaryName = SessionBoundaryName("SchoolStarts"),
      boundaryStartTime = StartTime("09:00"),
      boundaryType = SessionBoundaryType.START_OF_TEACHING_SESSION,
      sessionName = Some(SessionName("EarlyMorningSession"))
    )) ::
      SessionBoundaryWrapper(SessionBoundary(
        sessionBoundaryName = SessionBoundaryName("MorningBreakStarts"),
        boundaryStartTime = StartTime("10:30"),
        boundaryType = SessionBoundaryType.END_OF_TEACHING_SESSION,
        sessionName = None)
      ) ::
      SessionBoundaryWrapper(SessionBoundary(
        sessionBoundaryName = SessionBoundaryName("MorningBreakEnds"),
        boundaryStartTime = StartTime("10:45"),
        boundaryType = SessionBoundaryType.START_OF_TEACHING_SESSION,
        sessionName = Some(SessionName("LateMorningSession"))
      )) ::
      SessionBoundaryWrapper(SessionBoundary(
        sessionBoundaryName = SessionBoundaryName("LunchStarts"),
        boundaryStartTime = StartTime("12:05"),
        boundaryType = SessionBoundaryType.END_OF_TEACHING_SESSION,
        sessionName = None)
      ) ::
      SessionBoundaryWrapper(SessionBoundary(
        sessionBoundaryName = SessionBoundaryName("LunchEnds"),
        boundaryStartTime = StartTime("13:00"),
        boundaryType = SessionBoundaryType.START_OF_TEACHING_SESSION,
        sessionName = Some(SessionName("AfternoonSession"))
      )) ::
      SessionBoundaryWrapper(SessionBoundary(
        sessionBoundaryName = SessionBoundaryName("SchoolEnds"),
        boundaryStartTime = StartTime("15:00"),
        boundaryType = SessionBoundaryType.END_OF_TEACHING_SESSION,
        sessionName = None)
      ) :: Nil
  }

  def createSchoolTimes(): ClassTimetableSchoolTimes = {
    val sessionBoundaries: List[SessionBoundaryWrapper] = createSessionBoundaries()

    ClassTimetableSchoolTimes(
      schoolSessionBoundaries = sessionBoundaries
    )
  }

  def createClassTimetable: ClassTimetable = {
    val theSchoolTimes: ClassTimetableSchoolTimes = createSchoolTimes()
    val theAllSessionsOfTheWeek: List[SessionOfTheDayWrapper] = createAllSessionsOfTheWeek()

    ClassTimetable(
      "1234",
      schoolTimes = theSchoolTimes,
      allSessionsOfTheWeek = theAllSessionsOfTheWeek
    )
  }

}
