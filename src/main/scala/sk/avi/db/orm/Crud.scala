package sk.avi.db.orm

import java.sql.{Connection, PreparedStatement}

import javax.sql.DataSource

class Crud(dataSource: DataSource) {

  def withExecuteUpdate(stmt: String)
                               (closure: PreparedStatement => Unit = {_=>}): Int = {
    var updateCount = 0
    withConn { conn =>
      val preparedStmt = conn.prepareStatement(stmt)
      closure(preparedStmt)
      updateCount = preparedStmt.executeUpdate()
      preparedStmt.close()
    }
    updateCount
  }

  def withConn(closure: Connection => Unit): Unit = {
    val conn = dataSource.getConnection
    try {
      closure(conn)
    } finally {
      conn.close()
    }
  }
}
