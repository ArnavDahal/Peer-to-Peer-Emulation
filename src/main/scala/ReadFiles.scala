

import java.io.{BufferedWriter, File, FileWriter}
import java.security.MessageDigest

import scala.io.Source
import scala.util.Random.nextInt
import scala.util.matching.Regex
import util.MurmurHash._

object ReadFiles {


  def main(args: Array[String]): Unit = {
    parseMovies()
  }

  def parseMovies(): Unit = {
    // Counter for how far its gone
    var counter = 0
    // How many movies to grab
    val max = 2000

    // Theres 1,291,805 movies in the list
    val randomStart = nextInt(1290000)
    // Counter end point
    val randomEnd = randomStart + max

    // Filename of movies list
    val filename = "movies.txt"

    // To find the movies
    val pattern = new Regex(".* \\(.{4}\\)")

    val file = new File("input.txt")
    val bw = new BufferedWriter(new FileWriter(file))

    try {


      println("Range: " + randomStart + " - " + randomEnd)
      println("Parsing movies....")

      // Iso is needed for encoding type
      for (line <- Source.fromFile(filename, "iso-8859-1").getLines()) {

        if (counter > randomEnd)
          return

        if (counter > randomStart) {
          val foundString: String = (pattern findFirstIn line).mkString("")

          if (foundString != "") {
            val movie = foundString.dropRight(7)
            val year = foundString.takeRight(5).dropRight(1)
            val x = stringHash(year)
            val y = Integer.toUnsignedLong(x)

            bw.write(y + " " + movie + "\n")
          }
        }

        counter += 1

      }
    }
    finally {
      bw.close()
      println("Finished Parsing Movies")
      EntryPoint.logger.info("Finished Parsing Movies")

    }
  }

  // Converts the input string to a md5 hash and returns it as a string
  def md5Hash(text: String): String =
  MessageDigest.getInstance("MD5").digest(text.getBytes()).map(0xFF & _).map {
    "%02x".format(_)
  }.foldLeft("") {
    _ + _
  }


  // Opens the input.txt file and reads the values
  def getHashedFile(): Unit = {
    val filename = "input.txt"
    for (line <- Source.fromFile(filename).getLines()) {
      //println(line)

      // Split on each comma.
      val result = line.split('\t')
      val key = result {
        0
      }
      val value = result {
        1
      }

      println("Key: " + key + "\t Value: " + value)

      // Actor<Number>.write(key,value)


    }
  }
}



