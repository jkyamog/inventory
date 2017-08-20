package inventory.domain

import java.util.UUID

import inventory.commands.ReduceItem
import inventory.domain.ItemHelper.itemEventHandler
import inventory.events._
import play.api.test.PlaySpecification

import scala.concurrent.ExecutionContext

class ItemSpec extends PlaySpecification {
  "ItemEventHandler" should {
    "reduce its quantity when its sold" in {
      val id = UUID.randomUUID()
      val item = Item(id, "test item", 3)
      val sell = ItemReduced(id, 2)

      val triedItem = itemEventHandler(sell)(Some(item))

      triedItem must beSuccessfulTry(Item(id, "test item", 1))
    }

    "increase it's quantity when its restocked" in {
      val id = UUID.randomUUID()
      val item = Item(id, "test item", 3)
      val restock = ItemIncreased(id, 2)

      val triedItem = itemEventHandler(restock)(Some(item))

      triedItem must beSuccessfulTry(Item(id, "test item", 5))
    }

    "not sell if the quantity is lower than sold and fail" in {
      val id = UUID.randomUUID()
      val item = Item(id, "test item", 2)
      val sell = ItemReduced(id, 3)

      val triedItem = itemEventHandler(sell)(Some(item))

      triedItem must beFailedTry
    }

    "archive an item" in {
      val id = UUID.randomUUID()
      val item = Item(id, "test item", 3)
      val archive = ItemArchived(id)

      val archiveItem = itemEventHandler(archive)(Some(item))

      archiveItem must beSuccessfulTry(Item(id, "test item", 3))
    }
  }

  "ItemHelper" should {

    "produce a sell failed notification event, when sell fails" in {
      val id = UUID.randomUUID()
      val item = Item(id, "test item", 2)
      val sell = ReduceItem(id, 3)

      import ItemHelper._
      import scala.concurrent.ExecutionContext.Implicits.global

      val ar = new AggregateRoot {
        override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
      }

      val (_, event) = await(ar.tryTo(sell)(Some(item)).or(notifySellFailed))
      event must beLike{ case SellFailedNotification(_, quantity, _) => quantity must beEqualTo(3) }
    }

  }

}
