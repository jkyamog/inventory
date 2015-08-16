package inventory.reads

import java.util.UUID

import inventory.domain.Item
import inventory.events.{ItemSold, ItemCreated}
import inventory.storage.EventTx
import play.api.test.{WithApplication, FakeApplication, PlaySpecification}


class ReadDBSpec extends PlaySpecification {
  "ReadDB" should {

    "update item when item is sold" in new WithApplication(FakeApplication()) {
      val readDB = new ReadDB

      val id = UUID.randomUUID
      val item = Item(id, "test", 10)
      await(readDB.insert(item))

      val sold = ItemSold(id, 3)
      val eventTx = EventTx(1, id, sold)

      val updatedNo = await(readDB.handle(eventTx))

      updatedNo must equalTo(1)

      val updated = await(readDB.get(id))
      updated must beSome(Item(id, "test", 7))
    }

    "insert new items when event is for created item" in new WithApplication(FakeApplication()) {
      val readDB = new ReadDB

      val id = UUID.randomUUID
      val create = ItemCreated(id, "test", None, 1, None, 2.0, None)
      val eventTx = EventTx(1, id, create)

      val insertedNo = await(readDB.handle(eventTx))

      val inserted = await(readDB.get(id))
      inserted must beSome(Item(id, "test", 1))
    }

  }

}
