package no.nav.melosys.itest

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.repository.SkjemaSakMappingRepository
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.sak.SkjemaSakMappingService
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

/**
 * MELOSYS-8151, testplan 2.5: en claim-rad i skjema_sak_mapping (reservasjon uten data) skal fylles
 * med ekte data når delen mottas. Peker claim-raden på en annen sak enn den delen faktisk landet på,
 * må raden erstattes — `saksnummer` er `updatable = false`, så en ren merge ville beholdt den gamle
 * saken. Kjøres mot ekte Oracle slik at JPA-oppførselen (merge vs. delete + insert) er den reelle.
 */
class SkjemaSakMappingClaimIT(
    @Autowired val behandlingService: BehandlingService,
    @Autowired val mottatteOpplysningerService: MottatteOpplysningerService,
    @Autowired val skjemaSakMappingService: SkjemaSakMappingService,
    @Autowired val skjemaSakMappingRepository: SkjemaSakMappingRepository,
    @Autowired val fagsakRepository: FagsakRepository,
) : ComponentTestBase() {

    @Test
    fun `claim-rad mot annen sak erstattes og peker paa saken delen landet paa`() {
        val claimetSak = lagFagsak()
        val faktiskSak = lagFagsak()
        val mottatteOpplysninger = lagMottatteOpplysninger(faktiskSak)
        val skjemaId = UUID.randomUUID()

        skjemaSakMappingService.claimRelaterteSkjemaIder(listOf(skjemaId), claimetSak)
        skjemaSakMappingRepository.findBySkjemaId(skjemaId).get().saksnummer shouldBe claimetSak.saksnummer

        skjemaSakMappingService.lagreMapping(skjemaId, faktiskSak, mottatteOpplysninger, "{}", Instant.now())

        val mapping = skjemaSakMappingRepository.findBySkjemaId(skjemaId).orElse(null).shouldNotBeNull()
        mapping.saksnummer shouldBe faktiskSak.saksnummer
        mapping.originalData shouldBe "{}"
        mapping.mottatteOpplysninger.shouldNotBeNull().id shouldBe mottatteOpplysninger.id
        skjemaSakMappingRepository.findBySkjemaIdIn(listOf(skjemaId)).size shouldBe 1
    }

    @Test
    fun `claim-rad mot samme sak fylles med data`() {
        val sak = lagFagsak()
        val mottatteOpplysninger = lagMottatteOpplysninger(sak)
        val skjemaId = UUID.randomUUID()

        skjemaSakMappingService.claimRelaterteSkjemaIder(listOf(skjemaId), sak)
        skjemaSakMappingRepository.findBySkjemaId(skjemaId).get().originalData.shouldBeNull()

        skjemaSakMappingService.lagreMapping(skjemaId, sak, mottatteOpplysninger, "{}", Instant.now())

        val mapping = skjemaSakMappingRepository.findBySkjemaId(skjemaId).orElse(null).shouldNotBeNull()
        mapping.saksnummer shouldBe sak.saksnummer
        mapping.originalData shouldBe "{}"
        skjemaSakMappingRepository.findBySkjemaIdIn(listOf(skjemaId)).size shouldBe 1
    }

    private fun lagFagsak(): Fagsak = Fagsak.forTest {
        type = Sakstyper.FTRL
        tema = Sakstemaer.MEDLEMSKAP_LOVVALG
        status = Saksstatuser.OPPRETTET
        medBruker()
    }.let { fagsakRepository.save(it) }

    private fun lagMottatteOpplysninger(fagsak: Fagsak) = behandlingService.nyBehandling(
        fagsak,
        Behandlingsstatus.OPPRETTET,
        Behandlingstyper.FØRSTEGANG,
        Behandlingstema.UTSENDT_ARBEIDSTAKER,
        "test",
        "test",
        LocalDate.now(),
        Behandlingsaarsaktyper.SØKNAD,
        "test",
    ).let { behandling ->
        mottatteOpplysningerService.hentEllerOpprettMottatteOpplysninger(behandling.id, true).shouldNotBeNull()
    }
}
