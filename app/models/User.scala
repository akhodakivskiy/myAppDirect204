package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class User ( id:           Pk[Long] = NotAssigned
                , uuid:         String
                , email:        String
                , openid:       String
                , first:        String
                , last:         String
                , accountId:    Long = -1
                )

object User {

  /*! \brief Parse a User from a ResultSet
   *  
   */
  val simple = {
    get[Pk[Long]]("users.id") ~ 
    get[String]("users.uuid") ~
    get[String]("users.email") ~
    get[String]("users.openid") ~
    get[String]("users.first") ~
    get[String]("users.last") ~
    get[Long]("users.account_id") map {
      case id~uuid~email~openid~first~last~accountId => {
        User(id, uuid, email, openid, first, last, accountId)
      }
    }
  }

  /*! \brief Retrieve user by id
   *  
   */
  def findById(id: Long): Option[User] = {
    findBy[Long]("id", id)
  }

  /*! \brief Retrieve user by uuid
   *  
   */
  def findBy[A](key: String, value: A): Option[User] = {
    DB.withConnection { implicit connection => 
      SQL("select * from users where %s = {value} limit 1".format(key))
        .on('value -> value)
        .as(User.simple.singleOpt)
    }
  }

  /*! \brief Retrieve Users that belong to a particular Account
   *  
   */
  def allFromAccount(accountId: Long): List[User] = DB.withConnection { implicit c => 
    SQL("select * from users where account_id = {accountId}")
      .on('accountId -> accountId)
      .as(simple *)
  }

  /*! \brief Retrieve all Users
   *  
   */
  def all(): List[User] = DB.withConnection { implicit c => 
    SQL("select * from users").as(simple *)
  }

  /*! \brief Insert new user
   *  
   */
  def insert(user: User) {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into users values 
          ( (select next value for user_id_seq)
          , {uuid}
          , {email}
          , {openid}
          , {first}
          , {last}
          , {account_id}
          )
        """
      ).on( 'uuid       -> user.uuid
          , 'email      -> user.email
          , 'openid     -> user.openid
          , 'first      -> user.first
          , 'last       -> user.last
          , 'account_id -> user.accountId
      ).executeInsert()
    }
  }

  def update(id: Long, user: User) {
     DB.withConnection { implicit connection =>
      SQL(
        """
          update users
          set uuid        = {uuid}
            , email       = {email}
            , openid      = {openid}
            , first       = {first}
            , last        = {last}
            , account_id  = {accountId}
          where id = {id}
        """
      ).on( 'id         -> id
          , 'uuid       -> user.uuid
          , 'email      -> user.email
          , 'openid     -> user.openid
          , 'first      -> user.first
          , 'last       -> user.last
          , 'accountId  -> user.accountId
      ).executeUpdate()
    }
  }

  def delete(id: Long) {
    DB.withConnection { implicit connection =>
      SQL("delete from users where id = {id}").on('id -> id).executeUpdate()
    }
  }

  def deleteFromAccount(accountId: Long) {
    DB.withConnection { implicit connection =>
      SQL("delete from users where account_id = {accountId}").on('accountId -> accountId).executeUpdate()
    }
  }

}
