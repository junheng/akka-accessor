package io.github.junheng.akka.accessor.access

import akka.actor.ActorRef
import akka.event.LoggingAdapter
import akka.spray.UnregisteredActorRef
import io.github.junheng.akka.accessor.access.AccessorCaller.ErrorMessage
import org.json4s.FieldSerializer._
import org.json4s.jackson.Serialization.write
import org.json4s._
import spray.http._
import AccessorCaller.formats

object AccessorCaller {
  private val serializer = FieldSerializer[Throwable](ignore("stackTrace") orElse ignore("cause"))
  implicit val formats = typedFormats(ShortTypeHints(List(classOf[Any]))) + serializer

  case class ErrorMessage(code: String, message: String)

  def typedFormats(hints: TypeHints): Formats = new Formats {
    val dateFormat = DefaultFormats.lossless.dateFormat

    override def typeHintFieldName: String = "_type"

    override val typeHints = hints
  }
}


class AccessorCaller(ref: ActorRef, log: LoggingAdapter) extends UnregisteredActorRef(ref) {
  private val created = System.nanoTime()

  override protected def handle(message: Any)(implicit sender: ActorRef): Unit = {
    val response = message match {
      case error: Throwable => responseError(error)
      case _ => try {
        HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, write(Some(message))))
      } catch {
        case error: Throwable => responseError(error)
      }
    }
    ref.tell(response, sender)
    val cost: Long = (System.nanoTime() - created) / 1000 / 1000
    if (cost > 100) {
      log.warning(s"message [${message.getClass.getCanonicalName}] replied in $cost ms")
    }
  }

  def responseError(error: Throwable): HttpResponse = {
    val errorResponse = ErrorMessage(error.getClass.getCanonicalName, error.getMessage)
    log.error(error, "can not parse message")
    HttpResponse(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`application/json`, write(Some(errorResponse))))
  }
}