package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class Account ( id:        Pk[Long] = NotAssigned
                   , uuid:      String
                   , country:   String
                   , name:      String
                   , phone:     String
                   , website:   String
                   , edition:   String
                   , duration:  String
                   , active:    Boolean = true
                   , partner:   String
                   , baseUrl:   String
                   )

object Account {

  /*! \brief Parse an Account from a ResultSet
   *  
   */
  val simple = {
    get[Pk[Long]]("accounts.id") ~ 
    get[String]("accounts.uuid") ~
    get[String]("accounts.country") ~
    get[String]("accounts.name") ~
    get[String]("accounts.phone") ~
    get[String]("accounts.website") ~
    get[String]("accounts.edition") ~
    get[String]("accounts.duration") ~
    get[Boolean]("accounts.active") ~
    get[String]("accounts.partner") ~
    get[String]("accounts.base_url") map {
      case id~uuid~country~name~phone~website~edition~duration~active~partner~baseUrl => {
        Account(id, uuid, country, name, phone, website, edition, duration, active, partner, baseUrl)
      }
    }
  }

  /*! \brief Retrieve Account by id
   *  
   */
  def findById(id: Long): Option[Account] = {
    findBy("id", id)
  }

  /*! \brief Retrieve Account by uuid
   *  
   */
  def findBy[A](key: String, value: A): Option[Account] = {
    DB.withConnection { implicit connection => 
      SQL("select * from accounts where %s = {value} limit 1".format(key))
        .on('value -> value)
        .as(Account.simple.singleOpt)
    }
  }

  /*! \brief Retrieve all Accounts
   *  
   */
  def all(): List[Account] = DB.withConnection { implicit c => 
    SQL("select * from accounts").as(simple *)
  }

  /*! \brief Insert new Account
   *  
   */
  def insert(account: Account): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into accounts values 
          ( (select next value for account_id_seq)
          , {uuid}
          , {country}
          , {name}
          , {phone}
          , {website}
          , {edition}
          , {duration}
          , {active}
          , {partner}
          , {baseUrl}
          )
        """
      ).on( 'uuid     -> account.uuid
          , 'country  -> account.country
          , 'name     -> account.name
          , 'phone    -> account.phone
          , 'website  -> account.website
          , 'edition  -> account.edition
          , 'duration -> account.duration
          , 'active   -> account.active
          , 'partner  -> account.partner
          , 'baseUrl  -> account.baseUrl
      ).executeInsert()
    }
  }

  def update(id: Long, account: Account) {
     DB.withConnection { implicit connection =>
      SQL(
        """
          update accounts
          set uuid     = {uuid}
            , country  = {country}
            , name     = {name}
            , phone    = {phone}
            , website  = {website}
            , edition  = {edition}
            , duration = {duration}
            , active   = {active}
            , partner  = {partner}
            , base_url = {baseUrl}
          where id = {id}
        """
      ).on( 'id       -> id
          , 'uuid     -> account.uuid
          , 'country  -> account.country
          , 'name     -> account.name
          , 'phone    -> account.phone
          , 'website  -> account.website
          , 'edition  -> account.edition
          , 'duration -> account.duration
          , 'active   -> account.active
          , 'partner  -> account.partner
          , 'baseUrl  -> account.baseUrl
      ).executeUpdate()
    }
  }

  def delete(id: Long) {
    DB.withConnection { implicit connection =>
      SQL("delete from accounts where id = {id}").on('id -> id).executeUpdate()
    }
  }

}
