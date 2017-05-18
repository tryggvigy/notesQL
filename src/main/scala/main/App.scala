package main

import akka.http.scaladsl.model.StatusCodes._
import sangria.ast.Document
import scala.concurrent.ExecutionContext.Implicits.global
import sangria.schema._
import sangria.macros.derive._
import sangria.macros._
import sangria.execution._
import spray.json.JsObject

object app {

  case class Picture(width: Int, height: Int, url: Option[String])
  implicit val PictureType =
    deriveObjectType[Unit, Picture](
      ObjectTypeDescription("The product picture"),
      DocumentField("url", "Picture CDN URL"))

  trait Identifiable {
    def id: String
  }

  val IdentifiableType = InterfaceType(
    "Identifiable",
    "Entity that can be identified",

    fields[Unit, Identifiable](
      Field("id", StringType, resolve = _.value.id)))

  case class Product(id: String, name: String, description: String) extends Identifiable {
    def picture(size: Int): Picture =
      Picture(width = size, height = size, url = Some(s"//cdn.com/$size/$id.jpg"))
  }

  val ProductType =
    deriveObjectType[Unit, Product](
      Interfaces(IdentifiableType),
      IncludeMethods("picture"))

  val Id = Argument("id", StringType)

  val QueryType = ObjectType("Query", fields[ProductRepo, Unit](
    Field("product", OptionType(ProductType),
      description = Some("Returns a product with specific `id`."),
      arguments = Id :: Nil,
      resolve = c => c.ctx.product(c arg Id)),

    Field("products", ListType(ProductType),
      description = Some("Returns a list of all available products."),
      resolve = _.ctx.products)))

  val schema = Schema(QueryType)

  val query =
    graphql"""
      query MyProduct {
        product(id: "2") {
          name
          description

          picture(size: 500) {
            width, height, url
          }
        }

        products {
          name
        }
      }
    """

//  val result: Future[Json] =
//    Executor.execute(schema=schema, queryAst=query, userContext = new ProductRepo)

  def executeGraphQLQuery(query: Document, op: Option[String], vars: JsObject) =
    Executor.execute(schema, query, new ProductRepo, variables = vars, operationName = op)
      .map(OK → _)
      .recover {
        case error: QueryAnalysisError ⇒ BadRequest → error.resolveError
        case error: ErrorWithResolver ⇒ InternalServerError → error.resolveError
      }
}
