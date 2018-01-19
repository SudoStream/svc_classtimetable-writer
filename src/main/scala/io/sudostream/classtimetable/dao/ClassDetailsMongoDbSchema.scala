package io.sudostream.classtimetable.dao

object ClassDetailsMongoDbSchema {
  val CLASS_ID = "_id"
  val EPOCH_MILLI_UTC = "epochMillisUTC"
  val CLASS_NAME = "className"
  val CLASS_GROUPS = "classGroups"

  val TEACHERS_WITH_WRITE_ACCESS = "teachersWithWriteAccess"
  val TEACHERS_WHO_DELETED_CLASS = "teachersWhoDeletedClass"

//  val AUDIT_LOG = "audit_log"

  val GROUP_ID = "groupId"
  val GROUP_NAME = "groupName"
  val GROUP_TYPE = "groupType"
  val GROUP_LEVEL = "groupLevel"

}
