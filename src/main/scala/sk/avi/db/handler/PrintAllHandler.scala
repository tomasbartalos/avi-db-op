package sk.avi.db.handler

import sk.avi.di.AppContext

class PrintAllHandler extends DBHandler {
  val userCrud = AppContext.userCrud

  override def handle(): Unit = {
    println(s"${Thread.currentThread()}: Printing all users from DB")
    val users = userCrud.selectAll()
    println(s"Users in database: $users")
  }
}
