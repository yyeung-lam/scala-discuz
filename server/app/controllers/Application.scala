package controllers

import com.example.playscalajs.shared.SharedMessages
import model.InMemoryModel
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._

import javax.inject._

// handling submission: https://www.playframework.com/documentation/2.8.x/ScalaForms
case class UserData(username: String, password: String)

@Singleton
class Application @Inject()(val controllerComponents: MessagesControllerComponents) extends MessagesBaseController {

  def index = Action { implicit request =>
    Ok(views.html.index(SharedMessages.itWorks))
  }

  val loginForm = Form(mapping(
    "Username" -> text,
    "Password" -> text
  )(UserData.apply)(UserData.unapply))

  val signupForm = Form(mapping(
    "Username" -> text(3, 12),
    "Password" -> text(6)
  )(UserData.apply)(UserData.unapply))

  def login() = Action { implicit request =>
    Ok(views.html.login(loginForm, signupForm))
  }

  def validateLoginForm = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login(formWithErrors, formWithErrors)), // f := formsWithErrors
      loginData =>
        if (InMemoryModel.validateUser(loginData.username, loginData.password)) {
          val username = loginData.username
          Redirect(routes.Application.load)
            .withSession("username" -> username)
            .flashing("success" -> s"Hello $username!")
        } else Redirect(routes.Application.login()).flashing("error" -> "Invalid username/password")
    )
  }

  def logout() = Action {
    Redirect(routes.Application.login())
      .withNewSession
      .flashing("success" -> "user logged out")
  }

  def createUser = Action{ request =>
    val postVal = request.body.asFormUrlEncoded
    postVal.map { args =>
      val username = args("Username").head
      val password = args("Password").head
      if (InMemoryModel.createUser(username, password)) {
        Redirect(routes.Application.load).withSession("username" -> username)
      } else Redirect(routes.Application.login()).flashing("error" -> "user existed")
    }.getOrElse(Redirect("login"))
  }

  def createUserForm = Action { implicit request =>
    signupForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login(formWithErrors, formWithErrors)),
      registerData =>
        if (InMemoryModel.validateUser(registerData.username, registerData.password)) {
          val username = registerData.username
          val password = registerData.password
          if (InMemoryModel.createUser(username, password)) {
            Redirect(routes.Application.load).withSession("username" -> username)
          } else Redirect(routes.Application.login()).flashing("error" -> "user existed")
        } else Redirect(routes.Application.login()).flashing("error" -> "Invalid username/password")
    )
  }

  def load = Action { implicit request =>
    request.session.get("username") match {
      case Some(username: String) => Ok(views.html.allClasses(username, InMemoryModel.getClasses(username)))
      case None => Redirect("login")
    }
  }

  def goToClass(classId: Int) = Action { implicit request =>
    request.session.get("username") match {
      case Some(username: String) => Ok(views.html.classPage(classId))
      case None => Redirect("login")
    }
  }
}
