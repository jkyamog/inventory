package inventory.reads

import java.util.UUID

import inventory.domain.Item
import inventory.events.{ItemCreated, ItemReduced}
import inventory.storage.EventTx
import play.api.Application
import play.api.test.{PlaySpecification, WithApplication}
import play.api.inject.guice.GuiceApplicationBuilder


class ReadDBSpec extends PlaySpecification {
  "ReadDB" should {

    def appWithMemoryDatabase = new GuiceApplicationBuilder().configure(inMemoryDatabase("test")).build()

    "update item when item is sold" in new WithApplication(appWithMemoryDatabase) {
      val appToReadDb = Application.instanceCache[ReadDB]
      val readDB = appToReadDb(app)

      val id = UUID.randomUUID
      val item = Item(id, "test", 10)
      await(readDB.insert(item))

      val sold = ItemReduced(id, 3)
      val eventTx = EventTx(1, id, sold)

      val updatedNo = await(readDB.handle(eventTx))

      updatedNo must equalTo(1)

      val updated = await(readDB.get(id))
      updated must beSome(Item(id, "test", 7))
    }

    "insert new items when event is for created item" in new WithApplication(appWithMemoryDatabase) {
      val appToReadDb = Application.instanceCache[ReadDB]
      val readDB = appToReadDb(app)

      val id = UUID.randomUUID
      val create = ItemCreated(id, "test", None, 1, None, 2.0, None)
      val eventTx = EventTx(1, id, create)

      val insertedNo = await(readDB.handle(eventTx))

      val inserted = await(readDB.get(id))
      inserted must beSome(Item(id, "test", 1))
    }

  }

}
