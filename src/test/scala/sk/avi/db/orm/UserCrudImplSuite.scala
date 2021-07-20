package sk.avi.db.orm

import org.h2.jdbcx.JdbcDataSource
import org.scalatest.{BeforeAndAfter, FunSuite}
import sk.avi.model.User

class UserCrudImplSuite extends FunSuite with BeforeAndAfter {
  val dataSource = {
    val ds = new JdbcDataSource
    ds.setURL("jdbc:h2:mem:avi;DB_CLOSE_DELAY=-1")
    ds.setUser("sa")
    ds.setPassword("sa")
    ds
  }
  val crud = new UserCrudImpl(dataSource)
  crud.createUserTable

  before {
    crud.deleteAll()
  }

  test("test insert and select from table") {
    val toInsert = User(1, "123-567", "Jozo")
    crud.insert(toInsert)
    val selected = crud.selectAll().head
    assert(toInsert == selected)
  }


  test("test insert many and select from table") {
    val toInsert = User(1, "123-567", "Jozo") ::
      User(2, "123-123", "Fero") ::
      User(3, "456-789", "Brano") :: Nil
    toInsert.foreach( u => crud.insert(u))

    val selected = crud.selectAll()
    assert(toInsert.toSet == selected.toSet)
  }

  test("test delete from table") {
    val toInsert = User(1, "123-567", "Jozo") ::
      User(2, "123-123", "Fero") ::
      User(3, "456-789", "Brano") :: Nil
    toInsert.foreach( u => crud.insert(u))
    crud.deleteAll()
    val selected = crud.selectAll()
    assert(selected == Nil)
  }

}
