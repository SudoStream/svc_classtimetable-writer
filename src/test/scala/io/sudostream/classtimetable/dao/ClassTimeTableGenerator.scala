package io.sudostream.classtimetable.dao

import io.sudostream.timetoteach.messages.systemwide.model.classtimetable.{ClassName, ClassTimetable, TimeToTeachId}
import io.sudostream.timetoteach.messages.systemwide.model.classtimetable.sessions._
import io.sudostream.timetoteach.messages.systemwide.model.classtimetable.subjectdetail.{SubjectDetail, SubjectDetailAdditionalInfo, SubjectDetailWrapper, SubjectName}
import io.sudostream.timetoteach.messages.systemwide.model.classtimetable.time.{ClassTimetableSchoolTimes, DayOfTheWeek, EndTime, StartTime}

trait ClassTimeTableGenerator {

  def createClassTimetable(): ClassTimetable = {
    val theSchoolTimes: ClassTimetableSchoolTimes = createSchoolTimes()
    val theAllSessionsOfTheWeek: List[SessionOfTheDayWrapper] = createAllSessionsOfTheWeek()

    ClassTimetable(
      TimeToTeachId("1234"),
      ClassName("P3AB"),
      schoolTimes = theSchoolTimes,
      allSessionsOfTheWeek = theAllSessionsOfTheWeek
    )
  }


  private def createAllSessionsOfTheWeek(): scala.List[SessionOfTheDayWrapper] = {
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

  private def createSchoolTimes(): ClassTimetableSchoolTimes = {
    val sessionBoundaries: List[SessionBoundaryWrapper] = createSessionBoundaries()

    ClassTimetableSchoolTimes(
      schoolSessionBoundaries = sessionBoundaries
    )
  }


}
