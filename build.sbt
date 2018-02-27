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

val appName = "service-manager-support-frontend"

lazy val frontend = Project(appName, file("."))
  .enablePlugins(PlayScala)
  .settings(PlayKeys.playDefaultPort := 9981)
  .settings(
    scalaVersion        :=  "2.11.11",
    resolvers           +=  "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
    libraryDependencies ++= Seq(
      ws,
      "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1"  % Test,
      "org.jsoup"              %  "jsoup"              % "1.11.1" % Test,
      "org.mockito"            %  "mockito-core"       % "2.13.0" % Test
    )
  )
