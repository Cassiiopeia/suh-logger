package example

import kr.suhsaechan.suhlogger.annotation.LogMonitor
import kotlinx.coroutines.delay
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Service

@SpringBootApplication
class MemberApplication

fun main(args: Array<String>) {
    runApplication<MemberApplication>(*args)
}

data class Member(val id: Long, val name: String)

@Service
class MemberService {

    @LogMonitor
    fun register(member: Member): Member = member.copy(name = member.name.uppercase())

    @LogMonitor
    suspend fun find(id: Long): Member {
        delay(30)
        return Member(id, "found")
    }
}
