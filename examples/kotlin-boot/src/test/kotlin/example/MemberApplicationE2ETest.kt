package example

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension

@ExtendWith(OutputCaptureExtension::class)
@SpringBootTest
class MemberApplicationE2ETest {

    @Autowired
    lateinit var service: MemberService

    @Test
    fun kotlinServiceIsLogged(output: CapturedOutput) {
        assertThat(service.register(Member(1, "suh")).name).isEqualTo("SUH")
        assertThat(output.out).contains("[MemberService.register] CALL").contains("\"member\"")
    }

    @Test
    fun suspendFunctionIsLoggedWithoutContinuation(output: CapturedOutput) {
        val member = runBlocking { service.find(7) }
        assertThat(member.name).isEqualTo("found")
        assertThat(output.out).contains("[MemberService.find] CALL").contains("\"id\": 7")
        assertThat(output.out).doesNotContain("\$completion").doesNotContain("Continuation")
    }
}
