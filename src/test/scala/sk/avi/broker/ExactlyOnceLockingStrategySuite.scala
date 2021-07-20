package sk.avi.broker

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import org.scalatest.FunSuite

import scala.collection.JavaConverters._

class ExactlyOnceLockingStrategySuite extends FunSuite {
  val mutexLock = new ExactlyOnceLockingStrategy()
  val optimisticLock = new AtLeastOnceLockingStrategy()
  implicit val workQueue = new WorkQueue(1, mutexLock)

  test("Optimistic lock: Write lock and read lock executes in parallel") {
    val executedParallel = executeThreadsWithLock(optimisticLock)
    assert(executedParallel)
  }

  test("Mutex lock: Write lock and read lock have to execute serially") {
    val executedParallel = executeThreadsWithLock(mutexLock)
    assert(!executedParallel)
  }

  test("Mutex lock: read lock can re-enter lock") {
    val readCountByThread = execute10ThreadsInParallel(mutexLock.withAddLock)
    // we drop the first value, because it is 1
    readCountByThread.values.toList.sorted.drop(1).foreach( activeCount =>
      //we assert each thread run in parallel with at least one thread, but typically it is much higher
      assert(activeCount > 1)
    )
  }

  test("Mutex lock: write lock can't re-enter lock and is executed serially") {
    val readCountByThread = execute10ThreadsInParallel(mutexLock.withShutdownLock)
    // we drop the first value, because it is 1
    readCountByThread.values.foreach( activeCount =>
      // serial execution, at the same time only 1 thread can be active
      assert(activeCount == 1)
    )
  }

  private def execute10ThreadsInParallel(lock: (=> Unit) => Unit): Map[String, Integer] = {
    val readActiveCount = new AtomicInteger()
    val readCountByThread = new ConcurrentHashMap[String, Integer]()

    val readThreads = (1 to 10).map(_ => new Thread(() => {
      lock {
        val activeCount = readActiveCount.incrementAndGet()
        readCountByThread.put(Thread.currentThread().toString, activeCount)
        Thread.sleep(3000)
        readActiveCount.decrementAndGet()
      }
    }))
    readThreads.foreach(_.start())
    readThreads.foreach(_.join())
    assert(readCountByThread.size() == 10)
    readCountByThread.asScala.toMap
  }

  def executeThreadsWithLock(lock: LockingStrategy): Boolean = {
    val readActive = new AtomicBoolean(false)
    var executesInParalel = false
    val t1 = new Thread(() => {
      lock.withAddLock {
        readActive.set(true)
        Thread.sleep(5000)
        readActive.set(false)
      }
    })
    val t2 = new Thread(() => {
      lock.withShutdownLock {
        executesInParalel = readActive.get()
        Thread.sleep(3000)
        if (!executesInParalel) {
          executesInParalel = readActive.get()
        }
      }
    })
    t1.start()
    Thread.sleep(1000)
    t2.start()

    t1.join()
    t2.join()
    executesInParalel
  }
}
