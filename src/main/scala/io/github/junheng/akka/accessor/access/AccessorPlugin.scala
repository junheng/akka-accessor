package io.github.junheng.akka.accessor.access

import akka.actor.{ActorSystem, ActorRef}
import akka.event.LoggingAdapter
import spray.http.HttpRequest

trait AccessorPlugin {
  /**
   * 处理请求
    *
    * @param request 上行请求
   * @param receipt Actor 消息发送者
   * @return 返回false的情况下, 默认的消息拦截发送器将不会被执行
   */
  def processRequest(request: HttpRequest, receipt: ActorRef, log: LoggingAdapter, receiver: ActorSystem): Boolean
}
