import $ivy.{
  `com.fasterxml.jackson.module::jackson-module-scala:2.9.4`,
  `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.4`,
  `com.fasterxml.jackson.core:jackson-databind:2.9.4`,
  `com.fasterxml.jackson.core:jackson-core:2.9.4`
}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import scala.collection.immutable.HashMap
import scala.collection.JavaConverters._
import java.io.File
import scala.io.Source

// uses Jackson YAML to parsing, relies on SnakeYAML for low level handling
val mapper: ObjectMapper = new ObjectMapper(new YAMLFactory())

// provides all of the Scala goodiness
mapper.registerModule(DefaultScalaModule)

object Conf {
  def apply(baseurl: String, 
    _authentication: Map[String, String], 
    header: Map[String, String],
    params: Map[String, String]): Conf = {
 
    val authentication = _authentication.map { 
      case (k,v) => k -> {
        k match {
          case "bearer" => resolve(v) 
          case _  => v
        }
      }
    }.toMap
    new Conf(baseurl, authentication, header, params)
  }

  def apply(conf: Conf): Conf = {
    apply(conf.baseurl, conf.authentication, conf.header, conf.params)
  }

 /*
  * @param data
  * @return if data is like $name, return the content of the file under conf with the filename 'name'
  *         else data itself.
  */
  private def resolve(data: String): String = {
   if (data == null || data.length == 0) {
     return ""
   }
   if (data(0) == '$') 
     Source.fromFile("conf/" + data.substring(1)).getLines.toList.mkString("\r\n")
   else
     data
  }
}

class Conf(val baseurl: String, 
  val authentication: Map[String, String], 
  val header: Map[String, String],
  val params: Map[String, String]) {

  private def flatten(l: List[Any]) : List[Any] = l.flatMap {
    case ls: List[_] => flatten(ls)
    case el => List(el)
  }

  override def toString = flatten(List(
    "*** baseurl ***",
    s"baseurl \t $baseurl",
    "*** authentication ***",
    authentication.map { case (k, v) => k + "\t" + v }.toList,
    "*** header ***",
    header.map { case (k, v) => k + "\t" + v }.toList,
    " ***********"
  )).mkString("\r\n")

}

val conf: Conf = Conf(mapper.readValue(new File("conf/conf.yaml"), classOf[Conf]))
