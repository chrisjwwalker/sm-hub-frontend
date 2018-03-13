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

package controllers

import javax.inject.Inject

import forms.{AllProfilesForm, AllServiceForm, AvailablePortsForm, RunningServicesForm}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Controller}
import services.SMService
import views.html._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DefaultMainController @Inject()(val smService: SMService) extends MainController

trait MainController extends Controller {

  val smService: SMService

  def home(profile: String): Action[AnyContent] = Action { implicit request =>
    val apps    = smService.getRunningServices(profile)
    Ok(HomeView(apps, RunningServicesForm.form.fill(profile)))
  }

  def submitHome(): Action[AnyContent] = Action { implicit request =>
    RunningServicesForm.form.bindFromRequest.fold(
      errors => BadRequest(HomeView(smService.getRunningServices(), errors)),
      valid  => Redirect(routes.MainController.home(valid))
    )
  }

  def availablePorts(): Action[AnyContent] = Action { implicit request =>
    val ports = smService.getValidPortNumbers(None)
    Ok(PortsView(ports, AvailablePortsForm.form))
  }

  def submitAvailablePorts(): Action[AnyContent] = Action { implicit request =>
    AvailablePortsForm.form.bindFromRequest.fold(
      errors => BadRequest(PortsView(smService.getValidPortNumbers(None), errors)),
      valid  => {
        val ports = smService.getValidPortNumbers(Some(valid))
        Ok(PortsView(ports, AvailablePortsForm.form.fill(valid)))
      }
    )
  }

  def currentProfiles(): Action[AnyContent] = Action { implicit request =>
    val profiles = smService.getAllProfiles
    Ok(ProfilesView(profiles, AllProfilesForm.form))
  }

  def submitCurrentProfiles(): Action[AnyContent] = Action { implicit request =>
    AllProfilesForm.form.bindFromRequest.fold(
      errors => BadRequest(ProfilesView(smService.getAllProfiles, errors)),
      valid  => Ok(ProfilesView(smService.searchForProfile(valid), AllProfilesForm.form.fill(valid)))
    )
  }

  def currentServices(): Action[AnyContent] = Action { implicit request =>
    val services = smService.getAllServices
    Ok(ServicesView(services, AllServiceForm.form))
  }

  def submitCurrentServices(): Action[AnyContent] = Action { implicit request =>
    AllServiceForm.form.bindFromRequest.fold(
      errors => BadRequest(ServicesView(smService.getAllServices, errors)),
      valid  => Ok(ServicesView(smService.searchForService(valid), AllServiceForm.form.fill(valid)))
    )
  }

  def servicesInProfile(profile: String): Action[AnyContent] = Action { implicit request =>
    val services = smService.getServicesInProfile(profile)
    Ok(ServicesInProfileView(profile, services))
  }

  def detailsForService(service: String): Action[AnyContent] = Action { implicit request =>
    val serviceDetails = smService.getDetailsForService(service)
    Ok(ServiceDetailsView(serviceDetails))
  }

  def potentialConflicts(): Action[AnyContent] = Action { implicit request =>
    val potentialConflicts = smService.getDuplicatePorts
    Ok(PotentialConflictsView(potentialConflicts))
  }

  def serviceTestRoutes(): Action[AnyContent] = Action { implicit request =>
    val servicesWithTestRoutes = smService.getServicesWithDefinedTestRoutes
    Ok(ServiceTestRoutesView(servicesWithTestRoutes))
  }

  def serviceTestRoutesExpanded(service: String): Action[AnyContent] = Action { implicit request =>
    val serviceTestRoutes = smService.getServicesTestRoutes(service)
    Ok(ServiceTestRoutesExpandedView(service, serviceTestRoutes))
  }

  def availableAssetsVersions(): Action[AnyContent] = Action.async { implicit request =>
    smService.getAssetsFrontendVersions map { versions =>
      Ok(AssetsVersions(versions.reverse))
    }
  }

  def findGHEReferences(): Action[AnyContent] = Action { implicit request =>
    val references = smService.getAllGHERefs
    Ok(GHEReferences(references))
  }
}
