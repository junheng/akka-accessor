package io.github.junheng.akka.accessor.access

import akka.actor.{Actor, ActorLogging}
import io.github.junheng.akka.accessor.access.Accessor.{CodeMessageEntity, JsonMessageEntity, OctetMessageEntity, PartMessageEntity}
import io.github.junheng.akka.accessor.access.AccessorAdapter.{Children, OctetMessage, PartMessage}
import org.json4s.{Extraction, DefaultFormats}
import org.json4s.reflect.ScalaType
import spray.http.MultipartFormData
import org.json4s.jackson.JsonMethods._

import scala.reflect.runtime.universe

trait AccessorAdapter extends Actor with ActorLogging {
  private implicit val format = DefaultFormats

  private val runtime = universe.runtimeMirror(getClass.getClassLoader)

  protected val handler: Receive = {
    case entity: CodeMessageEntity => parseCodeMessageEntity(entity)
    case entity: JsonMessageEntity => parseJsonMessageEntity(entity)
    case entity: OctetMessageEntity => self.tell(OctetMessage(entity.payload), entity.caller)
    case entity: PartMessageEntity => self.tell(PartMessage(entity.payload), entity.caller)
  }

  abstract override def receive = handler orElse super.receive

  def parseCodeMessageEntity(entity: CodeMessageEntity): Unit = {
    entity.code match {
      case "GetChildren" => entity.caller.tell(Children(context.children.map(_.path.name).toList), self)
      case _ =>
        try {
          val module = runtime.staticModule(entity.code)
          val message = runtime.reflectModule(module).instance
          self.tell(message, entity.caller)
        } catch {
          case ex: Throwable =>
            log.warning(s"[PARSE CODE ERROR]\n${entity.code}")
            entity.caller.tell(ex, self)
        }
    }
  }

  def parseJsonMessageEntity(entity: JsonMessageEntity): Unit = {
    try {
      val clazz = runtime.runtimeClass(runtime.staticClass(entity.code))
      val started = System.nanoTime()
      val message = Extraction.extract(parse(entity.payload), ScalaType(clazz))
      val cost = System.nanoTime() - started
      if (cost > 1000 * 1000 * 10) {
        //process a message use more than 10ms
        log.info(s"parse message cost ${cost / 1000 / 1000} mills for ${entity.payload.getBytes().length / 1024} kb")
      }

      self.tell(message, entity.caller)
    } catch {
      case ex: Exception =>
        log.warning(s"[PARSE MESSAGE ERROR]\n${entity.payload}")
        entity.caller.tell(ex, self)
    }
  }
}

object AccessorAdapter {

  case class Children(actors: List[String])

  case class OctetMessage(payload: Array[Byte])

  case class PartMessage(payload: MultipartFormData)

}
