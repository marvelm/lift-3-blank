package code
package config

import model.User
import net.liftweb._
import common._
import http.S
import sitemap._
import sitemap.Loc._
import net.liftmodules.mapperauth.Locs

object MenuGroups {
  val SettingsGroup = LocGroup("settings")
  val TopBarGroup = LocGroup("topbar")
}

/*
 * Wrapper for Menu locations
 */
case class MenuLoc(menu: Menu) {
  lazy val url: String = S.contextPath + menu.loc.calcDefaultHref
  lazy val fullUrl: String = S.hostAndPath + menu.loc.calcDefaultHref
}

object Site extends Locs {

  import MenuGroups._

  // locations (menu entries)
  val home = MenuLoc(Menu.i("Home") / "index" >> Hidden)
  val loginToken = MenuLoc(buildLoginTokenMenu)
  val logout = MenuLoc(buildLogoutMenu)
  private val profileParamMenu = Menu.param[User]("User", "Profile",
    User.findByUsername,
    _.username.get
  ) / "user" >> Loc.CalcValue(() => User.currentUser)
  lazy val profileLoc = profileParamMenu.toLoc

  val dataAttr = MenuLoc(Menu("DataAttr", "Data Attributes") / "data_attr" >> RequireLoggedIn >> Hidden)

  val password = MenuLoc(Menu.i("Password") / "settings" / "password" >> RequireLoggedIn >> SettingsGroup)
  val account = MenuLoc(Menu.i("Account") / "settings" / "account" >> SettingsGroup >> RequireLoggedIn)
  val editProfile = MenuLoc(Menu("EditProfile", "Profile") / "settings" / "profile" >> SettingsGroup >> RequireLoggedIn)
  val register = MenuLoc(Menu.i("Register") / "register" >> RequireNotLoggedIn)

  private def menus = List(
    home.menu,
    dataAttr.menu,
    Menu.i("Login") / "login" >> RequireNotLoggedIn,
    register.menu,
    loginToken.menu,
    logout.menu,
    profileParamMenu,
    account.menu,
    password.menu,
    editProfile.menu,
    Menu.i("Error") / "error" >> Hidden,
    Menu.i("404") / "404" >> Hidden,
    Menu.i("Throw") / "throw" >> EarlyResponse(() => throw new Exception("This is only a test."))
  )

  /*
   * Return a SiteMap needed for Lift
   */
  def siteMap: SiteMap = SiteMap(menus: _*)
}
