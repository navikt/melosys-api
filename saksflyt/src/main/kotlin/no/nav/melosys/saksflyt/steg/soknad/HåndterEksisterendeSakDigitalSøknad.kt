package no.nav.melosys.saksflyt.steg.soknad

import tools.jackson.databind.json.JsonMapper
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.sak.SkjemaSakMappingService
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

private val log = KotlinLogging.logger { }

internal val OSLO_ZONE: ZoneId = ZoneId.of("Europe/Oslo")

private val SØKNADSBEHANDLING_TYPER = setOf(
    Behandlingstyper.FØRSTEGANG,
    Behandlingstyper.NY_VURDERING,
    Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
)

/**
 * Saga-steg som håndterer mottak av digital søknad på eksisterende sak.
 *
 * Brukes i MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD-flyten.
 *
 * Logikk:
 * 1. Hent eksisterende fagsak via saksnummer fra prosessinstans-data
 * 2. Finn åpen søknadsbehandling (FØRSTEGANG/NY_VURDERING/MANGLENDE_INNBETALING)
 * 3a. Åpen behandling funnet:
 *     - UNDER_BEHANDLING/AVVENT_DOK_PART → VURDER_DOKUMENT + reset stegvelger
 *     - OPPRETTET/VURDER_DOKUMENT → kun oppdater mottatte opplysninger
 * 3b. Ingen åpen behandling:
 *     - Opprett ny behandling (NY_VURDERING) + mottatte opplysninger + oppgave
 * 4. Lagre mapping (skjemaId, originalData, innsendtDato)
 * 5. Sett behandling på prosessinstansen
 */
@Component
class HåndterEksisterendeSakDigitalSøknad(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val mottatteOpplysningerService: MottatteOpplysningerService,
    private val oppgaveService: OppgaveService,
    private val skjemaSakMappingService: SkjemaSakMappingService,
    private val jsonMapper: JsonMapper
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.HÅNDTER_EKSISTERENDE_SAK_DIGITAL_SØKNAD

    override fun utfør(prosessinstans: Prosessinstans) {
        val søknadsdata = prosessinstans.hentData<UtsendtArbeidstakerSkjemaM2MDto>(ProsessDataKey.DIGITAL_SØKNADSDATA)
        val saksnummer = prosessinstans.hentData(ProsessDataKey.SAKSNUMMER)
        val referanseId = søknadsdata.referanseId

        val fagsak = fagsakService.hentFagsak(saksnummer)
        log.info { "Håndterer eksisterende sak $saksnummer for digital søknad referanseId=$referanseId, skjemaId=${søknadsdata.skjema.id}" }

        val åpenBehandling = finnÅpenSøknadsbehandling(fagsak)

        val (behandling, mottatteOpplysninger) = if (åpenBehandling != null) {
            håndterÅpenBehandling(åpenBehandling, søknadsdata)
        } else {
            opprettNyVurdering(fagsak, søknadsdata)
        }

        lagreSkjemaSakMapping(søknadsdata, fagsak, mottatteOpplysninger)

        prosessinstans.behandling = behandling
        log.info { "Ferdig med eksisterende sak $saksnummer, behandling=${behandling.id}" }
    }

    private fun lagreSkjemaSakMapping(
        søknadsdata: UtsendtArbeidstakerSkjemaM2MDto,
        fagsak: Fagsak,
        mottatteOpplysninger: MottatteOpplysninger
    ) {
        skjemaSakMappingService.lagreMapping(
            skjemaId = søknadsdata.skjema.id,
            fagsak = fagsak,
            mottatteOpplysninger = mottatteOpplysninger,
            originalData = jsonMapper.writeValueAsString(søknadsdata),
            innsendtDato = søknadsdata.innsendtTidspunkt.atZone(OSLO_ZONE).toInstant()
        )
    }

    private fun finnÅpenSøknadsbehandling(fagsak: Fagsak): Behandling? {
        val aktiv = fagsak.finnAktivBehandlingIkkeÅrsavregning() ?: return null
        return if (aktiv.type in SØKNADSBEHANDLING_TYPER) aktiv else null
    }

    private fun håndterÅpenBehandling(
        behandling: Behandling,
        søknadsdata: UtsendtArbeidstakerSkjemaM2MDto
    ): Pair<Behandling, MottatteOpplysninger> {
        val utledetBehandlingstema = BehandlingstemaUtleder.utled(søknadsdata)
        if (behandling.tema != utledetBehandlingstema) {
            behandlingService.endreTema(behandling, utledetBehandlingstema)
        }

        val skalResetteStegvelger = behandling.status in setOf(
            Behandlingsstatus.UNDER_BEHANDLING,
            Behandlingsstatus.AVVENT_DOK_PART
        )

        if (skalResetteStegvelger) {
            behandlingService.endreStatus(behandling, Behandlingsstatus.VURDER_DOKUMENT)
            behandlingsresultatService.tømBehandlingsresultat(behandling.id)
        }

        val (periode, land) = DigitalSøknadMapper.hentPeriodeOgLand(søknadsdata)

        mottatteOpplysningerService.oppdaterMottatteOpplysningerPeriodeOgLand(
            behandling.id, periode, land
        )

        val mottatteOpplysninger = mottatteOpplysningerService.hentMottatteOpplysninger(behandling.id)
        return behandling to mottatteOpplysninger
    }

    private fun opprettNyVurdering(
        fagsak: Fagsak,
        søknadsdata: UtsendtArbeidstakerSkjemaM2MDto
    ): Pair<Behandling, MottatteOpplysninger> {
        val saksnummer = fagsak.saksnummer
        val referanseId = søknadsdata.referanseId
        val behandlingstema = BehandlingstemaUtleder.utled(søknadsdata)

        val nyBehandling = behandlingService.nyBehandling(
            fagsak,
            Behandlingsstatus.OPPRETTET,
            Behandlingstyper.NY_VURDERING,
            behandlingstema,
            null, // journalpostId settes i journalpost-steget etterpå
            null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        )
        fagsak.leggTilBehandling(nyBehandling)
        log.info { "Opprettet behandling ${nyBehandling.id} (NY_VURDERING) på sak $saksnummer" }

        val søknad = DigitalSøknadMapper.tilSoeknad(søknadsdata)
        val mottatteOpplysninger = mottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs(
            nyBehandling.id, null, søknad, referanseId
        )

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
            nyBehandling,
            null,
            fagsak.finnBrukersAktørID(),
            null,
            fagsak.finnVirksomhetsOrgnr()
        )
        log.info { "Opprettet oppgave for ny behandling ${nyBehandling.id}" }

        return nyBehandling to mottatteOpplysninger
    }


}
