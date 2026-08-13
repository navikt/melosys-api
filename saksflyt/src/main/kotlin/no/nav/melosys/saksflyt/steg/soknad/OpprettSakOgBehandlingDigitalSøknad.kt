package no.nav.melosys.saksflyt.steg.soknad

import tools.jackson.databind.json.JsonMapper
import mu.KotlinLogging
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.repository.DigitalSøknadSakLås
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessDataKey.DIGITAL_SØKNADSDATA
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
import org.springframework.stereotype.Component
import java.util.UUID

private val log = KotlinLogging.logger { }

/**
 * Saga-steg som oppretter ny fagsak og behandling fra mottatt digital søknad.
 *
 * Brukes i MELOSYS_MOTTAK_DIGITAL_SØKNAD-flyten (ny sak).
 * For eksisterende sak brukes HåndterEksisterendeSakDigitalSøknad.
 *
 * 1. Oppretter fagsak med sakstype EU/EØS og tema MEDLEMSKAP_LOVVALG, og BRUKER-aktør
 * 2. Synkroniserer ARBEIDSGIVER + FULLMEKTIG-aktører + kontaktopplysninger fra søknaden
 * 3. Setter AVVENT_DOK_PART hvis kun arbeidsgiver-del uten motpart
 * 4. Lagrer mottatte opplysninger og skjema-sak-mapping
 */
@Component
class OpprettSakOgBehandlingDigitalSøknad(
    private val fagsakService: FagsakService,
    private val persondataFasade: PersondataFasade,
    private val mottatteOpplysningerService: MottatteOpplysningerService,
    private val jsonMapper: JsonMapper,
    private val skjemaSakMappingService: SkjemaSakMappingService,
    private val behandlingService: BehandlingService,
    private val aktørSynkronisering: DigitalSøknadAktørSynkronisering,
    private val sakLås: DigitalSøknadSakLås,
    private val eksisterendeSakHåndterer: DigitalSøknadEksisterendeSakHåndterer
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.OPPRETT_SAK_OG_BEHANDLING_DIGITAL_SØKNAD

    override fun utfør(prosessinstans: Prosessinstans) {
        val søknadsdata = prosessinstans.hentData<UtsendtArbeidstakerSkjemaM2MDto>(DIGITAL_SØKNADSDATA)
        val skjema = søknadsdata.skjema
        val metadata = skjema.metadata
        val fnr = skjema.fnr
        val referanseId = søknadsdata.referanseId

        log.info { "Oppretter fagsak og behandling for digital søknad, referanseId=$referanseId, skjemaId=${skjema.id}" }

        val aktørId = persondataFasade.hentAktørIdForIdent(fnr)

        // Atomisk sak-resolusjon (MELOSYS-8151): consumeren avgjorde NY-flyt, men en relatert melding
        // kan ha opprettet saken i mellomtiden. Lås på aktørId og re-sjekk om saken finnes — låsen
        // holdes til dette stegets transaksjon committer (StegBehandler.utfør = REQUIRES_NEW), så
        // opprett-eller-fest + mapping skjer serielt per person og kan ikke gi duplikate saker.
        sakLås.lås(aktørId)

        val relaterteSkjemaIder = utledRelaterteSkjemaIder(prosessinstans, søknadsdata)
        val eksisterendeSaksnummer = skjemaSakMappingService.finnGyldigSaksnummerForSkjemaIder(relaterteSkjemaIder)
        if (eksisterendeSaksnummer != null) {
            log.info {
                "Sak $eksisterendeSaksnummer finnes likevel (tapte kappløpet) for skjemaId=${skjema.id} — " +
                    "fester søknaden på eksisterende sak i stedet for å opprette ny"
            }
            // opprettOppgave = false: vi står under DB-låsen, og NY-flyten har et eget
            // OPPRETT_OPPGAVE-steg senere som gjør kallet mot Oppgave-API-et utenfor låsen.
            prosessinstans.behandling =
                eksisterendeSakHåndterer.håndter(eksisterendeSaksnummer, søknadsdata, opprettOppgave = false)
            prosessinstans.setData(ProsessDataKey.DIGITAL_SØKNAD_ATTACHED_EKSISTERENDE, true)
            return
        }

        val behandlingstema = BehandlingstemaUtleder.utled(søknadsdata)

        val opprettSakRequest = OpprettSakRequest.Builder()
            .medAktørID(aktørId)
            .medSakstype(Sakstyper.EU_EOS)
            .medSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG)
            .medBehandlingstema(behandlingstema)
            .medBehandlingstype(Behandlingstyper.FØRSTEGANG)
            .medBehandlingsårsaktype(Behandlingsaarsaktyper.SØKNAD)
            .medMottaksdato(søknadsdata.innsendtTidspunkt.toLocalDate())
            .build()

        val fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest)
        val behandling = fagsak.hentAktivBehandling()

        log.info { "Opprettet fagsak ${fagsak.saksnummer} med behandling ${behandling.id} for digital søknad" }

        val aktører = DigitalSøknadAktørerMapper.utled(søknadsdata)
        aktørSynkronisering.synkroniser(fagsak, aktører)

        if (metadata.skjemadel == Skjemadel.ARBEIDSGIVERS_DEL && søknadsdata.kobletSkjema == null) {
            behandling.status = Behandlingsstatus.AVVENT_DOK_PART
            behandlingService.lagre(behandling)
            log.info { "Satt behandlingsstatus til AVVENT_DOK_PART (kun arbeidsgiver-del mottatt)" }
        }

        val søknad = DigitalSøknadMapper.tilSoeknad(søknadsdata)
        val mottatteOpplysninger = mottatteOpplysningerService.opprettSøknadDigital(
            behandling.id, null, søknad, referanseId
        )

        lagreSkjemaSakMapping(søknadsdata, fagsak, mottatteOpplysninger)

        // Reserver de øvrige relaterte skjemaId-ene mot den nye saken, slik at en relatert del som
        // prosesseres senere (også på en annen instans) fester seg på samme sak — uavhengig av
        // rekkefølge. Spesielt rot-innsendingen, som ikke selv refererer de andre delene (MELOSYS-8151).
        skjemaSakMappingService.claimRelaterteSkjemaIder(relaterteSkjemaIder - skjema.id, fagsak)

        prosessinstans.behandling = behandling
        log.info { "Lagret mottatte opplysninger for digital søknad referanseId=$referanseId" }
    }

    /**
     * Samler alle skjemaId-er som binder denne innsendingen til en eventuell eksisterende sak:
     * skjemaet selv, et eventuelt motpart-koblet skjema, og tidligere innsendte versjoner fra den
     * ferske M2M-DTO-en, union-et med de relaterte id-ene consumeren så (båret videre i prosessdata).
     * Dekker dermed både DTO-baserte koblinger (kobletSkjema/tidligere versjoner) og koblinger som
     * kun er kjent på consumer-nivå.
     */
    private fun utledRelaterteSkjemaIder(
        prosessinstans: Prosessinstans,
        søknadsdata: UtsendtArbeidstakerSkjemaM2MDto
    ): Set<UUID> =
        buildSet {
            add(søknadsdata.skjema.id)
            søknadsdata.kobletSkjema?.id?.let { add(it) }
            addAll(søknadsdata.tidligereInnsendteSkjema.map { it.id })
            addAll(prosessinstans.finnData<List<UUID>>(ProsessDataKey.DIGITAL_SØKNAD_RELATERTE_SKJEMA_IDER) ?: emptyList())
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
}
