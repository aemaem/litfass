package lit.fass.server.helper

/**
 * @author Michael Mair
 */
object UnitTest {
    const val NAME = "UnitTest"
}

object IntegrationTest {
    const val NAME = "IntegrationTest"
}

object ApiTest {
    const val NAME = "ApiTest"
}

object TestTypes {
    const val UnitTest = lit.fass.server.helper.UnitTest.NAME
    const val IntegrationTest = lit.fass.server.helper.IntegrationTest.NAME
    const val ApiTest = lit.fass.server.helper.ApiTest.NAME
}
