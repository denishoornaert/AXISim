package axisim.dag.transaction


import scala.collection.mutable


abstract class Transaction() {

  def isRead(): Boolean = {
    return true
  }

}


class LiveTransaction(val id: Int, var age: Int) extends Transaction() {

  def makeOlder() {
    if (age != 0) { 
      age -= 1
    }
  }

  def isReady(): Boolean = {
    return age == 0
  }
  
}


class StillTransaction(val delay: Int) extends Transaction() {


}
