package com.example.quickstart

import cats.effect.{ContextShift, Sync}
import cats.implicits._
import org.http4s.dsl.Http4sDsl
import sttp.tapir._
import sttp.tapir.json.circe._
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final case class Person(name: String, age: Int)

object QuickstartRoutes {


  val booksListing: Endpoint[Person, String, List[Person], Nothing] = endpoint
    .get
    .in(("person" / path[String]("name") / path[Int]("age")).mapTo(Person))
    .errorOut(stringBody)
    .out(jsonBody[List[Person]])

  // Generate Swagger at path: /docs
  def personSwaggerRoutes[F[_]: Sync](implicit C: ContextShift[F]) = {
    import sttp.tapir.openapi.OpenAPI
    import sttp.tapir.docs.openapi._
    import sttp.tapir.openapi.circe.yaml._
    import sttp.tapir.swagger.http4s.SwaggerHttp4s


    val docs: OpenAPI = booksListing.toOpenAPI("My Bookshop", "1.0")
    //  println(docs.toYaml)

    new SwaggerHttp4s(docs.toYaml).routes[F]
  }

  def personRoutes[F[_] : Sync](implicit C: ContextShift[F]): HttpRoutes[F] = {
    booksListing.toRoutes(person => {
      Sync[F].delay {
        List(person).asRight
      }
    })
  }

  def jokeRoutes[F[_] : Sync](J: Jokes[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "joke" =>
        for {
          joke <- J.get
          resp <- Ok(joke)
        } yield resp
    }
  }

  def helloWorldRoutes[F[_] : Sync](H: HelloWorld[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "hello" / name =>
        for {
          greeting <- H.hello(HelloWorld.Name(name))
          resp <- Ok(greeting)
        } yield resp
    }
  }
}