package main

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileAndResourceDirectives.getFromResource
import akka.http.scaladsl.server.directives.MarshallingDirectives.{as, entity}
import akka.http.scaladsl.server.directives.MethodDirectives.{get, post}
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import sangria.marshalling.sprayJson._
//import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
import akka.stream.ActorMaterializer
import sangria.parser.QueryParser
import scala.util.{Failure, Success}
import spray.json.{JsObject, JsString, JsValue}


object Server extends App {
  implicit val system = ActorSystem("sangria-server")
  implicit val materializer = ActorMaterializer()

  val route: Route =
    (post & path("graphql")) {
      entity(as[JsValue]) { requestJson ⇒
        graphQLEndpoint(requestJson)
      }
    } ~
      get {
        getFromResource("graphiql.html")
      }

  Http().bindAndHandle(route, "0.0.0.0", 8080)

  def graphQLEndpoint(requestJson: JsValue) = {
    val JsObject(fields) = requestJson
    val JsString(query) = fields("query")

    val operation = fields.get("operationName") collect {
      case JsString(op) => op
    }

    val vars = fields.get("variablels") match {
      case Some(obj: JsObject) => obj
      case _ => JsObject.empty
    }

    QueryParser.parse(query) match {

      // query parsed successfully, time to execute it!
      case Success(queryAst) ⇒
        complete(app.executeGraphQLQuery(queryAst, operation, vars))

      // can't parse GraphQL query, return error
      case Failure(error) ⇒
        complete(BadRequest, JsObject("error" → JsString(error.getMessage)))
    }
  }
}

