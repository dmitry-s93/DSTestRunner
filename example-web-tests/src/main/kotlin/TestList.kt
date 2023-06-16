import test.TestBuilder
import test.TestList
import tests.LoginAndLogoutTest
import tests.LoginFormValidationTest
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