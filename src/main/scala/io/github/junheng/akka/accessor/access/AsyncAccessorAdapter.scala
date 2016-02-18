package io.github.junheng.akka.accessor.access

import java.util.concurrent.Executors

import com.typesafe.config.ConfigFactory
import io.github.junheng.akka.accessor.access.Accessor.{PartMessageEntity, OctetMessageEntity, JsonMessageEntity, CodeMessageEntity}
import io.github.junheng.akka.accessor.access.AccessorAdapter.{PartMessage, OctetMessage}

import scala.concurrent.{ExecutionContext, Future}

trait AsyncAccessorAdapter extends AccessorAdapter {

  private val config = ConfigFactory.load()

  private val parallel = "accessor.async.parallel"

  private implicit val execution = ExecutionContext.fromExecutor(
    Executors.newFixedThreadPool({
      if (config.hasPath(parallel)) config.getInt(parallel)
      else Runtime.getRuntime.availableProcessors() * 4
    })
  )

  override val handler: Receive = {
    case entity: CodeMessageEntity => Future {
      parseCodeMessageEntity(entity)
    }
    case entity: JsonMessageEntity => Future {
      parseJsonMessageEntity(entity)
    }
    case entity: OctetMessageEntity => Future {
      self.tell(OctetMessage(entity.payload), entity.caller)
    }
    case entity: PartMessageEntity => Future {
      self.tell(PartMessage(entity.payload), entity.caller)
    }
  }
}
