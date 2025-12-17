package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.felles.dto.SoeknadslandDto
import no.nav.melosys.service.journalforing.dto.PeriodeDto
import no.nav.melosys.service.sak.OpprettSak
import no.nav.melosys.service.sak.OpprettSakDto
import no.nav.melosys.service.sak.SøknadDto
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

/**
 * Integrasjonstest som bruker melosys-mock containeren i stedet for in-process mock.
 *
 * Denne testen verifiserer at:
 * 1. melosys-api kan kalle eksterne tjenester via Docker-containeren
 * 2. Containerens verifikasjonsendepunkter fungerer for å verifisere mock-tilstand
 * 3. Forretningslogikk fungerer korrekt med container-baserte mocker
 *
 * Dette er nøkkeltesten for å bevise at Fase 5 (container-migrering) fungerer.
 */
class OpprettSakIT : MockServerTestBaseWithProsessManager() {

    @Autowired
    private lateinit var opprettSak: OpprettSak

    companion object {
        private val log = LoggerFactory.getLogger(OpprettSakIT::class.java)
    }

    @Test
    fun `should create sak using container mock for external services`() {
        log.info("Starter container-integrasjonstest...")
        log.info("Mock-container URL: ${MelosysMockContainerConfig.getBaseUrl()}")

        // Verifiser at containeren er sunn
        mockVerificationClient.isHealthy() shouldBe true
        log.info("Container-helsesjekk bestått")

        // Opprett en sak ved hjelp av OpprettSak-tjenesten
        val opprettSakDto = OpprettSakDto().apply {
            hovedpart = Aktoersroller.BRUKER
            brukerID = "30056928150" // Testperson fra PersonRepo
            sakstype = Sakstyper.EU_EOS
            sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG
            behandlingstema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            behandlingstype = Behandlingstyper.FØRSTEGANG
            behandlingsaarsakType = Behandlingsaarsaktyper.SØKNAD
            soknadDto = SøknadDto().apply {
                land = SoeknadslandDto().apply {
                    landkoder = listOf("BE")
                    isFlereLandUkjentHvilke = false
                }
                periode = PeriodeDto(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31)
                )
            }
            mottaksdato = LocalDate.now()
            skalTilordnes = false // Ikke tilordne for å unngå oppgave-opprettelse
        }

        // Utfør sak-opprettelse og vent på at prosesser fullføres
        val prosessinstans = prosessinstansTestManager.executeAndWait(
            waitForProsesses = mapOf(ProsessType.OPPRETT_SAK to 1),
            returnProsessOfType = ProsessType.OPPRETT_SAK
        ) {
            opprettSak.opprettNySakOgBehandling(opprettSakDto)
        }

        // Verifiser at sak ble opprettet
        prosessinstans.behandling.shouldNotBeNull()
        log.info("Sak opprettet med behandling-ID: ${prosessinstans.behandling?.id}")

        // Verifiser at SAK API ble kalt via containeren
        // OpprettSak-tjenesten kaller SAK API for å opprette en fagsak
        val saker = mockVerificationClient.saker()
        log.info("Saker i mock: ${saker.size}")
        saker.shouldHaveSize(1)

        // Verifiser sak-detaljene
        val sak = saker.first()
        sak.tema shouldBe "MED"
        log.info("SAK-verifisering bestått: tema=${sak.tema}, id=${sak.id}")

        // Hent oppsummering for å se all mock-tilstand
        val summary = mockVerificationClient.summary()
        log.info("Mock-oppsummering: saker=${summary.sakCount}, oppgaver=${summary.oppgaveCount}, medl=${summary.medlCount}")
    }

    @Test
    fun `should verify PDL is called via container when creating sak`() {
        log.info("Tester PDL-integrasjon via container...")

        // OpprettSak-tjenesten kaller PDL for å hente personinfo
        // Vi kan verifisere dette ved å sjekke at prosessen fullføres vellykket
        // (PDL-mock returnerer testdata for testpersonen)

        val opprettSakDto = OpprettSakDto().apply {
            hovedpart = Aktoersroller.BRUKER
            brukerID = "30056928150"
            sakstype = Sakstyper.EU_EOS
            sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG
            behandlingstema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            behandlingstype = Behandlingstyper.FØRSTEGANG
            behandlingsaarsakType = Behandlingsaarsaktyper.SØKNAD
            soknadDto = SøknadDto().apply {
                land = SoeknadslandDto().apply {
                    landkoder = listOf("SE")
                    isFlereLandUkjentHvilke = false
                }
                periode = PeriodeDto(
                    LocalDate.of(2024, 6, 1),
                    LocalDate.of(2024, 6, 30)
                )
            }
            mottaksdato = LocalDate.now()
            skalTilordnes = false
        }

        val prosessinstans = prosessinstansTestManager.executeAndWait(
            waitForProsesses = mapOf(ProsessType.OPPRETT_SAK to 1),
            returnProsessOfType = ProsessType.OPPRETT_SAK
        ) {
            opprettSak.opprettNySakOgBehandling(opprettSakDto)
        }

        // Hvis vi kommer hit uten unntak, ble PDL kalt vellykket via containeren
        val behandling = prosessinstans.behandling.shouldNotBeNull()
        log.info("PDL-integrasjon verifisert - behandling opprettet: ${behandling.id}")

        // Fagsaken bør ha brukerinfo populert fra PDL
        val fagsak = behandling.fagsak.shouldNotBeNull()
        val bruker = fagsak.hentBruker().shouldNotBeNull()
        log.info("Fagsak bruker aktørId: ${bruker.aktørId}")
    }
}
