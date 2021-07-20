package sk.avi.di

import org.h2.jdbcx.JdbcDataSource
import sk.avi.broker.{AtLeastOnceLockingStrategy, ExactlyOnceLockingStrategy, WorkQueue}
import sk.avi.consumer.DBOperationAsyncConsumer
import sk.avi.db.orm.{UserCrud, UserCrudImpl}
import sk.avi.model.DBOperation
import sk.avi.producer.DBOperationAsyncProducer

object AppContext {
  private val QUEUE_CAPACITY = 10
  private lazy val exaclyOncelockStrategy = new ExactlyOnceLockingStrategy()
  private lazy val atLeastOncelockStrategy = new AtLeastOnceLockingStrategy()

  // pick locking strategy - exactly-once or at-least-once
  lazy val workQueue = new WorkQueue(QUEUE_CAPACITY, exaclyOncelockStrategy)
  lazy val consumer = new DBOperationAsyncConsumer(workQueue)
  def producer(operations: Seq[DBOperation]) = new DBOperationAsyncProducer(workQueue, operations)
  val dataSource = {
    val ds = new JdbcDataSource
    ds.setURL("jdbc:h2:mem:avi;DB_CLOSE_DELAY=-1")
    ds.setUser("sa")
    ds.setPassword("sa")
    ds
  }
  val userCrud: UserCrud = new UserCrudImpl(dataSource)
  userCrud.createUserTable
}

