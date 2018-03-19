
package models

import play.api.libs.json._
import play.api.libs.functional.syntax.unlift
case class UserSettings(
                        preferredServiceProfiles:Seq[String] = Seq.empty,
                        otherSettings: Option[JsObject] = None
                       )

object UserSettings {

 implicit val formats:Format[UserSettings] = (
    (__ \ "preferred-service-profiles").format[Seq[String]],
    (__ \ "other-settings").formatNullable[JsObject])(UserSettings.apply _)

  def convertToString(psp:Seq[String]):String = psp.reduce((a,b) => a + ";" + b)

}