package sk.avi.model

/**
 * Algebraic Data Type (ADT) for DB operations
 */
sealed abstract class DBOperation extends Product with Serializable

object DBOperation {

  final case class Add(user: User) extends DBOperation
  final case class PrintAll() extends DBOperation
  final case class DeleteAll() extends DBOperation

}

case class User(userId: Long, guid: String, name: String)

object DBOperationImplicits {
  implicit def addSimplified(tuple3: (Int, String, String)) = User(tuple3._1, tuple3._2, tuple3._3)
}