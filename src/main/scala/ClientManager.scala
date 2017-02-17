
import akka.actor.{Actor, ActorRef, PoisonPill, Props}

import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await

case class assign_values(ratio: String, min: Int, max: Int)

case class InitActors(num_users: Int, ratio: Double, min: Int, max: Int)

case class StartRequests(max: Int)

case class StartRequests2()

case class EndSimulation()

class UserManager extends Actor {

  var user_array: Array[ActorRef] = _

  def receive = {
    case InitActors(num_users: Int, ratio: Double, min: Int, max: Int) => {
      //      println("**INITIALIZING ACTORS AND ASSIGNING ROLES**")
      EntryPoint.logger.info("**INITIALIZING ACTORS AND ASSIGNING ROLES**")

      user_array = Array.fill[ActorRef](num_users)(context.system.actorOf(Props(new User()), getActorName))

      var how_many_writes = ratio * num_users
      var how_many_reads = num_users - how_many_writes

      //sanity check
      assert(how_many_reads + how_many_writes == num_users)

      for (i <- user_array) {
        if (how_many_writes > 0) {
          i ! assign_values("WRITE", min, max)
          how_many_writes -= 1
        }
        else if (how_many_reads > 0) {
          i ! assign_values("READ", min, max)
          how_many_reads -= 1
        }
      }

      sender ! "  ...COMPLETE USERS INIT"
    }
    case StartRequests(max: Int) => {
      for (count <- 0 until max) {
        for (i <- user_array) {
          i ! StartRequests2
        }
      }
    }
    case EndSimulation() => {
      for (i <- user_array)
        i ! PoisonPill
    }
    case _ => {
      println("Something unexpected received in ClientManager.scala")
      EntryPoint.logger.info("Something unexpected received in ClientManager.scala")
    }
  }

  var counter = 0

  def getActorName: String = {
    counter = counter + 1
    "UsersActor" + counter.toString
  }
}

class User() extends Actor {
  var min_requests: Int = _
  var max_requests: Int = _
  var type_of_request: String = _

  override def receive: Receive = {
    case assign_values(task: String, min: Int, max: Int) => {
      type_of_request = task
      min_requests = min
      max_requests = max
    }
    case StartRequests2 => {
      if (type_of_request == "READ") {
        val key_to_read = EntryPoint.Get_Next_Movie_Read()

        if (key_to_read != "NO MORE MOVIES") {
          //then store it
          //parse into key and value
          val x: Array[String] = key_to_read.split(" ")

          val key = x {
            0
          }
          EntryPoint.server ! GetKeyValue(key, context.self)
        }
      }

      else if (type_of_request == "WRITE") {
        val movie_to_store = EntryPoint.Get_Next_Movie_Write()

        if (movie_to_store != "NO MORE MOVIES") {
          //then store it
          //parse into key and value
          val x: Array[String] = movie_to_store.split(" ")

          val key = x {
            0
          }
          val value = movie_to_store.substring(key.size)

          EntryPoint.logger.info("Got key: " + key + " | value: " + value)

          implicit val timeout = Timeout(100 seconds)
          val future1 = EntryPoint.server ? PutKeyValue(key, value)
          val result1 = Await.result(future1, 100 second)
        }
      }
    }
    case GetKeyValueReturn(kv: kvPair) => {
      EntryPoint.logger.info(context.self.path.toString + " Received request for key: " + kv.Key + " with value: " + kv.Value)
    }
  }
}

