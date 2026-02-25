package no.nav.melosys.saksflyt.steg.soknad

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey.SØKNADSDATA
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.sak.OpprettSakRequest
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import org.springframework.stereotype.Component
import java.time.LocalDate

private val log = KotlinLogging.logger { }

/**
 * Saga-steg som oppretter fagsak og behandling fra mottatt digital søknad,
 * og lagrer mottatte opplysninger (søknadsdata) på behandlingen.
 *
 * Henter søknadsdata fra prosessinstansen (lagret av HENT_SØKNADSDATA) og:
 * 1. Oppretter fagsak med sakstype EU/EØS og tema MEDLEMSKAP_LOVVALG
 * 2. Oppretter behandling med tema UTSENDT_ARBEIDSTAKER
 * 3. Lagrer behandling på prosessinstansen for videre bruk
 * 4. Mapper søknadsdata til Soeknad og lagrer som MottatteOpplysninger
 *
 * TODO MELOSYS-7774: Legg til matching-logikk for å sjekke om behandling allerede finnes
 * (arbeidstaker-fnr + juridiskEnhetOrgnr + overlappende periode) for todelt søknad.
 */
@Component
class OpprettSakOgBehandlingSøknad(
    private val fagsakService: FagsakService,
    private val persondataFasade: PersondataFasade,
    private val mottatteOpplysningerService: MottatteOpplysningerService,
    private val objectMapper: ObjectMapper
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.OPPRETT_SAK_OG_BEHANDLING_SØKNAD

    override fun utfør(prosessinstans: Prosessinstans) {
        val søknadsdata = prosessinstans.hentData<UtsendtArbeidstakerSkjemaM2MDto>(SØKNADSDATA)

        // Hent skjema (arbeidstaker er alltid hovedperson)
        val skjema = søknadsdata.skjema
        val fnr = skjema.fnr
        val referanseId = søknadsdata.referanseId

        log.info { "Oppretter fagsak og behandling for digital søknad, referanseId=$referanseId" }

        // Hent aktørId fra fødselsnummer
        val aktørId = persondataFasade.hentAktørIdForIdent(fnr)

        // TODO MELOSYS-7774: Sjekk om matchende behandling allerede finnes
        // (arbeidstaker-fnr + juridiskEnhetOrgnr + overlappende periode)

        val opprettSakRequest = OpprettSakRequest.Builder()
            .medAktørID(aktørId)
            .medSakstype(Sakstyper.EU_EOS)
            .medSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG)
            .medBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER)
            .medBehandlingstype(Behandlingstyper.FØRSTEGANG)
            .medBehandlingsårsaktype(Behandlingsaarsaktyper.SØKNAD)
            .medMottaksdato(LocalDate.now())
            .build()

        val fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest)
        val behandling = fagsak.hentAktivBehandling()

        prosessinstans.behandling = behandling

        log.info { "Opprettet fagsak ${fagsak.saksnummer} med behandling ${behandling.id} for digital søknad referanseId=$referanseId" }

        // Lagre mottatte opplysninger (søknadsdata) på behandlingen
        val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(søknadsdata)
        val originalData = objectMapper.writeValueAsString(søknadsdata)

        mottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs(
            behandling.id,
            originalData,
            søknad,
            referanseId
        )

        log.info { "Lagret mottatte opplysninger for digital søknad referanseId=$referanseId" }
    }
}
