package no.nav.melosys.saksflyt.steg.soknad

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey.SØKNADSDATA
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.sak.SkjemaSakMappingService
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import org.springframework.stereotype.Component
import java.time.LocalDate

private val log = KotlinLogging.logger { }

/**
 * Saga-steg som håndterer journalføring på eksisterende sak for digital søknad.
 *
 * Brukes i MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD-flyten når mapping-oppslag i consumer
 * avdekket at en relatert instans allerede har opprettet sak.
 *
 * Logikk:
 * 1. Henter eksisterende fagsak via saksnummer fra prosessinstans-data
 * 2. Sjekker behandlingsstatus:
 *    - Aktiv behandling: oppdater status (AVVENT_DOK_PART → OPPRETTET) om nødvendig
 *    - Alle avsluttet: opprett ny behandling med NY_VURDERING + oppgave
 * 3. Lagrer mapping for nåværende skjemaId
 * 4. Setter behandling på prosessinstansen for journalpost-steget
 */
@Component
class HåndterEksisterendeSakSøknad(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val mottatteOpplysningerService: MottatteOpplysningerService,
    private val skjemaSakMappingService: SkjemaSakMappingService,
    private val objectMapper: ObjectMapper
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.HÅNDTER_EKSISTERENDE_SAK_SØKNAD

    override fun utfør(prosessinstans: Prosessinstans) {
        val søknadsdata = prosessinstans.hentData<UtsendtArbeidstakerSkjemaM2MDto>(SØKNADSDATA)
        val saksnummer = prosessinstans.hentData(no.nav.melosys.saksflytapi.domain.ProsessDataKey.SAKSNUMMER)
        val metadata = søknadsdata.skjema.metadata as UtsendtArbeidstakerMetadata
        val referanseId = søknadsdata.referanseId

        val fagsak = fagsakService.hentFagsak(saksnummer)
        log.info { "Håndterer eksisterende sak $saksnummer for digital søknad referanseId=$referanseId" }

        // Lagre mapping for nåværende skjemaId
        skjemaSakMappingService.lagreMapping(søknadsdata.skjema.id, saksnummer)

        val aktivBehandling = fagsak.finnAktivBehandlingIkkeÅrsavregning()

        val behandling = if (aktivBehandling != null) {
            val erArbeidstakerEllerBegge = metadata.skjemadel != Skjemadel.ARBEIDSGIVERS_DEL
            if (aktivBehandling.status == Behandlingsstatus.AVVENT_DOK_PART && erArbeidstakerEllerBegge) {
                aktivBehandling.status = Behandlingsstatus.OPPRETTET
                behandlingService.lagre(aktivBehandling)
                log.info { "Endret behandlingsstatus fra AVVENT_DOK_PART til OPPRETTET for behandling ${aktivBehandling.id}" }
            }
            aktivBehandling
        } else {
            // Alle behandlinger er avsluttet — opprett ny behandling med NY_VURDERING
            val nyBehandling = behandlingService.nyBehandling(
                fagsak,
                Behandlingsstatus.OPPRETTET,
                Behandlingstyper.NY_VURDERING,
                Behandlingstema.UTSENDT_ARBEIDSTAKER,
                null, null,
                LocalDate.now(),
                Behandlingsaarsaktyper.SØKNAD,
                null
            )
            fagsak.leggTilBehandling(nyBehandling)
            log.info { "Opprettet ny behandling ${nyBehandling.id} (NY_VURDERING) på eksisterende sak $saksnummer" }

            // Lagre mottatte opplysninger på ny behandling
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(søknadsdata)
            val originalData = objectMapper.writeValueAsString(søknadsdata)
            mottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs(
                nyBehandling.id, originalData, søknad, referanseId
            )
            nyBehandling
        }

        prosessinstans.behandling = behandling
        log.info { "Ferdig med håndtering av eksisterende sak $saksnummer, behandling=${behandling.id}" }
    }
}
