package no.nav.melosys.integrasjon.trygdeavgift

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

class TrygdeavgiftBrukerIdTest {
    @Test
    fun `begge beregningskall videresender innlogget bruker og saksbehandler fra saga per kall`() {
        val requests = mutableListOf<ClientRequest>()
        val client = TrygdeavgiftClient("http://beregning", WebClient.builder().exchangeFunction { request ->
            requests.add(request)
            Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json").body("[]").build())
        })
        val subject = mockk<SubjectHandler>()
        mockkStatic(SubjectHandler::class)
        try {
            every { SubjectHandler.getInstance() } returns subject
            for ((innlogget, saga, forventet) in listOf(
                Triple("Z123456", "Z654321", "Z123456"),
                Triple(null, "Z654321", "Z654321"),
                Triple(null, null, null)
            )) {
                every { subject.userID } returns innlogget
                val prosessId = UUID.randomUUID()
                ThreadLocalAccessInfo.beforeExecuteProcess(prosessId, "beregning", saga, null)
                try {
                    client.beregnTrygdeavgift(TrygdeavgiftsberegningRequest(emptySet(), emptySet(), emptyList(), LocalDate.of(2000, 1, 1)))
                    val dato = LocalDate.of(2026, 1, 1)
                    client.beregnTrygdeavgiftEosPensjonist(EøsPensjonistTrygdeavgiftsberegningRequest(
                        HelseutgiftDekkesPeriodeDto(DatoPeriodeDto(dato, dato)), emptySet(), emptyList(), LocalDate.of(1950, 1, 1)
                    ))
                    requests.takeLast(2).map { it.headers().getFirst("Nav-User-Id") } shouldBe listOf(forventet, forventet)
                } finally {
                    ThreadLocalAccessInfo.afterExecuteProcess(prosessId)
                }
            }
            requests.map { it.url().path }.toSet() shouldBe setOf("/v2/beregn", "/v2/eos-pensjonist/beregn")
        } finally {
            unmockkStatic(SubjectHandler::class)
        }
    }
}
