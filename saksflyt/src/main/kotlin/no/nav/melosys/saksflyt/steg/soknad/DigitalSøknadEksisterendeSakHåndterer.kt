package no.nav.melosys.saksflyt.steg.soknad

import tools.jackson.databind.json.JsonMapper
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.sak.SkjemaSakMappingService
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import org.springframework.stereotype.Component
import java.time.LocalDate

private val log = KotlinLogging.logger { }

private val SØKNADSBEHANDLING_TYPER = setOf(
    Behandlingstyper.FØRSTEGANG,
    Behandlingstyper.NY_VURDERING,
    Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
)

/**
 * Felles attach-logikk for mottak av digital søknad på en eksisterende sak.
 *
 * Brukes både av [HåndterEksisterendeSakDigitalSøknad] (EKSISTERENDE-flyten, der consumeren
 * allerede så en committed sak) og av [OpprettSakOgBehandlingDigitalSøknad] (NY-flyten, når den
 * under DB-låsen oppdager at saken likevel finnes — taper av kappløpet). Å dele logikken sikrer
 * at begge veiene oppfører seg identisk (inkl. at oppgave kun opprettes ved NY_VURDERING).
 *
 * Logikk:
 * 1. Finn åpen søknadsbehandling (FØRSTEGANG/NY_VURDERING/MANGLENDE_INNBETALING)
 * 2a. Åpen behandling funnet:
 *     - UNDER_BEHANDLING/AVVENT_DOK_PART → VURDER_DOKUMENT + reset stegvelger
 *     - OPPRETTET/VURDER_DOKUMENT → kun oppdater mottatte opplysninger
 * 2b. Ingen åpen behandling:
 *     - Opprett ny behandling (NY_VURDERING) + mottatte opplysninger + oppgave
 * 3. Synkroniser aktører fra ny innsending
 * 4. Lagre mapping (skjemaId, originalData, innsendtDato)
 */
@Component
class DigitalSøknadEksisterendeSakHåndterer(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val mottatteOpplysningerService: MottatteOpplysningerService,
    private val oppgaveService: OppgaveService,
    private val skjemaSakMappingService: SkjemaSakMappingService,
    private val jsonMapper: JsonMapper,
    private val aktørSynkronisering: DigitalSøknadAktørSynkronisering
) {

    /**
     * Håndterer en mottatt digital søknad på saken [saksnummer] og returnerer behandlingen
     * søknaden ble knyttet til.
     */
    fun håndter(saksnummer: String, søknadsdata: UtsendtArbeidstakerSkjemaM2MDto): Behandling {
        val referanseId = søknadsdata.referanseId
        val fagsak = fagsakService.hentFagsak(saksnummer)
        log.info { "Håndterer eksisterende sak $saksnummer for digital søknad referanseId=$referanseId, skjemaId=${søknadsdata.skjema.id}" }

        val åpenBehandling = finnÅpenSøknadsbehandling(fagsak)

        val (behandling, mottatteOpplysninger) = if (åpenBehandling != null) {
            håndterÅpenBehandling(åpenBehandling, søknadsdata)
        } else {
            opprettNyVurdering(fagsak, søknadsdata)
        }

        oppdaterAktørerFraNyInnsending(fagsak, søknadsdata)

        lagreSkjemaSakMapping(søknadsdata, fagsak, mottatteOpplysninger)

        log.info { "Ferdig med eksisterende sak $saksnummer, behandling=${behandling.id}" }
        return behandling
    }

    private fun oppdaterAktørerFraNyInnsending(
        fagsak: Fagsak,
        søknadsdata: UtsendtArbeidstakerSkjemaM2MDto
    ) {
        val aktører = DigitalSøknadAktørerMapper.utled(søknadsdata)
        aktørSynkronisering.synkroniser(fagsak, aktører)
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

        val nySoeknad = DigitalSøknadMapper.tilSoeknad(søknadsdata)
        mottatteOpplysningerService.oppdaterMottatteOpplysningerFraSøknad(behandling.id, nySoeknad)

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
        val mottatteOpplysninger = mottatteOpplysningerService.opprettSøknadDigital(
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
