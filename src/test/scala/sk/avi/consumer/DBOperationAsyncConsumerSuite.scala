package sk.avi.consumer

import java.util.concurrent.atomic.AtomicInteger

import org.scalatest.{BeforeAndAfter, FunSuite}
import sk.avi.broker.{AtLeastOnceLockingStrategy, ExactlyOnceLockingStrategy, LockingStrategy, QueueClosedException, WorkQueue}
import sk.avi.model.DBOperation._
import sk.avi.model.{DBOperation, User}

import scala.collection.mutable

class DBOperationAsyncConsumerSuite extends FunSuite with BeforeAndAfter {
  var queue: WorkQueue = _
  var processedQueue: mutable.ListBuffer[DBOperation] = _
  var consumer: DBOperationAsyncConsumer = _

  before {
    initWithStrategy(new ExactlyOnceLockingStrategy)
  }

  private def initWithStrategy(lockingStrategy: LockingStrategy) = {
    queue = new WorkQueue(Integer.MAX_VALUE, lockingStrategy)
    processedQueue = mutable.ListBuffer[DBOperation]()
    consumer = new DBOperationAsyncConsumer(queue) {
      override def processElement(op: DBOperation): Unit = processedQueue += op
    }
  }

  test("test added element processed") {
    consumer.start()
    queue.addOperation(DBOperation.PrintAll())
    consumer.shutdown()
    consumer.join()
    assert(DBOperation.PrintAll() :: Nil == processedQueue.toList)
  }

  test("test many added elements processed") {
    consumer.start()
    val ops = PrintAll() ::
      DeleteAll() ::
      PrintAll() ::
      Add(User(1, "123-456", "Jozo")) ::
      DeleteAll() :: Nil
    queue.addAllOperations(ops)
    consumer.shutdown()
    consumer.join()
    assert(ops == processedQueue.toList)
  }

  test("test can't use shutdown pool") {
    consumer.start()
    queue.addOperation(DeleteAll())
    consumer.shutdown()
    intercept[QueueClosedException] {
      queue.addOperation(DeleteAll())
    }
  }


  test("poisoning empty queue") {
    consumer.start()
    consumer.shutdown()
    consumer.join()
    assert(processedQueue.toList == Nil)
  }

  test("exactly-once processing - number of success add operation equals to number of processed") {
    val expectedAddedCount = runMassiveQueueInsert(threadCount = 50)
    val actualAddedCount = processedQueue.size
    assert(expectedAddedCount == actualAddedCount)
  }

  test("at-least-once processing - number of success add operation is less than real processed") {
    initWithStrategy(new AtLeastOnceLockingStrategy())
    val expectedAddedCount = runMassiveQueueInsert(threadCount = 50)
    val actualAddedCount = processedQueue.size
    // in massive load of 100 threads at-least-once strategy should reveal
    // in reality more elements are processed than detected ->
    // exception is thrown to the producer even when the element will be processed by the consumer
    println(s"Number of duplicates: ${actualAddedCount - expectedAddedCount}")
    assert(expectedAddedCount < actualAddedCount)
  }

  private def runMassiveQueueInsert(threadCount: Int) = {
    val successAddCount = new AtomicInteger()
    val totalDuration = new AtomicInteger()
    consumer.start()
    val infiniteAdd: Runnable = () => {
      try {
        while (true) {
          val start = System.currentTimeMillis()
          queue.addOperation(DeleteAll())
          val duration = System.currentTimeMillis() - start
          totalDuration.addAndGet(duration.toInt)
          successAddCount.incrementAndGet()
        }
      } catch {
        case e: Exception => //finish thread
      }
    }
    // threads with massive append
    val threads = (1 to threadCount).map(_ => new Thread(infiniteAdd))
    threads.foreach{t => t.start()}
    Thread.sleep(5000)
    consumer.shutdown()
    threads.foreach(t => t.join())
    consumer.join()
    println(s"Average add duration: ${totalDuration.get().toDouble / successAddCount.get()} ms.")
    successAddCount.get()
  }

}
