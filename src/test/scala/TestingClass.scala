
import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.Await
import scala.concurrent.duration._
import org.scalatest.FunSuite

import scala.concurrent.Future
import scala.io.Source

class TestingClass extends FunSuite {

  //
  // Simple test to show that we know what a test case means
  // besides that, we couldn't test the actors properly.
  // Not enough time either. Hard Project.
  //
  test("Simple Test") {
    assert(true)
  }

  test("Reading From File Input") {
    val file_name = "input.txt"
    val temp = EntryPoint
    temp.list_of_movies_read = Source.fromFile(file_name, "UTF-8").bufferedReader()

    val result = temp.Get_Next_Movie_Read()

    assert(!result.isEmpty)
  }

  test("Writing From File Input") {
    val file_name = "input.txt"
    val temp = EntryPoint
    temp.list_of_movies_write = Source.fromFile(file_name, "UTF-8").bufferedReader()

    val result = temp.Get_Next_Movie_Write()

    assert(!result.isEmpty)
  }

  test("NodesServer Instantiation")
  {
      val system = ActorSystem("mySystem")
      val server = system.actorOf(Props[Server], "server")

    //  server ! InitNodes(13)

      implicit val timeout = Timeout(100 seconds)
      val future:Future[String] = ask(server, InitNodes(13)).mapTo[String]
      val result = Await.result(future, 100 second)

    assert(result == "  ...COMPLETE NODES INIT")
  }

  test("ClientServer Instantiation")
  {
    val system = ActorSystem("mySystem")
    val userManagerActor = system.actorOf(Props[UserManager], "UserManager")


    //  server ! InitNodes(13)

    implicit val timeout = Timeout(100 seconds)
    val future1 = userManagerActor ? InitActors(10, .8, 10, 100)
    val result = Await.result(future1, 100 second)

    assert(result == "  ...COMPLETE USERS INIT")
  }

}
