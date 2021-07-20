package sk.avi.consumer

import sk.avi.broker.WorkQueue
import sk.avi.broker.WorkQueue.POISON
import sk.avi.db.handler.DBHandler
import sk.avi.model.DBOperation

class DBOperationAsyncConsumer(workQueue: WorkQueue) extends Thread {
  var isPoisoned = false

  def shutdown(): Unit = {
    workQueue.shutdown()
  }

  override def run(): Unit = {
    runEventLoop()
  }

  private def runEventLoop(): Unit = {
    while(shouldRun) {
      takeFromQueue()
    }
  }

  private def takeFromQueue(): Unit = {
    val element = workQueue.takeElement()
    if (element == POISON) {
      isPoisoned = true
      return
    }
    processElementSafely(element.op)
  }

  private def shouldRun: Boolean = {
    // if not poisoned => continue
    // if poisoned => we want to process all elements in the poisoned queue
    // queue itself must ensure that no more elements can be added when poisoned
    !(isPoisoned && workQueue.isEmpty)
  }

  private def processElementSafely(op: DBOperation): Unit = {
    try {
      processElement(op)
    } catch {
      case e: Exception =>
        System.err.println(s"Error occurred when handling DB operation $op. Dropping operation.")
        e.printStackTrace(System.err)
    }
  }

  protected def processElement(op: DBOperation): Unit = {
    val handler = DBHandler.pickOperationHandler(op)
    handler.handle()
  }
}
