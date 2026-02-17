package no.nav.melosys.saksflyt.steg.soknad

import mu.KotlinLogging
import no.nav.melosys.domain.arkiv.OpprettJournalpost
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.integrasjon.melosysskjema.MelosysSkjemaApiClient
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey.SØKNADSDATA
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.skjema.types.Skjemadel
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerM2MSkjemaData
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

private const val TITTEL_ARBEIDSTAKER = "Søknad om A1 for utsendte arbeidstakere i EØS/Sveits"
private const val TITTEL_ARBEIDSGIVER = "Bekreftelse på utsending i EØS eller Sveits"

/**
 * Saga-steg som oppretter og ferdigstiller journalpost i Joark for digital søknad.
 *
 * Henter PDF fra melosys-skjema-api og oppretter journalpost med sakstilknytning.
 * Journalposten ferdigstilles i samme kall (forsøkEndeligJfr=true).
 *
 * Forutsetninger:
 * - HENT_SØKNADSDATA har kjørt og lagret søknadsdata på prosessinstansen
 * - OPPRETT_SAK_OG_BEHANDLING_SØKNAD har kjørt og satt behandling på prosessinstansen
 */
@Component
class OpprettOgFerdigstillJournalpostSøknad(
    private val melosysSkjemaApiClient: MelosysSkjemaApiClient,
    private val joarkFasade: JoarkFasade,
    private val behandlingService: BehandlingService
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.OPPRETT_OG_FERDIGSTILL_JOURNALPOST_SØKNAD

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = prosessinstans.behandling
        val fagsak = behandling.fagsak

        val søknadsdata = prosessinstans.hentData<UtsendtArbeidstakerM2MSkjemaData>(SØKNADSDATA)
        val skjema = søknadsdata.skjemaer.first()
        val skjemaId = skjema.id
        val brukerFnr = skjema.fnr
        val referanseId = søknadsdata.referanseId
        val tittel = utledTittel(skjema.metadata.skjemadel)

        log.info { "Henter PDF og oppretter journalpost for digital søknad, referanseId=$referanseId, saksnummer=${fagsak.saksnummer}" }

        val pdf = melosysSkjemaApiClient.hentPdf(skjemaId)

        val opprettJournalpost = OpprettJournalpost.lagJournalpostForDigitalSøknad(
            fagsak,
            pdf,
            brukerFnr,
            referanseId,
            tittel
        )

        val journalpostId = joarkFasade.opprettJournalpost(opprettJournalpost, true)

        behandling.initierendeJournalpostId = journalpostId
        behandlingService.lagre(behandling)

        log.info { "Opprettet journalpost $journalpostId for digital søknad referanseId=$referanseId" }
    }

    private fun utledTittel(skjemadel: Skjemadel): String = when (skjemadel) {
        Skjemadel.ARBEIDSTAKERS_DEL -> TITTEL_ARBEIDSTAKER
        Skjemadel.ARBEIDSGIVERS_DEL -> TITTEL_ARBEIDSGIVER
    }
}
