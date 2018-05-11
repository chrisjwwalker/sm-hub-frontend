/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import play.api.libs.json._

case class Sources(cmd: Option[String], extra_params: Option[String])

case class Binary(groupId: String, nexus: String, cmd: String)

case class ConfigSetup(name: String,
                       template: String,
                       defaultPort: Int,
                       hasMongo: Boolean,
                       sources: Sources,
                       binary: Binary)

object ConfigSetup {

  val writes: String => Writes[ConfigSetup] = githubOrg => new Writes[ConfigSetup] {
    def writes(configSetup: ConfigSetup): JsValue = {
      val serviceId                    = configSetup.name.toUpperCase.replace(" ", "_")
      val flattenedName                = configSetup.name.replace(" ", "-").toLowerCase()
      val sourcesCmdList: List[String] = configSetup.sources.cmd.getOrElse("").split(" ").toList.filterNot(_.equals(""))
      val sourcesEPList: List[String]  = List(
        if(configSetup.hasMongo) s"-DProd.mongodb.uri=mongodb://localhost:27017/$flattenedName" else ""
      ).filterNot(_.equals("")) ++ configSetup.sources.extra_params.getOrElse("").split(" ").toList.filterNot(_.equals(""))
      val binaryCmdList: List[String]  = List(
        s"./$flattenedName/bin/$flattenedName",
        if(configSetup.hasMongo) s"-DProd.mongodb.uri=mongodb://localhost:27017/$flattenedName" else ""
      ).filterNot(_.equals("")) ++ configSetup.binary.cmd.split(" ").toList

      def sourcesObj: JsObject = {
        val cmd  = if(sourcesCmdList.nonEmpty) Json.obj("cmd" -> Json.toJson(sourcesCmdList)) else Json.obj()
        val ep   = if(sourcesEPList.nonEmpty) Json.obj("cmd" -> Json.toJson(sourcesEPList)) else Json.obj()
        val repo = Json.obj("repo" -> s"git@github.com/$githubOrg/$flattenedName")
        cmd ++ ep ++ repo
      }

      Json.obj(
        serviceId -> Json.obj(
          "name"               -> configSetup.name,
          "template"           -> configSetup.template,
          "location"           -> s"/$flattenedName",
          "defaultPort"        -> configSetup.defaultPort,
          "hasMongo"           -> configSetup.hasMongo,
          "sources" -> sourcesObj,
          "binary" -> Json.obj(
            "artifact"          -> s"${flattenedName}_2.11",
            "groupId"           -> configSetup.binary.groupId,
            "nexus"             -> configSetup.binary.nexus,
            "destinationSubdir" -> flattenedName,
            "cmd"               -> Json.toJson(binaryCmdList)
          )
        )
      )
    }
  }
}
