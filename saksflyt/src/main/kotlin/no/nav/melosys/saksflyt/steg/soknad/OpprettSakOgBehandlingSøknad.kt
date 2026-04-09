package no.nav.melosys.saksflyt.steg.soknad

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
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
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.sak.OpprettSakRequest
import no.nav.melosys.service.sak.SkjemaSakMappingService
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

private val log = KotlinLogging.logger { }

/**
 * Saga-steg som oppretter ny fagsak og behandling fra mottatt digital søknad.
 *
 * Brukes i MELOSYS_MOTTAK_DIGITAL_SØKNAD-flyten (ny sak).
 * For eksisterende sak brukes HåndterEksisterendeSakSøknad i MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD-flyten.
 *
 * 1. Oppretter fagsak med sakstype EU/EØS og tema MEDLEMSKAP_LOVVALG
 * 2. Oppretter behandling med tema UTSENDT_ARBEIDSTAKER
 * 3. Setter AVVENT_DOK_PART hvis kun arbeidsgiver-del uten motpart
 * 4. Lagrer alle relaterte skjemaId-er i mapping-tabellen
 * 5. Lagrer mottatte opplysninger og setter behandling på prosessinstansen
 */
@Component
class OpprettSakOgBehandlingSøknad(
    private val fagsakService: FagsakService,
    private val persondataFasade: PersondataFasade,
    private val mottatteOpplysningerService: MottatteOpplysningerService,
    private val objectMapper: ObjectMapper,
    private val skjemaSakMappingService: SkjemaSakMappingService,
    private val behandlingService: BehandlingService
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.OPPRETT_SAK_OG_BEHANDLING_SØKNAD

    override fun utfør(prosessinstans: Prosessinstans) {
        val søknadsdata = prosessinstans.hentData<UtsendtArbeidstakerSkjemaM2MDto>(SØKNADSDATA)
        val skjema = søknadsdata.skjema
        val metadata = skjema.metadata as UtsendtArbeidstakerMetadata
        val fnr = skjema.fnr
        val referanseId = søknadsdata.referanseId

        log.info { "Oppretter fagsak og behandling for digital søknad, referanseId=$referanseId" }

        val aktørId = persondataFasade.hentAktørIdForIdent(fnr)

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

        log.info { "Opprettet fagsak ${fagsak.saksnummer} med behandling ${behandling.id} for digital søknad referanseId=$referanseId" }

        // Sett AVVENT_DOK_PART hvis kun arbeidsgiver-del og ingen koblet motpart
        if (metadata.skjemadel == Skjemadel.ARBEIDSGIVERS_DEL && søknadsdata.kobletSkjema == null) {
            behandling.status = Behandlingsstatus.AVVENT_DOK_PART
            behandlingService.lagre(behandling)
            log.info { "Satt behandlingsstatus til AVVENT_DOK_PART (kun arbeidsgiver-del mottatt)" }
        }

        // Lagre alle relaterte skjemaId-er i mapping-tabellen
        val alleRelatertIder = samleRelaterteSkjemaIder(søknadsdata)
        val originalData = objectMapper.writeValueAsString(søknadsdata)
        val innsendtDato = søknadsdata.innsendtTidspunkt.atZone(java.time.ZoneId.of("Europe/Oslo")).toInstant()
        skjemaSakMappingService.lagreMappinger(
            alleRelatertIder, fagsak.saksnummer,
            originalData = originalData,
            innsendtDato = innsendtDato
        )

        // Lagre mottatte opplysninger (kun periode + land)
        val søknad = ForenkletSøknadMapper.tilSoeknad(søknadsdata)
        mottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs(
            behandling.id, null, søknad, referanseId
        )

        prosessinstans.behandling = behandling
        log.info { "Lagret mottatte opplysninger for digital søknad referanseId=$referanseId" }
    }

    companion object {
        fun samleRelaterteSkjemaIder(søknadsdata: UtsendtArbeidstakerSkjemaM2MDto): Set<UUID> {
            val ider = mutableSetOf<UUID>()
            ider.add(søknadsdata.skjema.id)
            søknadsdata.kobletSkjema?.let { ider.add(it.id) }
            (søknadsdata.skjema.metadata as? UtsendtArbeidstakerMetadata)?.erstatterSkjemaId?.let { ider.add(it) }
            (søknadsdata.kobletSkjema?.metadata as? UtsendtArbeidstakerMetadata)?.erstatterSkjemaId?.let { ider.add(it) }
            søknadsdata.tidligereInnsendteSkjema.forEach { ider.add(it.id) }
            return ider
        }
    }
}
