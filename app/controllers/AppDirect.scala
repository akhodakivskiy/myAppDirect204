package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.ws.WS
import play.api.libs.oauth._
import play.api.libs.openid._
import play.api.libs.concurrent._

import scala.xml._

import models._

object AppDirect extends Controller {

  object ErrorCode extends Enumeration {
    type ErrorCode = Value

    val UserAlreadyExists   = Value("USER_ALREADY_EXISTS")
    val UserNotFound        = Value("USER_NOT_FOUND")
    val AccountNotFound     = Value("ACCOUNT_NOT_FOUND")
    val MaxUsersReached     = Value("MAX_USERS_REACHED")
    val Unauthorized        = Value("UNAUTHORIZED")
    val OperationCanceled   = Value("OPERATION_CANCELED")
    val ConfigurationError  = Value("CONFIGURATION_ERROR")
    val InvalidResponse     = Value("INVALID_RESPONSE")
    val UnknownError        = Value("UNKNOWN_ERROR")
  }

  object EventType extends Enumeration {
    type EvenType = Value

    val SubscriptionOrder   = Value("SUBSCRIPTION_ORDER")
    val SubscriptionChange  = Value("SUBSCRIPTION_CHANGE")
    val SubscriptionCancel  = Value("SUBSCRIPTION_CANCEL")
    val SubscriptionNotice  = Value("SUBSCRIPTION_NOTICE")
    
    val UserAssignment      = Value("USER_ASSIGNMENT")
    val UserUnassignment    = Value("USER_UNASSIGNMENT")
  }

  object NoticeType extends Enumeration {
    type NoticeType = Value

    val Deactivate = Value("DEACTIVATE")
    val Reactivate = Value("REACTIVATE")
  }

  // Creates OAuthCalculator used to sign HTTP requests
  def signCalc = {
    val ck = ConsumerKey(
      Play.current.configuration.getString("oauth.key").get,
      Play.current.configuration.getString("oauth.secret").get
    )
    val oauth = OAuth(ServiceInfo(null, null, null, ck))
    OAuthCalculator(ck, RequestToken("", ""))
  }

  /*! \brief Generic API response helper
   *
   *  Wrapped in Ok
   */
  def AppDirectResponse(success:Boolean, errorCode: String, message:String, accountId: String=null) = {
    Ok(
      <result>
          <success>{ success }</success>
          { if (!success) <errorCode>{ errorCode }</errorCode> else null }
          <message>{ message }</message>
          <accountIdentifier>{ accountId }</accountIdentifier>
      </result>
    )
  }

  /*! \brief Success API response helper
   *  
   */
  def AppDirectOk(message: String, accountId: String=null) = {
    AppDirectResponse(true, null, message, accountId)
  }

  /*! \brief Error API response helper
   *  
   */
  def AppDirectError(errorCode: String, message:String, accountId:String=null) = {
    AppDirectResponse(false, errorCode, message, accountId)
  }

  def parseUser(user: Node) = {
    User( 
      uuid      = (user \ "uuid").text
    , email     = (user \ "email").text
    , openid    = (user \ "openId").text
    , first     = (user \ "firstName").text
    , last      = (user \ "lastName").text
    )
  }

  def parseAccount(marketplace: Node, company: Node, order: Node) = {
    Account(
      uuid      = (company \ "uuid").text
    , country   = (company \ "country").text
    , name      = (company \ "name").text
    , phone     = (company \ "phoneNumber").text
    , website   = (company \ "website").text
    , edition   = (order \ "editionCode").text
    , duration  = (order \ "pricingDuration").text
    , partner   = (marketplace \ "partner").text
    , baseUrl   = (marketplace \ "baseUrl").text
    )
  }

  def parseLong(str: String, default: Long) = {
    try {
      str.toLong
    } catch {
      case _: java.lang.NumberFormatException => {
        default
      }
    }
  }

  /*! \brief Creates new Accont
   *
   *  Creaes new Account, and new User properly handling the marketplace and the order
   */
  def AppDirectOrder(event:Elem) = {
    val marketplace = event \\ "event" \ "marketplace" head
    val creator     = event \\ "event" \ "creator" head
    val order       = event \\ "event" \ "payload" \ "order" head
    val company     = event \\ "event" \ "payload" \ "company" head

    // Check if user exists
    User.findBy("uuid", (creator \ "uuid").text) match {
      case Some(_) => AppDirectError(ErrorCode.UserAlreadyExists.toString, "User Already subscribed")
      case None => {

        // Retrieve accountId by checking for the existing Account or creating one
        // Return Option[Long] to handle problems with retrieving/storing an Account
        val accountId = Account.findBy("uuid", (company \ "uuid").text) match {
          case Some(a) => Option[Long](a.id.get)
          case None => Account.insert(parseAccount(marketplace, company, order))
        }

        // Make sure that an Account has been created
        accountId match {
          case Some(aid) => {
            User.insert(parseUser(creator).copy(accountId = aid))
            AppDirectOk("User subscription created", aid.toString)
          }
          case None => AppDirectError(ErrorCode.UnknownError.toString, "Failed to create an account")
        }
      }
    }
  }

