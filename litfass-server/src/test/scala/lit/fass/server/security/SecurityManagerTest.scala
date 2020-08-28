//package lit.fass.server.security
//
//import com.typesafe.config.ConfigFactory
//import lit.fass.server.helper.UnitTest
//import org.junit.experimental.categories.Category
//import org.junit.runner.RunWith
//import org.scalatest.matchers.should.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//import org.scalatestplus.junit.JUnitRunner
//
//
///**
// * @author Michael Mair
// */
//@RunWith(classOf[JUnitRunner])
//@Category(Array(classOf[UnitTest]))
//class SecurityManagerTest extends AnyWordSpec with Matchers {
//
//  "security manager" should {
//
//    "initialize users from config" in {
//      val config = ConfigFactory.parseResources("application-test.conf")
//      val securityManager: SecurityManager = SecurityManager(config)
//      securityManager.getRealm.accountExists("admin") shouldBe true
//      securityManager.getRealm.accountExists("user") shouldBe true
//      securityManager.getRealm.accountExists("foo") shouldBe false
//    }
//
//    "authenticate users" in {
//      val securityManager = SecurityManager(ConfigFactory.parseResources("application-test.conf"))
//      val subject = securityManager.loginHttpBasic("YWRtaW46YWRtaW4=") // admin:admin
//      subject.isAuthenticated shouldBe true
//    }
//
//    "not authenticate users with wrong password" in {
//      val securityManager = SecurityManager(ConfigFactory.parseResources("application-test.conf"))
//      val subject = securityManager.loginHttpBasic("YWRtaW46QURNSU4=") // admin:ADMIN
//      subject.isAuthenticated shouldBe false
//    }
//
//    "authorize users" in {
//      val securityManager = SecurityManager(ConfigFactory.parseResources("application-test.conf"))
//      val subject = securityManager.loginHttpBasic("YWRtaW46YWRtaW4=") // admin:admin
//      subject.hasRole("ADMIN") shouldBe true
//      subject.hasRole("FOO") shouldBe false
//    }
//
//  }
//}
