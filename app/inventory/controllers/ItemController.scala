package inventory.controllers

import java.util.UUID
import javax.inject.Inject

import inventory.commands._
import inventory.events._
import inventory.reads.{EventStoreSubscriber, ReadDB}
import inventory.storage.{EventStore, SqlEventStore}
import inventory.domain.{AggregateRoot, ItemHelper}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future
import scala.util.Success

class Items @Inject() (val eventStore: SqlEventStore,
                       val readDB: ReadDB,
                       eventStoreSubscriber: EventStoreSubscriber) extends ItemController {

  eventStoreSubscriber.subscribe(eventStore.source)
}

trait ItemController extends Controller {

  import JsonHelpers._
  import AggregateRoot._

  val eventStore: EventStore

  val readDB: ReadDB

  def create = Action.async(parse.json) { request =>
    request.body.validate[CreateItem].fold (
      errors => {
        Future.successful(BadRequest(JsError.toJson(errors)))
      },
      createItem => {
        ItemHelper.itemCommandHandler(createItem)(None) match {  // TODO: collapse this
          case Success(itemCreated: ItemCreated) =>
            for {
              txId <- eventStore.saveEvent(itemCreated, itemCreated.id)
            } yield Ok(Json.obj("txId" -> txId, "eId" -> itemCreated.id)).withHeaders("content-type" -> "application/json")

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
    getById(uuid)(eventStore).map { item =>
      Ok(Json.toJson(item))
    }
  }

  def archive(id: String) = Action.async { request =>
    import ItemHelper._

    val uuid = UUID.fromString(id)
    for {
      item <- getById(uuid)(eventStore)
      (eId, event) <- tryTo(ArchiveItem())(Some(item)).doCommand
      txId <- eventStore saveEvent(event, eId)
    } yield Ok(Json.obj("txId" -> txId))
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
            item <- getById(uuid)(eventStore)
            (eId, event) <- tryTo(sell)(Some(item)).or(notifySellFailed)
            txId <- eventStore saveEvent(event, eId)
          } yield Ok(Json.obj("txId" -> txId))
      }
    )
  }

  def getAll = Action.async { request =>
    readDB.getAll.map { items  =>
      Ok(Json.toJson(items)(Writes.seq))
    }

  }
}
