import $ivy.{
  `org.scalaj::scalaj-http:2.3.0`,
  `com.twitter::finatra-http:18.3.0`,
  `ch.qos.logback:logback-classic:1.2.3`,
  `io.circe::circe-core:0.9.3`,
  `io.circe::circe-parser:0.9.3`,
  `io.circe::circe-generic:0.9.3`
}
import $file.reader
import reader.conf

import com.twitter.finatra.http._
import javax.inject.Singleton
import com.twitter.finagle.http._
import scalaj.http.{Http, HttpRequest, MultiPart}
import com.twitter.inject.Logging
import com.twitter.finatra.utils.FuturePools
import com.twitter.finatra.http.request.RequestUtils
import io.circe._, io.circe.parser._
import io.circe.generic.auto._
import javax.inject.Inject
import com.twitter.finatra.http.fileupload._
import com.twitter.finagle.http._

object definitions {

  case class OuvertureDossier(codeAcprAssureurDO: String, refSinistreDO: String, refInterneExpert: String, dateOuvertureChantier: String)
}

// TODO: could be done more easily: take request and map it directly (url, parameters, headers), 
// and add headers, token...
class ProxyController extends Controller with Logging {

    private val futurePool = FuturePools.unboundedPool("CallbackConverter")
    private val conf: reader.Conf = reader.conf

    private def getToken() : String = conf.authentication("bearer")
    private def getBaseurl() : String = conf.baseurl
    private def getAuthentication(): Map[String, String] = conf.authentication
    private def getHeader(): Map[String, String] = conf.header

  /*
   * Build request sent to remote server
   */
  private def buildServerRequest(relativePath: String) : HttpRequest = {

    val url = if (relativePath.length > 0) getBaseurl + "/" + relativePath else getBaseurl
    Http(url).
    headers(Seq("Authorization" -> ("Bearer " + getToken))).
    headers(Seq("X-Api-Version" -> "123", "X-Canal" -> "456"))
  }

  get("/list") { request: Request =>
    futurePool {
      val req: HttpRequest = buildServerRequest("dossiers")
      req.asString.body
    }
  }
  get("/dossiers/:refDossier") { request: Request =>
    val refDossier = request.params("refDossier")
    val req: HttpRequest = buildServerRequest(s"dossiers/$refDossier")
    futurePool {
      req.asString.body
    }
  }
  //java.lang.ClassNotFoundException: ammonite.
  //post("/ouvrir") { opReq: definitions.OuvertureDossier => 
  //  debug(opReq)
  //}
  post("/ouvrir") { request: Request =>
    parse(request.contentString) match {
      case Left(failure) => "Invalid Json"
      case Right(json) => 
        val ouvertureDossier = decode[definitions.OuvertureDossier](json.toString) 
        ouvertureDossier match {
          case Left(failure) => "Missing values"
          case Right(ouvertureDossier) => 
            futurePool {
              val req : HttpRequest = buildServerRequest("dossiers").postForm(
                // TODO: use circe generic
                Seq(
                  "codeAcprAssureurDO" -> ouvertureDossier.codeAcprAssureurDO,
                  "refSinistreDO" -> ouvertureDossier.refSinistreDO,
                  "refInterneExpert" -> ouvertureDossier.refInterneExpert,
                  "dateOuvertureChantier" -> ouvertureDossier.dateOuvertureChantier
                ))
              req.asString.body
            }
        }
    }
  }

  // enregistrer piece jointe
  post("/dossiers/:refDossier/documents") { request: Request =>
    val refDossier = request.params("refDossier")
    //debug(request.contentType)
    futurePool {
      val lMultiPart = RequestUtils.multiParams(request).map {
        case (key, MultipartItem(data, fieldName, isFormField, Some(contentType), Some(filename), headers)) =>
          // key == 'file' or 'file1', 'file2"
          // pdf content type == application/octet-stream !
          val cType = if (filename.reverse.split("\\.").reverse.equals("pdf")) "application/pdf" else contentType
          MultiPart(key, filename, "application/pdf", data)
        case _ => throw new Exception("invalid file")
      }
      buildServerRequest(s"dossiers/$refDossier/documents").postMulti(lMultiPart.toSeq : _* ).
      asString.body
    }
  }
  get("/dossiers/:refDossier/documents/:refPieceJointe") { request: Request =>
    val refDossier = request.params("refDossier")
    val refPieceJointe = request.params("refPieceJointe")
    futurePool {
      buildServerRequest(s"dossiers/$refDossier/documents/$refPieceJointe").
      header("content-type", "application/pdf").
      asBytes.body
    }
  }
}

// https://twitter.github.io/finatra/user-guide/http/exceptions.html#default-exception-mappers
import com.twitter.finatra.http.exceptions.ExceptionMapper
import java.net.MalformedURLException
import com.twitter.finatra.http.response.ResponseBuilder

@Singleton
class MalformedURLExceptionMapper @Inject()(response: ResponseBuilder)
  extends ExceptionMapper[MalformedURLException] {

  override def toResponse(request: Request, exception: MalformedURLException): Response = {
    response.badRequest(s"Malformed URL - ${exception.getMessage}")
  }
}

import com.twitter.finatra.http.routing.HttpRouter;
import com.twitter.finatra.http.{Controller, HttpServer}

// https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/filters/CommonFilters.scala
import com.twitter.finatra.http.filters.CommonFilters

class ProxyServer extends HttpServer {

  override val defaultFinatraHttpPort: String = ":8080"

  @Override
  def configureHttp(httpRouter: HttpRouter) {
    httpRouter
  .filter[CommonFilters]
  .add[ProxyController]
  .exceptionMapper[MalformedURLExceptionMapper]
  }
}

(new ProxyServer).main(Array[String]())
