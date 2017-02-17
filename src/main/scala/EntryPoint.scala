
import java.io.{BufferedReader}


import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import grizzled.slf4j.Logger

import scala.concurrent.Await

import scala.concurrent.duration._
import scala.io.{Source, StdIn}
import scala.language.postfixOps

object EntryPoint {

  //all the inputs to the program as per the spec
  var num_users, duration_in_minutes, min, max, num_computers,
  time_mark, num_readers, num_writers: Int = _

  var ratio: Double = _
  var file_name: String = _
  var inputArray: List[String] = _

  var list_of_movies_read: BufferedReader = _
  var list_of_movies_write: BufferedReader = _

  var userManagerActor: ActorRef = _
  var server: ActorRef = _

  val logger = Logger("log")

  def Get_Next_Movie_Read(): String = {
    val curr = list_of_movies_read.readLine()

    if (curr != null)
      curr
    else
      "NO MORE MOVIES"
  }

  def Get_Next_Movie_Write(): String = {
    val curr = list_of_movies_write.readLine()

    if (curr != null)
      curr
    else
      "NO MORE MOVIES"
  }

  def main(args: Array[String]): Unit = {

    //region GetAllUserInput
    print("Enter the duration of the simulation (in minutes) : ")
    duration_in_minutes = StdIn.readInt()

    print("Enter the # of users : ")
    num_users = StdIn.readInt()

    print("Enter the MINIMUM number of requests each user actor can send every minute : ")
    min = StdIn.readInt()

    print("Enter the MAXIMUM number of requests each user actor can send every minute : ")
    max = StdIn.readInt()

    print("Enter the # of computers : ")
    num_computers = StdIn.readInt()

    print("Enter the name of the file that contains Key/Value pairs : ")
    file_name = StdIn.readLine()

    list_of_movies_read = Source.fromFile(file_name, "UTF-8").bufferedReader()
    list_of_movies_write = Source.fromFile(file_name, "UTF-8").bufferedReader()

    print("Enter the ratio of Write/Read requests <= 1.0 (as DECIMAL) : ")
    ratio = StdIn.readDouble()

    print("Enter the time mark (elapsed time in SECONDS) of when to pause the system and dump state as an XML : ")
    time_mark = StdIn.readInt()

    num_readers = (num_users * ratio).toInt
    num_writers = num_users - num_readers

    //endregion

    val system = ActorSystem("ClientSystem")
    userManagerActor = system.actorOf(Props[UserManager], "UserManager")
    server = system.actorOf(Props[Server], "server")


    implicit val timeout = Timeout(100 seconds)

    val future1 = userManagerActor ? InitActors(num_users, ratio, min, max)
    logger.info(Await.result(future1, 100 second))

    val future2 = server ? InitNodes(num_computers)
    logger.info(Await.result(future2, 100 second))

    //
    // Start simulation:  Cycle through each user and perform a certain task (read/write),
    //                    triggering the appropriate actors in the server
    //
    userManagerActor ! StartRequests(max)
  }
}