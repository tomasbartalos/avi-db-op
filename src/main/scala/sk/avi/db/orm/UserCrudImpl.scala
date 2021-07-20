package sk.avi.db.orm

import java.sql.ResultSet

import javax.sql.DataSource
import sk.avi.model.User

import scala.collection.mutable

/**
 * Handles CRUD DB operations for User entity
 */
class UserCrudImpl(dataSource: DataSource) extends Crud(dataSource) with UserCrud {
  var CREATE_STMT: String = "CREATE TABLE SUSER (USER_ID bigint primary key, USER_GUID VARCHAR(255), USER_NAME VARCHAR(255))"
  var INSERT_STMT: String = "INSERT INTO SUSER (USER_ID, USER_GUID, USER_NAME) values (?,?,?)"
  var SELECT_STMT: String = "SELECT * FROM SUSER"
  var DELETE_STMT: String = "DELETE FROM SUSER"

  def createUserTable: Unit = {
    withExecuteUpdate(CREATE_STMT)()
  }

  def insert(user: User): Unit = {
    withExecuteUpdate(INSERT_STMT) { stmt =>
      stmt.setLong(1, user.userId)
      stmt.setString(2, user.guid)
      stmt.setString(3, user.name)
    }
  }

  def selectAll(): List[User] = {
    var users = mutable.ListBuffer[User]()
    withConn { conn =>
      val stmt = conn.prepareStatement(SELECT_STMT)
      val rs = stmt.executeQuery()
      while(rs.next()) {
        users += toUser(rs)
      }
    }
    users.toList
  }

  def toUser(rs: ResultSet): User = {
    User(rs.getLong(1), rs.getString(2), rs.getString(3))
  }

  def deleteAll(): Unit = {
    withExecuteUpdate(DELETE_STMT)()
  }
}
