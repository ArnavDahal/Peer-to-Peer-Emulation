
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.actor._

import scala.concurrent.{Await, Future}

case class getMax()

case class getMin()

case class assignMax(value: Long) {}

case class assignMin(value: Long) {}

case class assignNextNode(next: ActorRef, min: Long, max: Long) {}

case class InitNodes(numNodes: Int)

case class PutKeyValue(key: String, value: String)

case class PutKeyValue2(key: Long, value: String)

case class GetKeyValue(key: String, asker: ActorRef)

case class GetKeyValue2(key: Long, asker: ActorRef)

case class GetKeyValueReturn(kv: kvPair)


//
// Server Actor: In other words its the Node Manager.
//
class Server extends Actor {

  // The Max range that can be represented by this implementation
  var maxHashRange: Long = 99999999999L

  // Ring of nodes, stored in an array for easy access, but
  // each node has its own fingertable that constructs all the
  // nodes into a ring for the chord protocol
  var Nodes: Array[ActorRef] = null

  // counter for naming the actors
  var counter = 0

  // simple method that names the actors accordingly
  def getActorName(): String = {

    counter = counter + 1
    "NodeActor_" + counter.toString
  }

  // receive method override for Akka Actor implementation
  override def receive = {
    // Case Class to Initialize all the nodes.
    case InitNodes(numNodes: Int) => {

      // fill up the Ring with Node Actors
      Nodes = Array.fill[ActorRef](numNodes)(context.system.actorOf(Props(new myNode(context.self)), getActorName()))

      // determine distribution of the node ranges
      val distribution: Long = maxHashRange / numNodes
      var holder = 0L

      //
      // assign ranges
      //
      for (i <- 0 to (numNodes - 1)) {

        // determine the next actor to pass in. easier for the fingertable
        // if assigning ranges to the last one then send in the first node
        var nextNode: ActorRef = null
        if ((i + 1) >= numNodes)
          nextNode = Nodes {
            0
          }
        else
          nextNode = Nodes {
            i + 1
          }

        // send the message to assign the node.
        Nodes {
          i
        } ! assignNextNode(nextNode, (holder + distribution + 1L), (holder + (2 * distribution)))

        // send the message to assign max
        Nodes {
          i
        } ! assignMax(holder + distribution)

        // send the message to assign min
        Nodes {
          i
        } ! assignMin(holder)

        // increment the holder
        holder += distribution + 1L

      }

      // return a string to the sender to notify the future of completion
      sender ! "  ...COMPLETE NODES INIT"

    }
    // Case Class to put key and value, this will be called by write request
    // by the client users
    case PutKeyValue(key: String, value: String) => {

      // check the key to see if it is withing range
      if (key.toLong > maxHashRange) {
        EntryPoint.logger.info("The Key: \"" + key + "\" exceeds the max hash limit allowed.")
      }
      // if it is then pass the message to the node to place the value accordingly
      // on to the RING
      else {
        Nodes.head ! PutKeyValue2(key.toLong, value)
      }

      // respond so the future knows whats up
      sender ! ""
    }
    // Case Class to get value based on specified key
    case GetKeyValue(key: String, asker: ActorRef) => {
      // check the key to see if it is withing range
      if (key.toLong > maxHashRange) {
        //        println("The Key: \"" + key + "\" exceeds the max hash limit allowed.")
        EntryPoint.logger.info("The Key: \"" + key + "\" exceeds the max hash limit allowed.")
      }
      // if it is then pass the message to the node to place the value accordingly
      // on to the RING
      else {
        Nodes.head ! GetKeyValue2(key.toLong, asker)
      }
    }
  }

}

//
// FingerTableTuple : This is the tuple that every index
// in the fingertable will be consisted of.
//    - ActorRef : pretty much a pointer to another node on the ring
//    - Int : Minimum range of the actor node above
//    - Int : Maximum range of the actor node above
//
class FingerTableTuple(pNode: ActorRef, pMin: Long, pMax: Long) {

  // pretty much a pointer to another node on the ring
  var node = pNode
  // Minimum range of the actor node above
  var min = pMin
  // Maximum range of the actor node above
  var max = pMax

}

//
// myNode class is the simulation of a computer node in the
// chord algorithm protocol
//
class myNode(server: ActorRef) extends Actor {

  // saves an of the server and the nextactor in the ring
  val serverInstance: ActorRef = server
  var nextNode: ActorRef = _

  // this nodes ranges for storing the hash key
  var maxValue: Long = 0
  var minValue: Long = 0

  // the actual data or the values that will be stored in this node
  var listValues = List[kvPair]()

  // FingerTableList for this node computer in the ring
  var fingerTable = List[FingerTableTuple]()

  override def receive = {
    case getMin() => {
      sender ! minValue.toString
    }
    case getMax() => {
      sender ! maxValue.toString
    }
    case assignMax(value: Long) => {
      maxValue = value
    }
    case assignMin(value: Long) => {
      minValue = value
    }
    case assignNextNode(next: ActorRef, min: Long, max: Long) => {
      // assigne next node as well as save it in the fingertable
      nextNode = next
      fingerTable = new FingerTableTuple(next, min, max) :: fingerTable
    }
    case PutKeyValue2(key: Long, value: String) => {

      // check if the requested key is within range
      if (key >= minValue && key <= maxValue) {
        EntryPoint.logger.info("Putting Key: " + key + "with value" + value + " into: " + context.self.path.toString)
        //        println("Putting Key: " + key + "with value" + value + " into: " + context.self.path.toString)
        listValues = new kvPair(key, value) :: listValues
      }
      // if it is not within range then pass the value along to the next node
      // according to the chord implementation. The actors communicate via messages
      else {
        nextNode ! PutKeyValue2(key, value)
      }
    }
    case GetKeyValue2(key: Long, asker: ActorRef) => {

      // if the key is within this nodes range
      if (key >= minValue && key <= maxValue) {
        var b = true
        for (i <- listValues) {
          if (i.Key == key) {
            EntryPoint.logger.info("Inside of " + context.self.path.toString + " Sending back the kvpair to parent " +
              "Key " + i.Key + " with value " + i.Value)
            //            println("Inside of " + context.self.path.toString + " Sending back the kvpair to parent " +
            //              "Key " + i.Key + " with value " + i.Value)
            asker ! GetKeyValueReturn(i)
            b = false
          }
        }
        if (b) {
          EntryPoint.logger.info("Inside of " + context.self.path.toString + " Sending back an EMPTY kvpair to parent, could not find key. ")
          //          println("Inside of " + context.self.path.toString + " Sending back an EMPTY kvpair to parent, could not find key. ")
          asker ! GetKeyValueReturn(new kvPair(key))
        }
      }
      else {
        nextNode ! GetKeyValue2(key, asker)
      }
    }
  }
}


//
// kvPair Tuple-like class to hold the key/value pairs
//
class kvPair(val k: Long = 0L, val v: String = "Not Found") {
  var Key = k
  var Value = v

  override def toString: String = {
    v
  }

}

//object ServerManager extends App {
//
//  val system = ActorSystem("mySystem")
//  val server = system.actorOf(Props[Server], "server")
//
////  server ! InitNodes(13)
//
//  implicit val timeout = Timeout(100 seconds)
//  val future:Future[String] = ask(server, InitNodes(13)).mapTo[String]
//  val result = Await.result(future, 100 second)
//
//  println("Completed Init")
//
//  server ! PutKeyValue("9016219780", "Nagin Ke Do Dushman")
//
//  println("Completed Put")
//
//  server ! GetKeyValue("9016219780", "id")
//  println("Completed Get")
//
//
//}
