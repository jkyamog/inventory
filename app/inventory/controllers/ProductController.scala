package inventory.controllers

import inventory.domain.Product.ProductAggregate
import inventory.events.CreateProduct
import inventory.storage.EventStore
import inventory.domain.{AggregateRoot, Product}
import play.api.Play
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}

import play.api.libs.json._
import play.api.mvc._
import slick.driver.JdbcProfile


class ProductController extends Controller with HasDatabaseConfig[JdbcProfile] {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  import dbConfig.driver.api._

  implicit val productFormatter = Json.format[Product]
  implicit val productCreateFormatter = Json.format[CreateProduct]

  def create = Action(parse.json) { request =>
    request.body.validate[CreateProduct].fold (
      errors => {
        BadRequest(JsError.toJson(errors))
      },
      createProduct => {
        val (txId, eId) = EventStore saveEvent createProduct
        Ok(Json.obj("txId" -> txId, "eId" -> eId))
      }
    )
  }

  def get(id: Long) = Action { request =>
    import Product.ProductAggregate

    AggregateRoot.getById(id) match {
      case Some(product) => Ok(Json.toJson(product))
      case None => NotFound
    }
  }
}
