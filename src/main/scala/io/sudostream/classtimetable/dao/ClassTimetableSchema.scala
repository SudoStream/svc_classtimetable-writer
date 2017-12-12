package io.sudostream.classtimetable.dao

object ClassTimetableSchema {
  val TIME_TO_TEACH_ID = "_id"
  val EPOCH_MILLI_UTC = "epochMillisUTC"
  val CLASS_NAME = "className"
  val ALL_USER_CLASS_TIMETABLES = "allUserClassTimetables"
  val CLASS_TIMETABLES_FOR_SPECIFIC_CLASS = "classTimetablesForSpecificClass"


  // Sessions of the week
  val ALL_SESSIONS_OF_THE_WEEK = "allSessionsOfTheWeek"
  val SESSIONS_WEEK_SESSION_NAME = "sessionName"
  val SESSIONS_WEEK_DAY_OF_THE_WEEK = "dayOfTheWeek"
  val SESSIONS_WEEK_START_TIME = "startTime"
  val SESSIONS_WEEK_END_TIME = "endTime"
  val SESSIONS_WEEK_SUBJECTS = "subjects"

  val SUBJECT_DETAIL_NAME = "subjectName"
  val SUBJECT_DETAIL_START_TIME = "startTime"
  val SUBJECT_DETAIL_END_TIME = "endTime"
  val SUBJECT_DETAIL_ADDITIONAL_INFO = "additionalInfo"


  // School Times
  val SCHOOL_TIMES = "schoolTimes"
  val SESSION_BOUNDARY_NAME = "sessionBoundaryName"
  val SESSION_BOUNDARY_START_TIME = "boundaryStartTime"
  val SESSION_BOUNDARY_TYPE = "boundaryType"
  val SESSION_BOUNDARY_SESSION_NAME = "sessionName"

}
