package axisim.dag


import scala.io.Source
import scala.collection.mutable
import scala.collection.immutable
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._


import spinal.core._
import spinal.core.sim._
import spinal.lib.bus.amba4.axi._
import spinal.lib.sim.StreamMonitor


import axisim.{Axi4CheckerPrimary, Axi4Job, Axi4AWJob, Axi4WJob, Axi4ARJob, ChannelDriver}
import axisim.dag.json._
import axisim.dag.transaction._


object DirectAcyclicGraph {

  private def JsonNodeToAxi4Job(dag: DirectAcyclicGraph, node: GraphJsonNode): Axi4Job = {
    if (node.isRead) {
      return new Axi4ARJob(
        channel = dag.axi.ar,
        addr    = 0, // TODO: make random
        id      = 0, // TODO: make random
        len     = 4-1,
        size    = log2Up(dag.axi.ar.config.bytePerWord),
        delay   = node.delay
      )
    }
    else {
      return new Axi4AWJob(
        channel = dag.axi.aw,
        addr    = 0, // TODO: make random
        id      = 0, // TODO: make random
        len     = 4-1,
        size    = log2Up(dag.axi.aw.config.bytePerWord),
        delay   = node.delay
      )
    }
  }

  def apply(filename: String, axi: Axi4, clockDomain: ClockDomain): DirectAcyclicGraph = {
    val dag = DirectAcyclicGraph(axi, clockDomain)
    // Get file content
    val graphJson = decode[GraphJson](Source.fromFile(filename).mkString) match {
      case Right(graph) => graph
      case Left(error)  => throw new RuntimeException(error)
    }
    // Parse graph
    val map: immutable.Map[String, Axi4Job] = graphJson.nodes.map(n => n.id -> JsonNodeToAxi4Job(dag, n)).toMap
    dag.transactions = map.values.toSeq
    graphJson.edges.foreach { e =>
      (map(e.to)).addPrecedence(map(e.from), e.prec)
    }
    // find starting points
    for (transaction <- dag.transactions) {
      if (transaction.areAllPrecedencesSolved()) {
        dag.makeReady(transaction)
      }
    }
    // Done
    return dag
  }

}

case class DirectAcyclicGraph(axi: Axi4, clockDomain: ClockDomain) extends Axi4CheckerPrimary(axi, clockDomain) {

  var transactions = Seq.empty[Axi4Job]
  
  private def makeReady(transaction: Axi4Job): Unit = {
    transaction match {
      case read: Axi4ARJob => addRead(read)
      case write: Axi4AWJob => addWrite(write, new Axi4WJob(
        channel = axi.w,
        data    = Seq.fill(write.len+1)(BigInt(0)),
        strb    = Seq.fill(write.len+1)(BigInt(0)), // TODO; change
        parent  = write
      ))
    }
  }

  private def makePending[T <: Data](channel: ChannelDriver[T]): Unit = {
    if (channel.scheduled != null) {
      // Enqueue all precedence wainting for `transaction` to have been scheduled (i.e., that is now "pending")
      for (next <- channel.scheduled.pending) {
        next.prevs -= 1
        if (next.areAllPrecedencesSolved()) {
          makeReady(next)
        }
      }
    }
  }

  private def resolve(monitor: mutable.Map[Int, mutable.Queue[Axi4Job]], id: Int): Unit = {
    if (monitor(id).nonEmpty) {
      // Mark precedence as completed for all next steps
      for (next <- (monitor(id).front.served ++ monitor(id).front.pending)) {
        next.prevs -= 1
        if (next.areAllPrecedencesSolved()) {
          makeReady(next)
        }
      }
    }
  }

  StreamMonitor(axi.ar, clockDomain) { payload =>
    makePending(ARDriver)
  }

  StreamMonitor(axi.aw, clockDomain) { payload =>
    makePending(AWDriver)
  }

  StreamMonitor(axi.r, clockDomain) { payload =>
    if (payload.last.toBoolean) {
      resolve(RMonitor, payload.id.toInt)
    }
  }

  StreamMonitor(axi.b, clockDomain) { payload =>
    resolve(BMonitor, payload.id.toInt)
  }

}
