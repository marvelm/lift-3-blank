package bootstrap.liftweb

import javax.mail.internet.MimeMessage

import code.config._
import code.lib.DataAttributes
import code.model.{SystemUser, User}
import net.liftmodules.extras.{Gravatar, LiftExtras}
import net.liftmodules.mapperauth.MapperAuth
import net.liftweb._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.util.Helpers._
import net.liftweb.util._

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends Loggable {
  def boot {

    /*
     * Writing to log file configured in ...logback.xml
     */
    logger.info("Run Mode: "+Props.mode.toString)

    // init database
    MapperConfig.init()

    DBSetup.run()

    // init configuration
    MapperAuth.init()
    MapperAuth.authUserMeta.default.set(User)
    MapperAuth.loginTokenAfterUrl.default.set(Site.password.url)
    MapperAuth.siteName.default.set("AGYNAMIX Template")
    MapperAuth.systemEmail.default.set(SystemUser.user.email.get)
    MapperAuth.systemUsername.default.set(SystemUser.user.name.get)

    // For S.loggedIn_? and TestCond.loggedIn/Out builtin snippet
    LiftRules.loggedInTest = Full(() => User.isLoggedIn)

    // checks for ExtSession cookie
    LiftRules.earlyInStateful.append(User.testForExtSession)

    // Gravatar
    Gravatar.defaultImage.default.set("wavatar")

    // config an email sender
    SmtpMailer.init()

    // where to search snippet
    LiftRules.addToPackages("code")

    // set the default htmlProperties
    LiftRules.htmlProperties.default.set((r: Req) => new Html5Properties(r.userAgent))

    // Build SiteMap
    LiftRules.setSiteMap(Site.siteMap)

    // Error handler
    ErrorHandler.init()

    // 404 handler
    LiftRules.uriNotFound.prepend(NamedPF("404handler") {
      case (req, failure) =>
        NotFoundAsTemplate(ParsePath(List("404"), "html", false, false))
    })

    // Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-spinner").cmd)

    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-spinner").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // Init Extras
    LiftExtras.init()

    // don't include the liftAjax.js code. It's served statically.
    // LiftRules.autoIncludeAjaxCalc.default.set(() => (session: LiftSession) => false)

    // Mailer
    Mailer.devModeSend.default.set((m: MimeMessage) => logger.info("Dev mode message:\n" + prettyPrintMime(m)))
    Mailer.testModeSend.default.set((m: MimeMessage) => logger.info("Test mode message:\n" + prettyPrintMime(m)))

    // ALTERNATIVE:
    // Or, guard the API if only avl. for logged in users, so you don't have to do it at every call
    // val withAuthentication: PartialFunction[Req, Unit] = { case _ if User.isLoggedIn => }
    // LiftRules.dispatch.append(withAuthentication guard TodoApi)

    // Add a DataProcessor
    LiftRules.dataAttributeProcessor.append {
      case ("ad-processor", attributeStr, enclosedNodes, itsSession) =>
           DataAttributes.adProcessor(attributeStr, enclosedNodes)
    }

    // Add a DataProcessor
    LiftRules.dataAttributeProcessor.append {
      case ("scaladays", attributeStr, enclosedNodes, itsSession) =>
            ("p *+" #> attributeStr).apply(enclosedNodes)
    }
  }

  private def prettyPrintMime(m: MimeMessage): String = {
    val buf = new StringBuilder
    val hdrs = m.getAllHeaderLines
    while (hdrs.hasMoreElements)
      buf ++= hdrs.nextElement.toString + "\n"

    val out =
      """
        |%s
        |====================================
        |%s
      """.format(buf.toString, m.getContent.toString).stripMargin

    out
  }
}
