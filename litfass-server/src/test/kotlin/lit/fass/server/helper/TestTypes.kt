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

object End2EndTest {
    const val NAME = "End2EndTest"
}

object TestTypes {
    const val UnitTest = lit.fass.server.helper.UnitTest.NAME
    const val IntegrationTest = lit.fass.server.helper.IntegrationTest.NAME
    const val End2EndTest = lit.fass.server.helper.End2EndTest.NAME
}
