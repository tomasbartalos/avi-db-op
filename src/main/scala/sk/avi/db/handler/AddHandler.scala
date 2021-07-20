package sk.avi.db.handler

import sk.avi.di.AppContext
import sk.avi.model.User

class AddHandler(user: User) extends DBHandler {
  val userCrud = AppContext.userCrud

  override def handle(): Unit = {
    println(s"${Thread.currentThread()}: Adding user to DB: $user")
    userCrud.insert(user)
  }

}
