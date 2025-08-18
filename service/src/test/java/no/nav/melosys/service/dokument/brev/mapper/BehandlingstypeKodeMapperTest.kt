package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.dok.melosysbrev.felles.melosys_felles.BehandlingstypeKode
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*
import no.nav.melosys.service.dokument.brev.mapper.BehandlingstypeKodeMapper.hentBehandlingstypeKode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BehandlingstypeKodeMapperTest {

    @ParameterizedTest
    @MethodSource("validBehandlingsTypeMapping")
    fun `hentBehandlingstypeKode skal mappe korrekt behandlingstype`(
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema,
        expectedKode: BehandlingstypeKode
    ) {
        val behandling = createBehandling(behandlingstype, behandlingstema)


        val result = hentBehandlingstypeKode(behandling)


        result shouldBe expectedKode
    }

    @Test
    fun `hentBehandlingstypeKode med behandlingstype HENVENDELSE skal kaste exception`() {
        val behandling = createBehandling(HENVENDELSE, UTSENDT_ARBEIDSTAKER)

        val exception = shouldThrow<IllegalArgumentException> {
            hentBehandlingstypeKode(behandling)
        }

        exception.message shouldContain "Støtter ikke behandling med type : HENVENDELSE"
    }

    private fun createBehandling(
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema
    ): Behandling = Behandling.forTest {
        tema = behandlingstema
        type = behandlingstype
    }

    fun validBehandlingsTypeMapping() = listOf(
        Arguments.of(FØRSTEGANG, UTSENDT_ARBEIDSTAKER, BehandlingstypeKode.SOEKNAD),
        Arguments.of(FØRSTEGANG, UTSENDT_SELVSTENDIG, BehandlingstypeKode.SOEKNAD),
        Arguments.of(ENDRET_PERIODE, UTSENDT_ARBEIDSTAKER, BehandlingstypeKode.ENDRET_PERIODE),
        Arguments.of(NY_VURDERING, UTSENDT_SELVSTENDIG, BehandlingstypeKode.NY_VURDERING),
        Arguments.of(KLAGE, UTSENDT_ARBEIDSTAKER, BehandlingstypeKode.KLAGE),
        Arguments.of(FØRSTEGANG, BESLUTNING_LOVVALG_NORGE, BehandlingstypeKode.UTL_MYND_UTPEKT_NORGE)
    )
}
