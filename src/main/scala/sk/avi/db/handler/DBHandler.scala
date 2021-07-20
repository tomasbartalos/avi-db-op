package sk.avi.db.handler

import sk.avi.model.DBOperation.{Add, DeleteAll, PrintAll}
import sk.avi.model.{DBOperation, User}

trait DBHandler {
  /**
   * Handles DB operation from model sk.avi.model.DBOperation
   */
  def handle()
}

object DBHandler {
  def pickOperationHandler(op: DBOperation): DBHandler = {
    op match {
      case Add(user: User) => new AddHandler(user)
      case DeleteAll() => new DeleteAllHandler()
      case PrintAll() => new PrintAllHandler()
    }
  }
}


