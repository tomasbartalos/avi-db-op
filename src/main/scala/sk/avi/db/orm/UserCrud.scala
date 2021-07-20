package sk.avi.db.orm

import sk.avi.model.User

trait UserCrud {
  def createUserTable: Unit

  def insert(user: User)

  def selectAll(): List[User]

  def deleteAll(): Unit
}
