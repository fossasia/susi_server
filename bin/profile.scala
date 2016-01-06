import scalaj.http._
import scala.collection.JavaConversions._
import java.io.File
import scala.io.Source
case class HttpException(msg: String) extends Throwable

/*
In order to use this profiling script, you should use install the latest version of scala and sbt(basically sbt will install everything for you).
The following command may help you to get it run
Problem: tailing white space in file may cause the script to crash. Problem is on line 74. getlines() function will fail if there is a tailing whitespace.
For simplicity and readability, this issue is still in the program. Tailing white space should not appear in the query files anyway.

cd bin
sbt run
*/
object util{
  //get current dic
  def getCurrentDirectory = new java.io.File( "." ).getCanonicalPath

  /*
  This is a timer for timing
  */
  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) + "ns")
    result
}
  /**
  *
  * @param url url address
  * @param cookies:List of cookie pairs: (String,String)
  * @return
  */
  def get(url:String, cookies: List[(String,String)]): String ={
    val request = Http(url)
    cookies.foreach((tuple_string) => request.cookie(tuple_string._1, tuple_string._2)) //insert cookies
    request.header("User-Agent","Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2")
    val response = try{
      request.asString
    } catch {
      case _:Throwable => throw new HttpException("error in get data and translate to String, maybe it's bad connection")
    }

    val reg_200 = """.*(200).*""".r
    if(response.isSuccess && reg_200.findFirstIn(response.statusLine).isDefined){
      try{
        response.body
      }catch{
        case throwable: Throwable => throw new HttpException("fail to get the content")
      }
    }
    else{
      throw new HttpException("bad request" + response.statusLine)
    }
  }
}
//main func
object profile extends App{
  //two
  val main_dic_r = """(.*)/bin""".r
  val queries_r = """(.*\d\d.txt)""".r
  val main_dic_r(main_dic) = util.getCurrentDirectory
  val test_dic = main_dic + "/test/queries"
  //get the absolute path of all query files
  val file_list = new java.io.File(test_dic).listFiles.map{
    file => file.getName match{
      case queries_r(name) => test_dic + "/" + name
      case _ => null
    }
  }.filterNot(_==null)
  //
  val all_queries = file_list.map(fileName => Source.fromFile(fileName).getLines().toList).flatten
  println("Total queries count:" + all_queries.length)
  println("Start firing queries to localhost...")
  //don't change it to remote server, remote server may blacklist you
  val baseURL = "http://localhost:9000/api/search.json?q="
  //a util function for make queries, return 0 if failed, and 1 if succeed
  def make_query(word:String):Int = {
    //for accuracy, you should remove the line below
    println("Query:" + word)
    try{
      util.get(baseURL+word, Nil)
      1
    }catch{
      case _:Throwable => 0
    }
  }
  //time is just a genetic stop watch wrapper, one of the advantage of scala.
  //code is parallized to saturate the I/O, move out for accuracy
  val par_queries = all_queries.par
  val parlevel = 15 //parallization level(number of threads), don't set it too high. Higher number will give higher pressure to the server.
  import scala.collection.parallel._
  par_queries.tasksupport = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(parlevel))
  val good_unsumed = util.time{
    par_queries.map(make_query)
  }
  //move out for accuracy
  val good = good_unsumed.toList.sum
  println("Succeeded:" + good.toString)
  println("Failed:" + (all_queries.length - good).toString)
}
