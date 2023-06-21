import test.TestBuilder
import test.TestList
import tests.authentication.loginAndLogout.LoginAndLogoutTest
import tests.authentication.loginFormValidation.LoginFormValidationTest
import kotlin.reflect.KClass

@Suppress("unused")
class TestList : TestList {
    private val tests = listOf(
        LoginAndLogoutTest::class,      // TEST_01 Login and logout
        LoginFormValidationTest::class, // TEST_02 Login Form Validation
    )

    override fun getTestList(): List<KClass<out TestBuilder>> {
        return tests
    }
}