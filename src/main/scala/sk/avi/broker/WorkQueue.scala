package sk.avi.broker

import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicBoolean

import sk.avi.broker.WorkQueue.POISON
import sk.avi.model.DBOperation

import scala.collection.JavaConverters._

class WorkQueue(capacity: Int, lockStrategy: LockingStrategy) {
  private val poisoned: AtomicBoolean = new AtomicBoolean(false)
  val queue = new LinkedBlockingDeque[QueueElement](capacity)
  // LockStrategy have circular dependency, solved by implicitly passing "this" queue to lock methods
  implicit val workQueue = this

  def shutdown(): Unit = {
    lockStrategy.withShutdownLock {
      poisoned.set(true)
      queue.add(POISON)
    }
  }

  def addOperation(op: DBOperation): Unit = {
    lockStrategy.withAddLock {
      queue.add(QueueElement(op))
    }
  }


  def addAllOperations(ops: Seq[DBOperation]): Unit = {
    lockStrategy.withAddLock {
      queue.addAll(ops.map(QueueElement(_)).asJava)
    }
  }

  def takeElement(): QueueElement = queue.take()

  def isEmpty: Boolean = queue.isEmpty

  def isPoisoned: Boolean = poisoned.get()

  def withLock(handler: => Unit): Unit = {
    checkPoisoned()
    handler
    checkPoisoned()
  }

  private def checkPoisoned(): Unit = {
    if (poisoned.get()) {
      throw new QueueClosedException(s"Queue is already closed, can't add new elements")
    }
  }
}

object WorkQueue {
  val POISON = QueueElement(null, true)
}

case class QueueElement(op: DBOperation, isPoisoned: Boolean = false)

class QueueClosedException(msg: String) extends RuntimeException(msg)
