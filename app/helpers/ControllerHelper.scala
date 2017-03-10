package helpers

import play.api.libs.json._
import play.api.mvc.{AnyContent, Request}

trait ControllerHelper {

  def getRequestItem[T: Format](implicit request: Request[AnyContent]): T = {
    val readJsonObject: Format[JsValue] = __.format[JsValue]

    getRequestBodyAsJson(request).validate(readJsonObject) match {
      case JsError(e) => throw JsResultException(e)
      case JsSuccess(jsValue, _) =>
        jsValue.validate[T] match {
          case JsSuccess(expectedItem, _) => expectedItem
          case JsError(e) => throw JsResultException(e)
        }
    }
  }

  private def getRequestBodyAsJson(implicit request: Request[AnyContent]): JsValue =
    request.body.asJson.getOrElse(throw new IllegalArgumentException("Request Body is not JSON!"))

  def jsonify[T : Writes](anything: T): JsValue = Json.toJson(anything)
}
