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

class BehandlingstypeKodeMapperKtTest {

    @Test
    fun `hentBehandlingstypeKode for alle behandlinger skal mappe korrekt behandlingstype`() {
        // No arrange needed for this test


        hentBehandlingstypeKode(behandling(FØRSTEGANG, UTSENDT_ARBEIDSTAKER)) shouldBe BehandlingstypeKode.SOEKNAD
        hentBehandlingstypeKode(behandling(FØRSTEGANG, UTSENDT_SELVSTENDIG)) shouldBe BehandlingstypeKode.SOEKNAD
        hentBehandlingstypeKode(behandling(ENDRET_PERIODE, UTSENDT_ARBEIDSTAKER)) shouldBe BehandlingstypeKode.ENDRET_PERIODE
        hentBehandlingstypeKode(behandling(NY_VURDERING, UTSENDT_SELVSTENDIG)) shouldBe BehandlingstypeKode.NY_VURDERING
        hentBehandlingstypeKode(behandling(KLAGE, UTSENDT_ARBEIDSTAKER)) shouldBe BehandlingstypeKode.KLAGE
        hentBehandlingstypeKode(behandling(FØRSTEGANG, BESLUTNING_LOVVALG_NORGE)) shouldBe BehandlingstypeKode.UTL_MYND_UTPEKT_NORGE
        hentBehandlingstypeKode(behandling(FØRSTEGANG, UTSENDT_ARBEIDSTAKER)) shouldBe BehandlingstypeKode.SOEKNAD
    }

    @Test
    fun `hentBehandlingstypeKode med behandlingstype HENVENDELSE skal kaste exception`() {
        // No arrange needed for this test


        val exception = shouldThrow<IllegalArgumentException> {
            hentBehandlingstypeKode(behandling(HENVENDELSE, UTSENDT_ARBEIDSTAKER))
        }


        exception.message shouldContain "Støtter ikke behandling med type : HENVENDELSE"
    }

    private fun behandling(behandlingstype: Behandlingstyper, behandlingstema: Behandlingstema): Behandling =
        Behandling.forTest {
            tema = behandlingstema
            type = behandlingstype
        }
}
