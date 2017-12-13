package io.sudostream.classtimetable.dao

import org.scalatest.FunSuite
import org.scalatest.mockito.MockitoSugar
import ClassTimetableMongoDbSchema._
import org.mongodb.scala.bson.{BsonArray, BsonDocument}

class MongoInserterProxyImplTest extends FunSuite with MockitoSugar with ClassTimeTableGenerator {

  val connectionWrapperMock = mock[MongoDbConnectionWrapper]

  test("Class Timetable as Document has an epoch milli value") {
    val mongoInserterProxy = new MongoInserterProxyImpl(connectionWrapperMock)
    val classTimetableAsDocument = mongoInserterProxy.convertClassTimetableToDocumentWithEpoch(createClassTimetable())
    assert((classTimetableAsDocument.getLong(EPOCH_MILLI_UTC) > 0L) === true)
  }

  test("Class Timetable as Document has a class name of 'P3AB'") {
    val mongoInserterProxy = new MongoInserterProxyImpl(connectionWrapperMock)
    val classTimetableAsDocument = mongoInserterProxy.convertClassTimetableToDocumentWithEpoch(createClassTimetable())
    assert(classTimetableAsDocument.getString(CLASS_NAME) === "P3AB")
  }

  test("Class Timetable as Document has a classtimetables document with a 'school times' array") {
    val mongoInserterProxy = new MongoInserterProxyImpl(connectionWrapperMock)
    val classTimetableAsDocument = mongoInserterProxy.convertClassTimetableToDocumentWithEpoch(createClassTimetable())
    val maybeClassTimetablesDocument = classTimetableAsDocument.get[BsonDocument](CLASS_TIMETABLES_FOR_SPECIFIC_CLASS)
    maybeClassTimetablesDocument match {
      case Some(timetablesDoc) =>
        assert(timetablesDoc.containsKey(SCHOOL_TIMES))
      case None => fail("Did not get a Class Timetables array back")
    }
  }

  test("Class Timetable as Document has a classtimetables document with a 'all sessions of the week' array") {
    val mongoInserterProxy = new MongoInserterProxyImpl(connectionWrapperMock)
    val classTimetableAsDocument = mongoInserterProxy.convertClassTimetableToDocumentWithEpoch(createClassTimetable())
    val maybeClassTimetablesDocument = classTimetableAsDocument.get[BsonDocument](CLASS_TIMETABLES_FOR_SPECIFIC_CLASS)
    maybeClassTimetablesDocument match {
      case Some(timetablesDoc) =>
        assert(timetablesDoc.containsKey(ALL_SESSIONS_OF_THE_WEEK))
      case None => fail("Did not get a Class Timetables array back")
    }
  }

  test("Class Timetable as Document has a classtimetables document with only 2 fields") {
    val mongoInserterProxy = new MongoInserterProxyImpl(connectionWrapperMock)
    val classTimetableAsDocument = mongoInserterProxy.convertClassTimetableToDocumentWithEpoch(createClassTimetable())
    val maybeClassTimetablesDocument = classTimetableAsDocument.get[BsonDocument](CLASS_TIMETABLES_FOR_SPECIFIC_CLASS)
    maybeClassTimetablesDocument match {
      case Some(timetablesDoc) =>
        assert(timetablesDoc.size() === 2)
      case None => fail("Did not get a Class Timetables array back")
    }
  }

  test("Class Timetable document's school times array has 6 time boundaries") {
    val mongoInserterProxy = new MongoInserterProxyImpl(connectionWrapperMock)
    val classTimetableAsDocument = mongoInserterProxy.convertClassTimetableToDocumentWithEpoch(createClassTimetable())
    val maybeClassTimetablesDocument = classTimetableAsDocument.get[BsonDocument](CLASS_TIMETABLES_FOR_SPECIFIC_CLASS)
    maybeClassTimetablesDocument match {
      case Some(timetablesDoc) =>
        println(timetablesDoc.toString)
        assert(timetablesDoc.getArray(SCHOOL_TIMES).size() === 6)
      case None => fail("Did not get a Class Timetables array back")
    }
  }

  test("Class Timetable document's all sessions of the week array has 15 sessions") {
    val mongoInserterProxy = new MongoInserterProxyImpl(connectionWrapperMock)
    val classTimetableAsDocument = mongoInserterProxy.convertClassTimetableToDocumentWithEpoch(createClassTimetable())
    val maybeClassTimetablesDocument = classTimetableAsDocument.get[BsonDocument](CLASS_TIMETABLES_FOR_SPECIFIC_CLASS)
    maybeClassTimetablesDocument match {
      case Some(timetablesDoc) =>
        println(timetablesDoc.toString)
        assert(timetablesDoc.getArray(ALL_SESSIONS_OF_THE_WEEK).size() === 3)
      case None => fail("Did not get a Class Timetables array back")
    }
  }



}