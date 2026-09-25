package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldContainExactly
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.SkjemaSakMapping
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.repository.SkjemaSakMappingRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

// TODO MELOSYS-8309: Engangsretting. Fjern når sakene er rettet i prod.
class FinnDigitalSoknadSakerMedUgyldigTemaIT(
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val skjemaSakMappingRepository: SkjemaSakMappingRepository
) : DataJpaTestBase() {

    private fun lagSak(
        sakstype: Sakstyper,
        behandlingstema: Behandlingstema,
        behandlingsstatus: Behandlingsstatus = Behandlingsstatus.UNDER_BEHANDLING,
        fraDigitalSøknad: Boolean = true
    ): Fagsak {
        val fagsak = fagsakRepository.save(Fagsak.forTest {
            saksnummer = "MEL-UGYLDIG-${UUID.randomUUID().toString().take(8)}"
            type = sakstype
        })
        behandlingRepository.save(Behandling.forTest {
            this.fagsak = fagsak
            tema = behandlingstema
            status = behandlingsstatus
        })
        if (fraDigitalSøknad) {
            skjemaSakMappingRepository.save(SkjemaSakMapping(UUID.randomUUID(), fagsak, null, "{}"))
        }
        return fagsak
    }

    @Test
    fun `finner kun aktive digital søknad-saker med tema utenfor gyldige temaer for sakstypen`() {
        val treff = lagSak(Sakstyper.TRYGDEAVTALE, Behandlingstema.UTSENDT_ARBEIDSTAKER)
        lagSak(Sakstyper.TRYGDEAVTALE, Behandlingstema.YRKESAKTIV)
        lagSak(Sakstyper.TRYGDEAVTALE, Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingsstatus.AVSLUTTET)
        lagSak(Sakstyper.TRYGDEAVTALE, Behandlingstema.UTSENDT_ARBEIDSTAKER, fraDigitalSøknad = false)
        lagSak(Sakstyper.EU_EOS, Behandlingstema.UTSENDT_ARBEIDSTAKER)

        val saksnumre = fagsakRepository.finnDigitalSoknadSaksnumreMedAktivBehandlingUtenforTemaer(
            Sakstyper.TRYGDEAVTALE,
            listOf(Behandlingsstatus.AVSLUTTET),
            listOf(Behandlingstema.YRKESAKTIV)
        )

        saksnumre shouldContainExactly listOf(treff.saksnummer)
    }
}