  /*! \brief Canges Account order
   *
   *  Replaces existing Account order with new edition/duration values
   */
  def AppDirectChange(event:Elem) = {
    val order   = event \\ "event" \ "payload" \ "order" head
    val account = event \\ "event" \ "payload" \ "account"

    val accountId = parseLong((account \ "accountIdentifier").text, -1)
    Account.findById(accountId) match {
      case Some(a) => {
        Account.update(a.id.get, a.copy(
            edition = (order \ "editionCode").text,
            duration = (order \ "pricingDuration").text
        ))
        AppDirectOk("User Subscription Cancelled")
      }
      case None => AppDirectError(ErrorCode.AccountNotFound.toString, "Account doesn't exist")
    }
  }

  /*! \brief Cancel user subscription
   *
   *  Will first check if the Account containing the User exists
   *  And then will try to find and delete the user
   */
  def AppDirectCancel(event:Elem) = {
    val creator = event \\ "event" \ "creator"
    val account = event \\ "event" \ "payload" \ "account"

    // To deal with String to Long conversion
    val accountId = parseLong((account \ "accountIdentifier").text, -1)
    Account.findById(accountId) match {
      case Some(a) => {
        User.findBy("uuid", (creator \ "uuid").text) match {
          case Some(u) => {
            User.delete(u.id.get)
            AppDirectOk("User Subscription Cancelled")
          }
          case None => {
            AppDirectError(ErrorCode.UserNotFound.toString, "User is not subscribed")
          }
        }
      }
      case None => AppDirectError(ErrorCode.AccountNotFound.toString, "Account doesn't exist")
    }
  }

  /*! \brief Notice event handles
   *
   *  Currently only activates/deactivates the account
   */
  def AppDirectNotice(event:Elem) = {
    val account = event \\ "event" \ "payload" \ "account"
    val notice  = event \\ "event" \ "payload" \ "notice"

    val accountId = parseLong((account \ "accountIdentifier").text, -1)
    val noticeType = (notice \ "type").text
    Account.findById(accountId) match {
      case Some(a) => {
        Account.update(a.id.get, NoticeType.withName(noticeType) match {
          case NoticeType.Reactivate => a.copy(active = true)
          case NoticeType.Deactivate => a.copy(active = false)
          case _ => a
        })
        AppDirectOk("User Subscription Cancelled")
      }
      case None => AppDirectError(ErrorCode.AccountNotFound.toString, "Account doesn't exist")
    }
  }

  /*! \brief Assigns User to the Account
   *
   *  First gets the existing Account, and then creates new User
   */
  def AppDirectAssign(event:Elem) = {
    val account = event \\ "event" \ "payload" \ "account" head
    val user    = event \\ "event" \ "payload" \ "user" head

    val accountId = parseLong((account \ "accountIdentifier").text, -1)
    Account.findById(accountId) match {
      case Some(a) => {
        User.insert(parseUser(user).copy(accountId = accountId))
        AppDirectOk("User Assigned to the Account")
      }
      case None => AppDirectError(ErrorCode.AccountNotFound.toString, "Account doesn't exist")
    }
  }

  /*! \brief Unassigns User to the Account
   *
   *  First gets the existing Account, and then deletes the User if one exists
   */
  def AppDirectUnassign(event:Elem) = {
    val account = event \\ "event" \ "payload" \ "account" head
    val user    = event \\ "event" \ "payload" \ "user" head

    val accountId = parseLong((account \ "accountIdentifier").text, -1)
    Account.findById(accountId) match {
      case Some(a) => {
        User.findBy("uuid", (user \ "uuid").text) match {
          case Some(u) => {
            User.delete(u.id.get)
            AppDirectOk("User Unassigned from the Account")
          }
          case _ => AppDirectError(ErrorCode.UserNotFound.toString, "User is not subscribed")
        }
      }
      case None => AppDirectError(ErrorCode.AccountNotFound.toString, "Account doesn't exist")
    }
  }

  /*! \brief OpenId enabled login Action
   *  
   */
  def login(openid: String, accountid: String = null) = Action { implicit request =>
    val user = User.findBy("openid", openid)

    user match {
      case Some(x) => Redirect("/").withSession("userId" -> x.id.toString)
      case None => Unauthorized("You don't have access to this page")
    }
  }

  /*! \brief Single entry Action for AppDirect subscription management
   *  
   */
  def subscription(token:String, url:String) = Action { implicit request =>
    Async {
      WS.url(url).sign(signCalc).get().map { response =>
        try {
          val event = response.xml

          Logger.info((event \\ "event" \ "type").text)
          Logger.info(event.toString)

          EventType.withName((event \\ "event" \ "type").text) match {
            case EventType.SubscriptionOrder  => AppDirectOrder(event)
            case EventType.SubscriptionChange => AppDirectChange(event)
            case EventType.SubscriptionCancel => AppDirectCancel(event)
            case EventType.SubscriptionNotice => AppDirectNotice(event)
            case EventType.UserAssignment     => AppDirectAssign(event)
            case EventType.UserUnassignment   => AppDirectUnassign(event)
            case _ => AppDirectError(ErrorCode.InvalidResponse.toString, null, null)
          }
        } catch {
          case _: java.lang.NumberFormatException => {
            AppDirectError(ErrorCode.InvalidResponse.toString, null, null)
          }
        }
      }
    }
  }

}
