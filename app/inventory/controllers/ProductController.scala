package inventory.controllers

import java.util.UUID

import inventory.commands._
import inventory.events._
import inventory.storage.{SqlEventStore, EventStore}
import inventory.domain.{ProductCommand, ProductHelper, AggregateRoot, Product}
import play.api.Logger

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class Products extends ProductController {
  val eventStore = SqlEventStore
  inventory.reads.Products.init
}

trait ProductController extends Controller {

  implicit val productFormatter = Json.format[Product]
  implicit val productCreateFormatter = Json.format[CreateProduct]
  implicit val sellProductFormatter = Json.format[SellProduct]

  val eventStore: EventStore

  def create = Action.async(parse.json) { request =>
    request.body.validate[CreateProduct].fold (
      errors => {
        Future.successful(BadRequest(JsError.toJson(errors)))
      },
      createProduct => {
        ProductCommand(createProduct)(None) match {
          case Success(productCreated: ProductCreated) =>
            for {
              txId <- eventStore.saveEvent(productCreated, productCreated.productId)
            } yield Ok(Json.obj("txId" -> txId, "eId" -> productCreated.productId))

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
    import ProductHelper.productEvents

    val productId = UUID.fromString(id)
    AggregateRoot.getById(productId)(eventStore).map { product =>
      Ok(Json.toJson(product))
    }
  }

  def sell(id: String) = Action.async(parse.json) { request =>
    request.body.validate[SellProduct].fold (
      errors => {
        Future.successful(BadRequest(JsError.toJson(errors)))
      },
      sellProduct => {
        import ProductHelper.productEvents

        def sell(product: Product) = {

          Future.fromTry(ProductCommand(sellProduct)(Some(product))).map{
              case soldProduct => (product.id, soldProduct)
            }.recover{
              case FailedToApply(command: SellProduct) =>
              Logger.debug("recovering from failed " + command)
              (UUID.randomUUID(), SellFailedNotification(product.id, sellProduct.quantity))
            }
        }

        val productId = UUID.fromString(id)
          for {
            product <- AggregateRoot.getById(productId)(eventStore)
            (eId, productSold) <- sell(product)
            txId <- eventStore saveEvent(productSold, eId)
          } yield Ok(Json.obj("txId" -> txId))
      }
    )
  }
}
