package sk.avi.producer

import sk.avi.broker.WorkQueue
import sk.avi.model.DBOperation

class DBOperationAsyncProducer(workQueue: WorkQueue, operations: Seq[DBOperation]) extends Thread {

  override def run(): Unit = {
    println(s"${Thread.currentThread()}: adding operations to queue: $operations")
    workQueue.addAllOperations(operations)
  }

}
