package inventory.controllers

import java.util.UUID

import inventory.commands._
import inventory.events._
import inventory.storage.{SqlEventStore, EventStore}
import inventory.domain.{ItemCommandHandler, ItemHelper, AggregateRoot, Item}
import play.api.Logger

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future
import scala.util.Success

class Items extends ItemController {
  val eventStore = SqlEventStore
  inventory.reads.EventStoreSubscriber.init
}

trait ItemController extends Controller {

  import JsonHelpers._

  val eventStore: EventStore

  def create = Action.async(parse.json) { request =>
    request.body.validate[CreateItem].fold (
      errors => {
        Future.successful(BadRequest(JsError.toJson(errors)))
      },
      createItem => {
        ItemCommandHandler(createItem)(None) match {
          case Success(itemCreated: ItemCreated) =>
            for {
              txId <- eventStore.saveEvent(itemCreated, itemCreated.id)
            } yield Ok(Json.obj("txId" -> txId, "eId" -> itemCreated.id))

          case Success(event) =>
            Logger.error("unexpected event" + event)
            Future.failed(new RuntimeException("unexpected event" + event))
          case error =>
            Logger.debug("error: " + error)
            Future.successful(InternalServerError)
        }

      }
    )
  }

  def get(id: String) = Action.async { request =>
    import ItemHelper.itemEventHandler

    val uuid = UUID.fromString(id)
    AggregateRoot.getById(uuid)(eventStore).map { item =>
      Ok(Json.toJson(item))
    }
  }

  def sell(id: String) = Action.async(parse.json) { request =>
    request.body.validate[SellItem].fold (
      errors => {
        Future.successful(BadRequest(JsError.toJson(errors)))
      },
      sell => {
        import ItemHelper._

        val uuid = UUID.fromString(id)
          for {
            item <- AggregateRoot.getById(uuid)(eventStore)
            (eId, event) <- tryTo(sell)(Some(item))
            txId <- eventStore saveEvent(event, eId)
          } yield Ok(Json.obj("txId" -> txId))
      }
    )
  }
}
