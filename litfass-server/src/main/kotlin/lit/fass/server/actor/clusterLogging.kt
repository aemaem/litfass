package lit.fass.server.actor

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.Behaviors.*
import akka.cluster.ClusterEvent.*
import akka.cluster.typed.Cluster
import akka.cluster.typed.Subscribe


fun clusterEventLoggingBehavior(cluster: Cluster): Behavior<ClusterDomainEvent> {
    return setup { context ->
        context.log.info("Started cluster {} with address {}", context.system, cluster.selfMember().address())
        cluster.subscriptions().tell(Subscribe(context.self, ClusterDomainEvent::class.java))
        receive(ClusterDomainEvent::class.java)
            .onMessage(MemberEvent::class.java) {
                context.log.info(it.toString())
                same()
            }
            .onMessage(UnreachableMember::class.java) {
                context.log.info(it.toString())
                same()
            }
            .onAnyMessage {
                context.log.debug(it.toString())
                same()
            }
            .build()
    }
}
