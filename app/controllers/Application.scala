package controllers

import play.api._
import play.api.mvc._

import models._

object Application extends Controller {

  def index = Action { implicit request =>
    session.get("userId").map { userId => 
      Ok(views.html.users.index(Account.all()))
    }.getOrElse {
      Unauthorized(views.html.error("You don't have access to this page", false))
    }
  }
  
  def users(accountId: Long) = Action { implicit request =>
    session.get("userId").map { userId => 
      Account.findById(accountId) match {
        case Some(a) => Ok(views.html.users.users(a, User.allFromAccount(accountId)))
        case None => BadRequest("account Doesn't exist")
      }
    }.getOrElse {
      Unauthorized(views.html.error("You don't have access to this page", false))
    }
  }

  def logout = Action { implicit request =>
    session.get("userId").map { userId => 
      try {
        User.findById(userId.toInt) match {
          case Some(u) => {
            Account.findById(u.accountId) match {
              case Some(a) => Redirect(a.baseUrl)
              case None => Redirect("/").withNewSession
            }
          }
          case None => Redirect("/").withNewSession
        }
      } catch {
        case _: java.lang.NumberFormatException => Redirect("/").withNewSession
      }
    }.getOrElse {
      Unauthorized(views.html.error("You don't have access to this page", false))
    }
  }

}
