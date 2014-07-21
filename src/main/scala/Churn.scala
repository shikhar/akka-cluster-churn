import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ Semaphore, ExecutorService, Executors }

import akka.actor.{ Actor, Props, ActorSystem }
import akka.cluster.{ ClusterEvent, Cluster }
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration
import scala.concurrent.{ Future, Await, Promise }

object Churn {

  case class Node(system: ActorSystem, cluster: Cluster)

  val SystemName = "churn"
  val BasePort = 8400

  def node(port: Int): Node = {
    val cfg = ConfigFactory.parseMap(Map(
      "akka.remote.netty.tcp.hostname" -> "127.0.0.1",
      "akka.remote.netty.tcp.port" -> new Integer(port),
      "akka.cluster.seed-nodes" -> Collections.singletonList(s"akka.tcp://$SystemName@127.0.0.1:$BasePort")))
    val system = ActorSystem(SystemName, cfg.withFallback(ConfigFactory.load()))
    val cluster = Cluster(system)
    Node(system, cluster)
  }

  def clusterUp(node: Node): Future[_] = {
    val p = Promise[Any]()
    node.cluster.registerOnMemberUp {
      p.success(Nil)
    }
    p.future
  }

  def leave(node: Node): Future[_] = {
    val p = Promise[Any]()
    node.cluster.subscribe(node.system.actorOf(Props(new Actor {
      override def receive = {
        case ClusterEvent.MemberRemoved(m, _) if m.address == node.cluster.selfAddress =>
          p.success(Nil)
          context.stop(self)
      }
    })), classOf[ClusterEvent.MemberEvent])
    node.cluster.leave(node.cluster.selfAddress)
    p.future
  }

  def main(args: Array[String]) {
    val fixedNodes = (0 to 20).map(i => node(BasePort + i))

    val sema = new Semaphore(Runtime.getRuntime.availableProcessors)
    val pool = Executors.newCachedThreadPool()

    val counter = new AtomicInteger

    while (true) {
      val count = counter.incrementAndGet()
      sema.acquire()
      pool.execute(new Runnable {
        override def run() {
          val n = node(0)
          Await.ready(clusterUp(n), Duration.Inf)
          println(s"$count is up")
          Await.ready(leave(n), Duration.Inf)
          n.system.shutdown()
          println(s"$count is shutdown")
          sema.release()
        }
      })
    }

  }

}
