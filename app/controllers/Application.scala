package controllers

import play.api._
import play.api.mvc._

import models._

object Application extends Controller {

  def index = Action { implicit request =>
    session.get("userId").map { userId => 
      Ok(views.html.users.index(Account.all()))
    }.getOrElse {
      Unauthorized("You don't have access to this page")
    }
  }
  
  def users(accountId: Long) = Action { implicit request =>
    session.get("userId").map { userId => 
      Account.findById(accountId) match {
        case Some(a) => Ok(views.html.users.users(a, User.allFromAccount(accountId)))
        case None => BadRequest("account Doesn't exist")
      }
    }.getOrElse {
      Unauthorized("You don't have access to this page")
    }
  }

}
