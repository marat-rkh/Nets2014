package controllers

import play.api.mvc._
import play.api.libs.json._
import play.twirl.api.{Xml, Html}

import BandsDbManager._

import play.api.libs.functional.syntax._

import scala.language.postfixOps


object Application extends Controller {
  private val bandsDbManager = BandsDbManager

  def index = Action {
    Ok(views.html.main("RESTful")(Html("<h1>Welcome to RESTful service about black metal bands!</h1>")))
  }

  def handleGetJson(taskId: Int) =
    handleGet(
      taskId,
      (bandName, albums) => Ok(makeJsonResponse(bandName, albums)),
      NotFound(Json.obj("status" -> "error", "details" -> "no band with such name")),
      NotFound(taskNotFoundJson(taskId))
    )

  def handleGetXml(taskId: Int) =
    handleGet(
      taskId,
      (bandName, albums) => Ok(views.html.makeXmlResponse(bandName, albums)).as("application/xml"),
      NotFound(Html(
          "<response>" +
          "<status>error</status>" +
          "<details>no band with such name</details>" +
          "</response>"))
        .as("application/xml"),
      NotFound(Html(
          "<response>" +
          "<status>error</status>" +
          "<details>no task with id " + taskId + "</details>" +
          "</response>"))
        .as("application/xml")
    )

  def handleGetHtml(taskId: Int) =
    handleGet(
      taskId,
      (bandName, albums) => Ok(views.html.makeHtmlResponse(bandName, albums)),
      NotFound(Html("<h3>error: no band with such name</h3>")),
      NotFound(Html("<h3>error: no task with id " + taskId + "</h3>"))
    )

  implicit val putRds = (
      (__ \ 'name).read[String] and
      (__ \ 'year).read[Int]
    ) tupled

  def handlePut = Action(parse.json) { request =>
    request.body.validate[(String, Int)].map {
      case (name, year) =>
        val taskId = bandsDbManager put(name, year)
        Ok(Json.obj("status" -> "Ok", "task_id" -> taskId))
    }.recoverTotal {
      e => BadRequest(badRequestJson(e))
    }
  }

  implicit val postRds = (
      (__ \ 'id).read[Int] and
      (__ \ 'year).read[Int]
    ) tupled

  def handlePost = Action(parse.json) { request =>
    request.body.validate[(Int, Int)].map {
      case (id, year) =>
        bandsDbManager post (id, year) match {
          case true =>
            Ok(Json.obj("status" -> "Ok", "task_id" -> id))
          case false =>
            NotFound(taskNotFoundJson(id))
        }
    }.recoverTotal {
      e => BadRequest(badRequestJson(e))
    }
  }

  implicit val delRds = (__ \ 'id).read[Int]

  def handleDelete = Action(parse.json) { request =>
    request.body.validate[Int].map {
      case taskId =>
        bandsDbManager delete taskId match {
          case true =>
            Ok(Json.obj("status" -> "Ok", "task_id" -> taskId))
          case false =>
            NotFound(taskNotFoundJson(taskId))
        }
    }.recoverTotal {
      e => BadRequest(badRequestJson(e))
    }
  }

  def make405(arg1: String) = Action {
    MethodNotAllowed(Json.obj(
      "status" -> "error",
      "details" -> "This URI doesn't support specified request type"
    ))
  }

  def makeRootOptions() = Action {
    val getFormat = Json.obj()
    val putFormat = Json.obj("name" -> "band name", "year" -> "albums beginning from year (number)")
    val postFormat = Json.obj("id" -> "task id to update (number)", "year" -> "updated year (number)")
    val delFormat = Json.obj("id" -> "task id to delete (number)")
    Ok(Json.obj(
      "status" -> "Ok",
      "GET" -> getFormat,
      "PUT" -> putFormat,
      "POST" -> postFormat,
      "DELETE" -> delFormat
    ))
  }

  private def handleGet(taskId: TaskId,
                         okRes: (BandName, List[AlbumEntry]) => Result,
                         bandNotFoundRes: Result,
                         taskNotFoundRes: Result) = Action {
    bandsDbManager get taskId match {
      case Some(mbRes) => mbRes match {
        case Some((bandName, albums)) => okRes(bandName, albums)
        case None                     => bandNotFoundRes
      }
      case None                     => taskNotFoundRes
    }
  }

  private def makeJsonResponse(bandName: BandName, albums: List[(Year, AlbumName)]) = {
    Json.obj(
      "status" -> "Ok",
      "band"   -> bandName,
      "albums" -> albums.map { p => Json.obj("year" -> p._1, "title" -> p._2) }
    )
  }

  private def taskNotFoundJson(taskId: TaskId) =
    Json.obj("status" -> "error", "details" -> ("no task with id: " + taskId))

  private def badRequestJson(err: JsError) =
    Json.obj("status" -> "error", "details" -> JsError.toFlatJson(err))

}