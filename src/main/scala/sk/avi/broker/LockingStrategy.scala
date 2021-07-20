package sk.avi.broker

import java.util.concurrent.locks.StampedLock

trait LockingStrategy {
  def withAddLock(handler: => Unit)(implicit workQueue: WorkQueue): Unit

  def withShutdownLock(handler: => Unit)(implicit workQueue: WorkQueue): Unit

  def checkNotPoisoned()(implicit workQueue: WorkQueue): Unit = {
    if (workQueue.isPoisoned) {
      throw new QueueClosedException(s"Queue is already closed, can't add new elements")
    }
  }
}

/**
 * Guarantees exactly-once delivery semantics.
 * Slower than at-least-once however in case of shutdown guarantees exactly-once processing.
 *
 * When queue's add operation throws an exception, it is guaranteed that the element was not processed.
 * Subsequent retry is guaranteed to be first element delivery.
 */
class ExactlyOnceLockingStrategy extends LockingStrategy {
  val lock = new StampedLock()

  override def withAddLock(handler: => Unit)(implicit workQueue: WorkQueue): Unit = {
    checkNotPoisoned()
    val stamp = lock.readLock()
    try {
      handler
    } finally {
      lock.unlock(stamp)
    }
  }

  override def withShutdownLock(handler: => Unit)(implicit workQueue: WorkQueue): Unit = {
    val stamp = lock.writeLock()
    try {
      handler
    } finally {
      lock.unlock(stamp)
    }
  }
}

/**
 * Guarantees at-least-once delivery semantics.
 * This is faster than exactly-once delivery
 * however in case of queue shutdown doesn't guarantee exactly-once delivery.
 *
 * When queue's add operation throws an exception, the element may or may not be processed.
 * Subsequent retry causes possible duplication.
 */
class AtLeastOnceLockingStrategy extends LockingStrategy {

  /**
   * In a race condition this method may throw QueueClosedException
   * even if the operation was accepted by the queue.
   * In case of failure producer should retry the operation
   * and it is up to consumer to deduplicate duplicates.
   *
   * Guarantees At-least-once delivery semantics
   */
  override def withAddLock(handler: => Unit)(implicit workQueue: WorkQueue): Unit = {
    checkNotPoisoned
    handler
    checkNotPoisoned
  }

  override def withShutdownLock(handler: => Unit)(implicit workQueue: WorkQueue): Unit = {
    // no checking required here
    handler
  }
}
