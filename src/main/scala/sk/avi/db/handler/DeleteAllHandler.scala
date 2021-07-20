package sk.avi.db.handler

import sk.avi.di.AppContext

class DeleteAllHandler extends DBHandler {
  val userCrud = AppContext.userCrud

  override def handle(): Unit = {
    println(s"${Thread.currentThread()}: Deleting all users from DB")
    userCrud.deleteAll()
  }
}
