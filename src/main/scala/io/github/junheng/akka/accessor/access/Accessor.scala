package io.github.junheng.akka.accessor.access

import akka.actor._
import akka.io.{Tcp, IO}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.github.junheng.akka.accessor.access.Accessor._
import spray.can.Http
import spray.can.server.Stats
import spray.http.{MultipartFormData, HttpMethods, HttpCharsets, HttpRequest}
import spray.httpx.unmarshalling.{FormDataUnmarshallers, Deserialized}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class Accessor(address: String, port: Int, receiver: ActorSystem, plugins: List[AccessorPlugin] = Nil, intervalOfStatusReport:Int) extends Actor with ActorLogging {

  import context.dispatcher

  implicit val timeout = Timeout(10 seconds)

  override def preStart(): Unit = {
    log.info("started, initializing http service...")
    IO(Http)(context.system) ! Http.Bind(self, interface = address, port = port)
  }

  override def aroundReceive(receive: Actor.Receive, msg: Any): Unit = {
    super.aroundReceive(receive, msg)
  }

  override def receive: Receive = {
    case bound: Tcp.Bound =>
      context.system.scheduler.schedule(intervalOfStatusReport minutes, intervalOfStatusReport minutes, sender(), Http.GetStats)
      log.info(s"bounded to ${bound.localAddress}, http services initialized")
    case connected: Tcp.Connected => sender() ! Tcp.Register(self)
    case stats: Stats =>
      import stats._
      log.info(s"HTTP totalReqs [$totalRequests] openReqs [$openRequests] maxOpReqs [$maxOpenRequests] totalConn [$totalConnections] openConn [$openConnections] maxOpConn [$maxOpenConnections] timeouts [$requestTimeouts]")
    case request: HttpRequest =>
      val receipt = sender()
      Future {
        if (plugins.forall(_.processRequest(request, receipt, log, receiver))) process(request, receipt)
      }(context.dispatcher)
  }

  def process(request: HttpRequest, receipt: ActorRef): Unit = {
    val path = request.uri.path.toString()
    if (request.uri.toString().matches("(.+)code=(.+)")) {
      val code = request.uri.query.toMap("code")
      val caller: AccessorCaller = new AccessorCaller(receipt, log)
      val entityOpt: Option[MessageEntity] = request.method match {
        case HttpMethods.POST =>
          val contentType = request.headers.find(_.name.toLowerCase == "content-type") match {
            case Some(header) => header.value.toLowerCase
            case None => "application/json"
          }
          contentType match {
            case method if method.startsWith("text/plain") =>
              Some(JsonMessageEntity(code, request.entity.asString(HttpCharsets.`UTF-8`), caller))

            case "application/json" | "text/plain" =>
              Some(JsonMessageEntity(code, request.entity.asString(HttpCharsets.`UTF-8`), caller))

            case "application/octet-stream" =>
              val content = request.entity.data.toByteArray
              Some(OctetMessageEntity(code, content, caller))

            case "multipart/form-data" =>
              val unmarshaller: Deserialized[MultipartFormData] = FormDataUnmarshallers.multipartFormDataUnmarshaller()(request.entity)
              unmarshaller match {
                case Left(deserializationError) => None
                case Right(data) => Some(PartMessageEntity(code, data, caller))
              }
            case _ => None
          }
        case HttpMethods.GET => Some(CodeMessageEntity(code, caller))
      }
      entityOpt match {
        case Some(entity) => receiver.actorSelection(path).tell(entity, receipt)
        case None => caller ! new Exception("unsupported request")
      }
    }
  }
}


object Accessor {
  val container = ActorSystem("accessor", ConfigFactory.parseString("akka {}"))

  def start(address: String, port: Int, plugins: List[AccessorPlugin] = Nil, intervalOfStatusReport:Int = 15)(implicit receiver: ActorSystem) = {
    container.actorOf(Props(new Accessor(address, port, receiver, plugins, intervalOfStatusReport)), "protocol")
  }

  abstract class MessageEntity(code: String, caller: ActorRef)

  case class CodeMessageEntity(code: String, caller: ActorRef) extends MessageEntity(code, caller)

  case class JsonMessageEntity(code: String, payload: String, caller: ActorRef) extends MessageEntity(code, caller)

  case class OctetMessageEntity(code: String, payload: Array[Byte], caller: ActorRef) extends MessageEntity(code, caller)

  case class PartMessageEntity(code: String, payload: MultipartFormData, caller: ActorRef) extends MessageEntity(code, caller)

}