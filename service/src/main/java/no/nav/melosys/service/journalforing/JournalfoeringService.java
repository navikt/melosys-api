package no.nav.melosys.service.journalforing;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import com.google.common.base.Enums;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.saksflytapi.domain.ProsessType;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.journalforing.dto.*;
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import static no.nav.melosys.domain.Fagsak.erSakstypeEøs;
import static no.nav.melosys.domain.kodeverk.Sakstemaer.MEDLEMSKAP_LOVVALG;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.FØRSTEGANG;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.NY_VURDERING;
import static no.nav.melosys.service.journalforing.UtledBehandlingsaarsak.utledÅrsaktype;

@Service
public class JournalfoeringService {
    private static final Logger log = LoggerFactory.getLogger(JournalfoeringService.class);

    private final JoarkFasade joarkFasade;
    private final ProsessinstansService prosessinstansService;
    private final EessiService eessiService;
    private final FagsakService fagsakService;
    private final PersondataFasade persondataFasade;
    private final LovligeKombinasjonerService lovligeKombinasjonerService;
    private final SaksbehandlingRegler saksbehandlingRegler;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;

    private final UtenlandskMyndighetService utenlandskMyndighetService;

    public JournalfoeringService(JoarkFasade joarkFasade,
                                 ProsessinstansService prosessinstansService,
                                 EessiService eessiService,
                                 FagsakService fagsakService,
                                 PersondataFasade persondataFasade,
                                 LovligeKombinasjonerService lovligeKombinasjonerService,
                                 SaksbehandlingRegler saksbehandlingRegler,
                                 BehandlingService behandlingService,
                                 BehandlingsresultatService behandlingsresultatService, UtenlandskMyndighetService utenlandskMyndighetService) {
        this.joarkFasade = joarkFasade;
        this.prosessinstansService = prosessinstansService;
        this.eessiService = eessiService;
        this.fagsakService = fagsakService;
        this.persondataFasade = persondataFasade;
        this.lovligeKombinasjonerService = lovligeKombinasjonerService;
        this.saksbehandlingRegler = saksbehandlingRegler;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    public Journalpost hentJournalpost(String journalpostID) {
        return joarkFasade.hentJournalpost(journalpostID);
    }

    public Optional<String> finnHovedpartIdent(Journalpost journalpost) {
        if (journalpost.getBrukerIdType() == null || journalpost.getBrukerId() == null) {
            return Optional.empty();
        }
        return switch (journalpost.getBrukerIdType()) {
            case FOLKEREGISTERIDENT, ORGNR -> Optional.of(journalpost.getBrukerId());
            case AKTØR_ID -> Optional.of(persondataFasade.hentFolkeregisterident(journalpost.getBrukerId()));
        };
    }

    @Transactional
    public void journalførOgOpprettSak(JournalfoeringOpprettDto journalfoeringDto) {
        Journalpost journalpost = hentJournalpost(journalfoeringDto.getJournalpostID());

        if (journalpost.isErFerdigstilt()) {
            throw new FunksjonellException("Journalposten er allerede ferdigstilt!");
        }

        if (journalfoeringDto.skalSendeForvaltningsmelding()) {
            validerKanSendeForvaltningsmelding(journalfoeringDto);
        }

        if (journalpost.mottaksKanalErEessi()) {
            validerKanOppretteSakFraSed(journalpost);
        }

        fellesValidering(journalfoeringDto);
        validerOpprettelseSak(journalfoeringDto);

        ProsessType prosessType;
        if (StringUtils.isNotEmpty(journalfoeringDto.getBrukerID())) {
            prosessType = ProsessType.JFR_NY_SAK_BRUKER;
        } else {
            prosessType = ProsessType.JFR_NY_SAK_VIRKSOMHET;
        }

        final var sakstype = Sakstyper.valueOf(journalfoeringDto.getFagsak().getSakstype());
        final var sakstema = Sakstemaer.valueOf(journalfoeringDto.getFagsak().getSakstema());
        final var behandlingstema = Behandlingstema.valueOf(journalfoeringDto.getBehandlingstemaKode());
        final var behandlingstype = Behandlingstyper.valueOf(journalfoeringDto.getBehandlingstypeKode());

        boolean skalSetteSøknadslandOgPeriode = skalSetteSøknadslandOgPeriode(sakstype, sakstema, behandlingstema, behandlingstype);
        if (skalSetteSøknadslandOgPeriode) {
            validerSøknadFelter(journalfoeringDto);
        }
        LocalDate mottaksdato = utledMottaksdato(journalfoeringDto.getMottattDato(), journalpost);
        Behandlingsaarsaktyper behandlingsaarsaktyper = utledÅrsaktype(journalpost, sakstema, behandlingstema, behandlingstype);

        prosessinstansService.opprettProsessinstansJournalføringNySak(journalfoeringDto.tilJournalfoeringOpprettRequest(), prosessType,
            skalSetteSøknadslandOgPeriode, mottaksdato, behandlingsaarsaktyper, finnInstitusjonIdEllerNull(journalfoeringDto.getAvsenderID()));

        log.info("Ny sak bestilt etter journalføring av journalpost {}", journalfoeringDto.getJournalpostID());
    }

    private void validerOpprettelseSak(JournalfoeringOpprettDto journalfoeringDto) {

        var sakstype = Sakstyper.valueOf(journalfoeringDto.getFagsak().getSakstype());
        var sakstema = Sakstemaer.valueOf(journalfoeringDto.getFagsak().getSakstema());
        var behandlingstema = Behandlingstema.valueOf(journalfoeringDto.getBehandlingstemaKode());
        var behandlingstype = Behandlingstyper.valueOf(journalfoeringDto.getBehandlingstypeKode());
        Aktoersroller hovedpart = journalføringGjelder(journalfoeringDto);

        lovligeKombinasjonerService.validerOpprettelseOgEndring(
            hovedpart, sakstype, sakstema, behandlingstema, behandlingstype);
        if (journalfoeringDto.getAvsenderType() == Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET) {
            validerSakstypeForTrygdemyndighet(sakstype, journalfoeringDto.getAvsenderID());
        }
        if (StringUtils.isNotEmpty(journalfoeringDto.getFullmektigID()) && CollectionUtils.isEmpty(journalfoeringDto.getFullmakter())) {
            throw new FunksjonellException("Fullmektig har ingen fullmakter");
        }
    }

    private static void validerSakstypeForTrygdemyndighet(Sakstyper sakstype,
                                                          String landkode) {
        boolean erEuEllerEøsLand = Enums.getIfPresent(Landkoder.class, landkode).isPresent();
        boolean erAvtaleland = Enums.getIfPresent(Trygdeavtale_myndighetsland.class, landkode).isPresent();

        if (erEuEllerEøsLand && !erAvtaleland && sakstype != Sakstyper.EU_EOS) {
            throw new FunksjonellException(
                "Sak for trygdemyndighet fra %s skal være av type %s".formatted(landkode, Sakstyper.EU_EOS));
        }
        if (erAvtaleland && !erEuEllerEøsLand && sakstype != Sakstyper.TRYGDEAVTALE) {
            throw new FunksjonellException(
                "Sak for trygdemyndighet fra %s skal være av type %s".formatted(landkode, Sakstyper.TRYGDEAVTALE));
        }
    }

    private void validerKanSendeForvaltningsmelding(JournalfoeringOpprettDto journalfoeringDto) {
        String behandlingstype = journalfoeringDto.getBehandlingstypeKode();
        String sakstema = journalfoeringDto.getFagsak().getSakstema();
        boolean manglerForventetTypeEllerTema =
            !((behandlingstype.equals(FØRSTEGANG.name()) || behandlingstype.equals(NY_VURDERING.name())) && sakstema.equals(MEDLEMSKAP_LOVVALG.name()));

        if (manglerForventetTypeEllerTema) {
            throw new FunksjonellException("Kan kun sende forvaltningsmelding for behandlingtyper: " +
                "FØRSTEGANG og NY_VURDERING og sakstema: MEDLEMSKAP_LOVVALG");
        }

        if (!journalføringGjelderBruker(journalfoeringDto)) {
            throw new FunksjonellException("Kan kun sende forvaltningsmelding for Aktoersroller: " +
                "BRUKER");
        }
    }

    private void validerKanOppretteSakFraSed(Journalpost journalpost) {
        final MelosysEessiMelding melosysEessiMelding = eessiService.hentSedTilknyttetJournalpost(journalpost.getJournalpostId());
        validerSkalIkkeBehandlesAutomatisk(melosysEessiMelding);

        Optional<Fagsak> tilknyttetFagsak = finnSakTilknyttetSed(melosysEessiMelding);
        if (tilknyttetFagsak.isPresent()) {
            throw new FunksjonellException(String.format("RINA-sak %s er allerede tilknyttet %s", melosysEessiMelding.getRinaSaksnummer(), tilknyttetFagsak.get().getSaksnummer()));
        }
    }

    public Optional<Fagsak> finnSakTilknyttetSedJournalpost(Journalpost journalpost) {
        if (!journalpost.mottaksKanalErEessi()) {
            return Optional.empty();
        }
        return finnSakTilknyttetSed(eessiService.hentSedTilknyttetJournalpost(journalpost.getJournalpostId()));
    }

    private Optional<Fagsak> finnSakTilknyttetSed(MelosysEessiMelding melosysEessiMelding) {
        final Optional<Long> tilknyttetArkivsak = eessiService.finnSakForRinasaksnummer(melosysEessiMelding.getRinaSaksnummer());
        return tilknyttetArkivsak.flatMap(fagsakService::finnFagsakFraArkivsakID);
    }

    private void validerSkalIkkeBehandlesAutomatisk(MelosysEessiMelding melosysEessiMelding) {
        if (eessiService.støtterAutomatiskBehandling(melosysEessiMelding)) {
            throw new FunksjonellException("Journalpost med id " + melosysEessiMelding.getJournalpostId() + " skal ikke journalføres manuelt");
        }
    }

    @Transactional
    public void journalførOgKnyttTilEksisterendeSak(JournalfoeringTilordneDto journalfoeringDto) {
        var journalpost = joarkFasade.hentJournalpost(journalfoeringDto.getJournalpostID());
        var saksnummer = journalfoeringDto.getSaksnummer();
        var fagsak = fagsakService.hentFagsak(saksnummer);

        if (journalpost.mottaksKanalErEessi()) {
            validerKanTilknytteJournalpostForSedTilSak(journalpost, saksnummer);
        }

        fellesValidering(journalfoeringDto);

        log.info("{} knytter journalpost {} til eksisterende sak {}", SubjectHandler.getInstance().getUserID(), journalfoeringDto.getJournalpostID(), saksnummer);

        prosessinstansService.opprettProsessinstansJournalføringKnyttTilEksisterende(journalfoeringDto.tilJournalfoeringTilordneRequest(), saksnummer, fagsak, finnInstitusjonIdEllerNull(journalfoeringDto.getAvsenderID()));
    }

    private String finnInstitusjonIdEllerNull(String avsenderID) {
        return utenlandskMyndighetService.finnInstitusjonID(avsenderID).orElse(null);
    }


    @Transactional
    public void journalførOgOpprettAndregangsBehandling(JournalfoeringTilordneDto journalfoeringDto) {
        var journalpost = joarkFasade.hentJournalpost(journalfoeringDto.getJournalpostID());
        var saksnummer = journalfoeringDto.getSaksnummer();
        var fagsak = fagsakService.hentFagsak(saksnummer);
        var behandlingstema = Behandlingstema.valueOf(journalfoeringDto.getBehandlingstemaKode());
        var behandlingstype = Behandlingstyper.valueOf(journalfoeringDto.getBehandlingstypeKode());
        final var sistBehandling = fagsak.hentSistRegistrertBehandling();
        final var sistBehandlingsresultat = behandlingsresultatService.hentBehandlingsresultatMedAnmodningsperioder(sistBehandling.getId());

        if (fagsak.hentAktivBehandling() != null && (sistBehandlingsresultat.erIkkeArtikkel16MedSendtAnmodningOmUnntak())) {
            throw new FunksjonellException("Det finnes allerede en aktiv behandling på fagsak " + saksnummer);
        }
        if (journalpost.mottaksKanalErEessi()) {
            validerKanTilknytteJournalpostForSedTilSak(journalpost, saksnummer);
        }

        fellesValidering(journalfoeringDto);
        lovligeKombinasjonerService.validerBehandlingstemaOgBehandlingstypeForAndregangsbehandling(fagsak, sistBehandling, sistBehandlingsresultat, behandlingstema, behandlingstype);

        if (sistBehandling.erAktiv()) {
            behandlingService.avsluttBehandling(sistBehandling.getId());
        }


        log.info("{} knytter journalpost {} til sak {} og lager ny vurdering", SubjectHandler.getInstance().getUserID(), journalfoeringDto.getJournalpostID(), saksnummer);

        ProsessType prosessTypeForAndregangsbehandling = finnProsessTypeForAndregangsbehandling(behandlingstype, behandlingstema, fagsak);
        Behandlingsaarsaktyper behandlingsaarsaktyper = utledÅrsaktype(journalpost, fagsak.getTema(), behandlingstema, behandlingstype);
        LocalDate mottaksdato = utledMottaksdato(journalfoeringDto.getMottattDato(), journalpost);

        prosessinstansService.journalførOgOpprettAndregangsBehandling(prosessTypeForAndregangsbehandling, behandlingstema, behandlingstype, journalfoeringDto.tilJournalfoeringTilordneRequest(),
            behandlingsaarsaktyper, mottaksdato, finnInstitusjonIdEllerNull(journalfoeringDto.getAvsenderID()));
    }

    private static LocalDate utledMottaksdato(LocalDate datoFraSaksbehandler, Journalpost journalpost) {
        return datoFraSaksbehandler != null ? datoFraSaksbehandler : LocalDate.ofInstant(journalpost.getForsendelseMottatt(), ZoneId.systemDefault());
    }

    private boolean skalSetteSøknadslandOgPeriode(Sakstyper sakstype, Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        return erSakstypeEøs(sakstype)
            && !saksbehandlingRegler.harIngenFlyt(sakstype, sakstema, behandlingstype, behandlingstema)
            && !saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(sakstype, sakstema, behandlingstema)
            && !saksbehandlingRegler.harIkkeYrkesaktivFlyt(sakstype, behandlingstema);
    }

    private ProsessType finnProsessTypeForAndregangsbehandling(Behandlingstyper behandlingstype, Behandlingstema behandlingstema, Fagsak fagsak) {
        if (saksbehandlingRegler.skalTidligereBehandlingReplikeres(fagsak, behandlingstype, behandlingstema)) {
            return ProsessType.JFR_ANDREGANG_REPLIKER_BEHANDLING;
        }
        return ProsessType.JFR_ANDREGANG_NY_BEHANDLING;
    }

    private void validerKanTilknytteJournalpostForSedTilSak(Journalpost journalpost, String tilknyttTilSaksnummer) {
        final MelosysEessiMelding melosysEessiMelding = eessiService.hentSedTilknyttetJournalpost(journalpost.getJournalpostId());
        validerSkalIkkeBehandlesAutomatisk(melosysEessiMelding);

        Optional<Fagsak> tilknyttetFagsak = finnSakTilknyttetSed(melosysEessiMelding);
        if (tilknyttetFagsak.isPresent() && !tilknyttetFagsak.get().getSaksnummer().equals(tilknyttTilSaksnummer)) {
            throw new FunksjonellException(String.format("RINA-sak %s er allerede tilknyttet %s", melosysEessiMelding.getRinaSaksnummer(), tilknyttetFagsak.get().getSaksnummer()));
        }
    }

    private void fellesValidering(JournalfoeringDto journalfoeringDto) {
        if (journalfoeringDto instanceof JournalfoeringOpprettDto journalfoeringOpprettDto
            && journalfoeringOpprettDto.getFagsak() == null) {
            throw new FunksjonellException("Opplysninger for å opprette en søknad mangler");
        }

        if (StringUtils.isEmpty(journalfoeringDto.getJournalpostID())) {
            throw new FunksjonellException("JournalpostID mangler");
        }
        if (StringUtils.isEmpty(journalfoeringDto.getOppgaveID())) {
            throw new FunksjonellException("OppgaveID mangler");
        }
        if (journalfoeringDto.getAvsenderType() != null && StringUtils.isEmpty(journalfoeringDto.getAvsenderID())) {
            throw new FunksjonellException("AvsenderID er påkrevd når AvsenderType er satt");
        }
        if (!StringUtils.isEmpty(journalfoeringDto.getAvsenderID()) && journalfoeringDto.getAvsenderType() == null) {
            throw new FunksjonellException("AvsenderType er påkrevd når AvsenderID er satt");
        }
        if (StringUtils.isEmpty(journalfoeringDto.getAvsenderNavn())) {
            throw new FunksjonellException("AvsenderNavn mangler");
        }
        if (StringUtils.isEmpty(journalfoeringDto.getBrukerID()) && StringUtils.isEmpty(journalfoeringDto.getVirksomhetOrgnr())) {
            throw new FunksjonellException("Både BrukerID og VirksomhetOrgnr mangler. Krever én");
        }
        if (StringUtils.isNotEmpty(journalfoeringDto.getBrukerID()) && StringUtils.isNotEmpty(journalfoeringDto.getVirksomhetOrgnr())) {
            throw new FunksjonellException("Både BrukerID og VirksomhetOrgnr finnes. Dette kan skape problemer. Velg én å journalføre dokumentet på.");
        }
        if (StringUtils.isEmpty(journalfoeringDto.getHoveddokument().getDokumentID())) {
            throw new FunksjonellException("DokumentID til hoveddokument mangler");
        }
        if (StringUtils.isEmpty(journalfoeringDto.getHoveddokument().getTittel())) {
            throw new FunksjonellException("Hoveddokument mangler tittel");
        }
        if (journalfoeringDto.getVedlegg().stream().map(DokumentDto::getDokumentID).anyMatch(StringUtils::isEmpty)) {
            throw new FunksjonellException("DokumentID mangler for et vedlegg");
        }
        if (journalfoeringDto.getVedlegg().stream().map(DokumentDto::getTittel).anyMatch(StringUtils::isEmpty)) {
            throw new FunksjonellException("Tittel mangler for et vedlegg");
        }
    }

    private void validerSøknadFelter(JournalfoeringOpprettDto journalfoeringDto) {
        final PeriodeDto søknadsperiode = journalfoeringDto.getFagsak().getSoknadsperiode();
        if (søknadsperiode == null) {
            throw new FunksjonellException("Søknadsperiode mangler");
        }
        if (søknadsperiode.getFom() == null) {
            throw new FunksjonellException("Søknadsperiodes fra og med dato mangler");
        }
        if (søknadsperiode.getTom() != null && søknadsperiode.getFom().isAfter(søknadsperiode.getTom())) {
            throw new FunksjonellException("Fra og med dato kan ikke være etter til og med dato.");
        }
        if (!journalfoeringDto.getFagsak().getLand().erGyldig()) {
            throw new FunksjonellException("Informasjon om land er ugyldig");
        }
        if (journalfoeringDto.getFagsak().getLand().getLandkoder().contains(null)) {
            throw new FunksjonellException("Et søknadsland er null!");
        }
        validerAntallLand(journalfoeringDto);
    }

    private void validerAntallLand(JournalfoeringOpprettDto journalfoeringDto) {
        String behandlingstemaKode = journalfoeringDto.getBehandlingstemaKode();
        int antallLand = journalfoeringDto.getFagsak().getLand().getLandkoder().size();
        boolean erUkjenteEllerAlleEosLand = journalfoeringDto.getFagsak().getLand().erUkjenteEllerAlleEosLand();

        if (Behandling.erBehandlingAvSøknadArbeidIFlereLand(behandlingstemaKode)) {
            if (erUkjenteEllerAlleEosLand && antallLand != 0) {
                throw new FunksjonellException(String.format("Det kan ikke være noen land for behandlingstema %s om ukjenteEllerAlleEosLand er valgt", behandlingstemaKode));
            } else if (!erUkjenteEllerAlleEosLand && antallLand < 2) {
                throw new FunksjonellException(String.format("Det er påkrevd med to eller flere land for behandlingstema %s om ikke ukjenteEllerAlleEosLand er valgt", behandlingstemaKode));
            }
        } else if (Behandling.erBehandlingAvSøknadUtsendtArbeidstaker(behandlingstemaKode) && antallLand != 1) {
            throw new FunksjonellException("Kun ett søknadsland er tillatt for behandlingstema " + behandlingstemaKode);
        }

    }

    @Transactional
    public void journalførSed(JournalfoeringSedDto journalfoeringSedDto) {
        validerJournalfoerSed(journalfoeringSedDto);
        MelosysEessiMelding eessiMelding = eessiService.hentSedTilknyttetJournalpost(journalfoeringSedDto.getJournalpostID());
        prosessinstansService.opprettProsessinstansSedMottak(eessiMelding, persondataFasade.hentAktørIdForIdent(journalfoeringSedDto.getBrukerID()));
    }

    private void validerJournalfoerSed(JournalfoeringSedDto journalfoeringSedDto) {

        if (StringUtils.isEmpty(journalfoeringSedDto.getJournalpostID())) {
            throw new FunksjonellException("JournalpostID er påkrevd!");
        } else if (StringUtils.isEmpty(journalfoeringSedDto.getBrukerID())) {
            throw new FunksjonellException("BrukerID er påkrevd!");
        } else if (StringUtils.isEmpty(journalfoeringSedDto.getOppgaveID())) {
            throw new FunksjonellException("OppgaveID er påkrevd!");
        } else if (!eessiService.støtterAutomatiskBehandling(journalfoeringSedDto.getJournalpostID())) {
            throw new FunksjonellException("Sed tilknyttet journalpost " + journalfoeringSedDto.getJournalpostID()
                + " støtter ikke automatisk behandling!");
        }
    }

    public Optional<Behandlingstema> finnBehandlingstemaForSedTilknyttetJournalpost(String journalpostID) {
        return eessiService.finnBehandlingstemaForSedTilknyttetJournalpost(journalpostID);
    }

    private boolean journalføringGjelderBruker(JournalfoeringOpprettDto journalfoeringDto) {
        return Aktoersroller.BRUKER.equals(journalføringGjelder(journalfoeringDto));
    }

    private Aktoersroller journalføringGjelder(JournalfoeringOpprettDto journalfoeringDto) {
        return journalfoeringDto.getBrukerID() != null ? Aktoersroller.BRUKER : Aktoersroller.VIRKSOMHET;
    }
}
