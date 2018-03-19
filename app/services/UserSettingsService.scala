
package services

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import models.UserSettings
import play.api.{Environment, Logger}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContent, Request}

import scala.io.Source
import UserSettings._

class UserSettingsService(val environment: Environment) {

  def getPreferredServicesFromRequest(r:Request[AnyContent]):UserSettings = {
    r.session.get("preferred-services").map(services =>
    services.split(";").toSeq).fold(UserSettings())(profiles => UserSettings(profiles))
  }

  def addPreferredServicesToRequest(r:Request[AnyContent], userSettings: UserSettings)  = {
    r.session - ("preferred-services") +
      ("preferred-services" -> convertToString(userSettings.preferredServiceProfiles))
  }


  def getSettingsFromFile:Option[UserSettings] = {
    val bufferedSource = Source.fromFile(environment.getFile("conf/userSettings.json"))
    val fileContents = bufferedSource.getLines.mkString
    bufferedSource.close

    val json = Json.parse(fileContents).as[JsObject]
    Json.fromJson[UserSettings](json).asOpt
  }

  def writeSettingsToFile(json:JsObject):Option[Boolean] =
    try {
      Files.write(Paths.get("conf/userSettings.json"), json.as[String].getBytes(StandardCharsets.UTF_8))
      Some(true)
    } catch {
      case e:Exception =>{ Logger.info(s"[writeSettingsToFile] failed to write to conf/userSettings with error: ## $e")
        None
      }
    }



}
