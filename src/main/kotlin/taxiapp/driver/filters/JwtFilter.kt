package taxiapp.driver.filters

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.servlet.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtFilter : Filter {
    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain
    ) {
        val httpRequest = request as HttpServletRequest
        val authHeader = httpRequest.getHeader("Authorization")

        if (authHeader?.startsWith("Bearer ") == true) {
            val token = authHeader.removePrefix("Bearer ")
            val username = extractUsernameFromJwt(token)

            if (!username.isNullOrBlank()) {
                val principal = UsernamePasswordAuthenticationToken(username, null)
                SecurityContextHolder.getContext().authentication = principal
            }
        }

        chain.doFilter(request, response)
    }

    private fun extractUsernameFromJwt(token: String): String? {
        val parts = token.split(".")
        if (parts.size != 3) return null

        return try {
            val decoded = String(Base64.getDecoder().decode(parts[1]))
            val json = jacksonObjectMapper().readTree(decoded)
            json["sub"]?.asText()
        } catch (e: Exception) {
            null
        }
    }
}