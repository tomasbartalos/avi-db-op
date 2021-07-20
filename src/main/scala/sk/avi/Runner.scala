package sk.avi

import sk.avi.di.AppContext
import sk.avi.model.DBOperation
import sk.avi.model.DBOperation.{Add, DeleteAll, PrintAll}

object Runner {

  def main(args: Array[String]): Unit = {
    // to be able to use simplified Add op
    import sk.avi.model.DBOperationImplicits.addSimplified
    val operationSequence =
        Add(1, "a1", "Robert") ::
        Add(2, "a2", "Martin") ::
        PrintAll() ::
        DeleteAll() ::
        PrintAll() ::
        Nil

    runOperationInFiFo(operationSequence)
  }

  private def runOperationInFiFo(operationSequence: Seq[DBOperation]): Unit = {
    AppContext.consumer.start()
    val producer = AppContext.producer(operationSequence)
    producer.start()
    producer.join()
    AppContext.consumer.shutdown()
  }
}
