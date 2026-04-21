package no.nav.melosys.saksflyt.steg.soknad

import mu.KotlinLogging
import no.nav.melosys.domain.arkiv.BrukerIdType
import no.nav.melosys.domain.arkiv.FysiskDokument
import no.nav.melosys.domain.arkiv.Journalposttype
import no.nav.melosys.domain.arkiv.OpprettJournalpost
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.integrasjon.melosysskjema.MelosysSkjemaApiClient
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey.DIGITAL_SØKNADSDATA
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.sak.SkjemaSakMappingService
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

private const val TITTEL_ARBEIDSTAKER = "Søknad om A1 for utsendte arbeidstakere i EØS/Sveits"
private const val TITTEL_ARBEIDSGIVER = "Bekreftelse på utsending i EØS eller Sveits"
private const val MEDLEMSKAP_OG_AVGIFT = "4530"
private const val MEDLEMSKAP = "MED"
private const val NAV_NO = "NAV_NO"

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
class OpprettOgFerdigstillJournalpostDigitalSøknad(
    private val melosysSkjemaApiClient: MelosysSkjemaApiClient,
    private val joarkFasade: JoarkFasade,
    private val behandlingService: BehandlingService,
    private val persondataFasade: PersondataFasade,
    private val skjemaSakMappingService: SkjemaSakMappingService
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.OPPRETT_OG_FERDIGSTILL_JOURNALPOST_DIGITAL_SØKNAD

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = requireNotNull(prosessinstans.behandling) {
            "Behandling må være opprettet før journalpost kan opprettes"
        }
        val fagsak = behandling.fagsak

        val søknadsdata = prosessinstans.hentData<UtsendtArbeidstakerSkjemaM2MDto>(DIGITAL_SØKNADSDATA)
        val skjema = søknadsdata.skjema
        val skjemaId = skjema.id
        val brukerFnr = skjema.fnr
        val innsenderFnr = søknadsdata.innsenderFnr
        val referanseId = søknadsdata.referanseId
        val tittel = utledTittel(skjema.metadata.skjemadel)

        log.info { "Henter PDF og oppretter journalpost for digital søknad, referanseId=$referanseId, saksnummer=${fagsak.saksnummer}" }

        val pdf = melosysSkjemaApiClient.hentPdf(skjemaId)
        val innsenderNavn = persondataFasade.hentSammensattNavn(innsenderFnr)

        //TODO: Hent vedlegg fra melosys-skjema-api og legg til som vedleggsdokumenter på journalposten (egen task/PR - MELOSYS-8030)
        val opprettJournalpost = OpprettJournalpost().apply {
            hoveddokument = FysiskDokument.lagFysiskDokumentDigitalSøknad(pdf, tittel)
            innhold = tittel
            saksnummer = fagsak.saksnummer
            mottaksKanal = NAV_NO
            journalposttype = Journalposttype.INN
            journalførendeEnhet = MEDLEMSKAP_OG_AVGIFT
            tema = MEDLEMSKAP
            brukerId = brukerFnr
            brukerIdType = BrukerIdType.FOLKEREGISTERIDENT
            eksternReferanseId = referanseId
            korrespondansepartId = innsenderFnr
            korrespondansepartNavn = innsenderNavn
            setKorrespondansepartIdType(OpprettJournalpost.KorrespondansepartIdType.FNR)
        }

        val journalpostId = joarkFasade.opprettJournalpost(opprettJournalpost, true)

        // Eksisterende-sak-flyten gjenbruker behandling som allerede kan ha en journalpost
        if (behandling.initierendeJournalpostId == null) {
            behandling.initierendeJournalpostId = journalpostId
            behandlingService.lagre(behandling)
        }

        skjemaSakMappingService.oppdaterJournalpostId(skjemaId, journalpostId)

        log.info { "Opprettet journalpost $journalpostId for digital søknad referanseId=$referanseId" }
    }

    private fun utledTittel(skjemadel: Skjemadel): String = when (skjemadel) {
        Skjemadel.ARBEIDSTAKERS_DEL -> TITTEL_ARBEIDSTAKER
        Skjemadel.ARBEIDSGIVERS_DEL -> TITTEL_ARBEIDSGIVER
        Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL -> TITTEL_ARBEIDSTAKER
    }
}
