package no.nav.melosys.service.vedtak

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class FattVedtakVelgerTest {
    val fattVedtakVelger: FattVedtakVelger = FattVedtakVelger(
        mockk<EosVedtakService>(),
        mockk<FtrlVedtakService>(),
        mockk<TrygdeavtaleVedtakService>(),
        mockk<ÅrsavregningVedtakService>()
    )

    @ParameterizedTest
    @MethodSource("sakOgBehandlingKombinasjoner")
    fun getFattVedtakServiceVelgerRiktigImplementasjon(
        saksType: Sakstyper,
        erÅrsavregningBehandling: Boolean,
        fattVedtakServiceImplementasjon: Class<FattVedtakInterface?>
    ) {
        val fagsak = Fagsak(
            "MEL-1", 1L, saksType,
            Sakstemaer.MEDLEMSKAP_LOVVALG, Saksstatuser.OPPRETTET, null,
            mutableSetOf(), mutableListOf()
        )

        val behandling = Behandling.forTest()
        behandling.fagsak = fagsak

        if (erÅrsavregningBehandling) {
            behandling.type = Behandlingstyper.ÅRSAVREGNING
        }


        val fattVedtakService = fattVedtakVelger.getFattVedtakService(behandling)


        fattVedtakService.javaClass shouldBe fattVedtakServiceImplementasjon
    }

    companion object {
        @JvmStatic
        private fun sakOgBehandlingKombinasjoner(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(Sakstyper.FTRL, false, FtrlVedtakService::class.java),
                Arguments.of(Sakstyper.FTRL, true, ÅrsavregningVedtakService::class.java),
                Arguments.of(Sakstyper.EU_EOS, false, EosVedtakService::class.java),
                Arguments.of(Sakstyper.EU_EOS, true, ÅrsavregningVedtakService::class.java),
                Arguments.of(Sakstyper.TRYGDEAVTALE, false, TrygdeavtaleVedtakService::class.java),
                Arguments.of(Sakstyper.TRYGDEAVTALE, true, ÅrsavregningVedtakService::class.java)
            )
        }
    }
}
