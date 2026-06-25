package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldNotBe
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
import no.nav.melosys.service.sak.EndreSakService
import no.nav.melosys.service.sak.SkjemaSakMappingService
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

/**
 * Reproduserer MELOSYS-8135: når en behandling med en skjema_sak_mapping endres slik at
 * mottatte opplysninger må gjenopprettes, ble slettingen av mottatteopplysninger blokkert av
 * FK_SKJEMA_SAK_MAPPING_MOTTOPP (ORA-02292).
 *
 * Testen kjører mot ekte Oracle der fremmednøkkelen er aktivert, og verifiserer at endringen
 * går gjennom uten FK-brudd og at skjema_sak_mapping re-pekes til den nye mottatteopplysninger.
 */
class EndreSakSkjemaSakMappingIT(
    @Autowired val endreSakService: EndreSakService,
    @Autowired val behandlingService: BehandlingService,
    @Autowired val mottatteOpplysningerService: MottatteOpplysningerService,
    @Autowired val skjemaSakMappingService: SkjemaSakMappingService,
    @Autowired val skjemaSakMappingRepository: SkjemaSakMappingRepository,
    @Autowired val fagsakRepository: FagsakRepository,
) : ComponentTestBase() {

    @Test
    fun `endre behandlingstema gjenoppretter mottatte opplysninger og re-peker skjema-sak-mapping uten FK-brudd`() {
        // Arrange: EU/EØS-sak med aktiv behandling, mottatte opplysninger og tilknyttet skjema_sak_mapping
        val fagsak = Fagsak.forTest {
            type = Sakstyper.EU_EOS
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            status = Saksstatuser.OPPRETTET
            medBruker()
        }.let { fagsakRepository.save(it) }

        val behandling = behandlingService.nyBehandling(
            fagsak,
            Behandlingsstatus.UNDER_BEHANDLING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.UTSENDT_SELVSTENDIG,
            "test",
            "test",
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            "test",
        )

        val opprinneligMottatteOpplysninger = mottatteOpplysningerService
            .hentEllerOpprettMottatteOpplysninger(behandling.id, true)
            .shouldNotBeNull()
        val opprinneligMottatteOpplysningerId = opprinneligMottatteOpplysninger.id

        val skjemaId = UUID.randomUUID()
        skjemaSakMappingService.lagreMapping(
            skjemaId = skjemaId,
            fagsak = fagsak,
            mottatteOpplysninger = opprinneligMottatteOpplysninger,
            originalData = "{}",
            innsendtDato = Instant.now(),
        )

        // Act: endrer kun behandlingstema (sak uendret), som trigger gjenoppretting av mottatte opplysninger
        endreSakService.endre(
            fagsak.saksnummer,
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
            Behandlingstyper.FØRSTEGANG,
            Behandlingsstatus.UNDER_BEHANDLING,
            null,
        )

        // Assert: ny mottatte opplysninger opprettet, og mappingen er re-pekt fra gammel til ny MO
        val nyMottatteOpplysninger = mottatteOpplysningerService
            .finnMottatteOpplysninger(behandling.id)
            .orElse(null)
            .shouldNotBeNull()
        nyMottatteOpplysninger.id shouldNotBe opprinneligMottatteOpplysningerId

        skjemaSakMappingRepository.findByMottatteOpplysninger_Id(opprinneligMottatteOpplysningerId).shouldBeEmpty()
        skjemaSakMappingRepository.findByMottatteOpplysninger_Id(nyMottatteOpplysninger.id)
            .map { it.skjemaId } shouldContainExactly listOf(skjemaId)
    }
}
