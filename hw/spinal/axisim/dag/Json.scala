package axisim.dag.json


import io.circe._
import io.circe.generic.auto._
import io.circe.parser._



case class GraphJsonNode(id: String, delay: Int, isRead: Boolean = true) {}


case class GraphJsonEdge(from: String, to: String, prec: String) {}


case class GraphJson (nodes: List[GraphJsonNode], edges: List[GraphJsonEdge]) {}
