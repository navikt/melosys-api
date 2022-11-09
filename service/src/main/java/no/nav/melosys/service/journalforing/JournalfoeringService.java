package no.nav.melosys.service.journalforing;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Enums;
import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.journalforing.dto.*;
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.SakstypeSakstemaKobling;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.Behandling.erBehandlingAvSedForespørsler;
import static no.nav.melosys.domain.Behandling.erBehandlingAvSøknadGammel;
import static no.nav.melosys.domain.Fagsak.erSakstypeEøs;
import static no.nav.melosys.domain.kodeverk.Sakstemaer.MEDLEMSKAP_LOVVALG;
import static no.nav.melosys.domain.kodeverk.Sakstemaer.TRYGDEAVGIFT;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;
import static no.nav.melosys.service.sak.SakstypeBehandlingstemaKobling.erGyldigBehandlingstemaForSakstype;

@Service
public class JournalfoeringService {
    private static final Logger log = LoggerFactory.getLogger(JournalfoeringService.class);

    private final JoarkFasade joarkFasade;
    private final ProsessinstansService prosessinstansService;
    private final EessiService eessiService;
    private final FagsakService fagsakService;
    private final PersondataFasade persondataFasade;
    private final LovligeKombinasjonerService lovligeKombinasjonerService;
    private final Unleash unleash;
    private final SaksbehandlingRegler saksbehandlingRegler;

    public JournalfoeringService(JoarkFasade joarkFasade,
                                 ProsessinstansService prosessinstansService,
                                 EessiService eessiService,
                                 FagsakService fagsakService,
                                 PersondataFasade persondataFasade,
                                 LovligeKombinasjonerService lovligeKombinasjonerService,
                                 Unleash unleash,
                                 SaksbehandlingRegler saksbehandlingRegler) {
        this.joarkFasade = joarkFasade;
        this.prosessinstansService = prosessinstansService;
        this.eessiService = eessiService;
        this.fagsakService = fagsakService;
        this.persondataFasade = persondataFasade;
        this.lovligeKombinasjonerService = lovligeKombinasjonerService;
        this.unleash = unleash;
        this.saksbehandlingRegler = saksbehandlingRegler;
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

        var behandleAlleSakerToggleEnabled = unleash.isEnabled("melosys.behandle_alle_saker");
        if (behandleAlleSakerToggleEnabled && journalfoeringDto.skalSendeForvaltningsmelding()) {
            validerKanSendeForvaltningsmelding(journalfoeringDto);
        }

        if (journalpost.mottaksKanalErEessi()) {
            validerKanOppretteSakFraSed(journalpost);
        }

        fellesValidering(journalfoeringDto);

        final var sakstype = Sakstyper.valueOf(journalfoeringDto.getFagsak().getSakstype());
        final var sakstema = behandleAlleSakerToggleEnabled ? Sakstemaer.valueOf(journalfoeringDto.getFagsak().getSakstema()) : null;
        final var behandlingstema = Behandlingstema.valueOf(journalfoeringDto.getBehandlingstemaKode());
        final var behandlingstype = behandleAlleSakerToggleEnabled ? Behandlingstyper.valueOf(journalfoeringDto.getBehandlingstypeKode()) : null;

        validerOpprettelseSak(journalfoeringDto, behandleAlleSakerToggleEnabled, sakstype, sakstema, behandlingstema,
            behandlingstype);

        opprettJournalføringNySakProsessinstans(journalpost, journalfoeringDto, sakstype, sakstema, behandlingstema,
            behandlingstype);
    }

    private void validerOpprettelseSak(JournalfoeringOpprettDto journalfoeringDto, boolean behandleAlleSakerToggleEnabled,
                                       Sakstyper sakstype, Sakstemaer sakstema, Behandlingstema behandlingstema,
                                       Behandlingstyper behandlingstype) {
        if (behandleAlleSakerToggleEnabled) {
            Aktoersroller hovedpart = journalføringGjelder(journalfoeringDto);

            lovligeKombinasjonerService.validerBehandlingstema(hovedpart, sakstype, sakstema, behandlingstema, null);
            lovligeKombinasjonerService.validerBehandlingstype(hovedpart, sakstype, sakstema, behandlingstema,
                behandlingstype, null);
            if (journalfoeringDto.getAvsenderType() == Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET) {
                validerSakstypeForTrygdemyndighet(sakstype, journalfoeringDto.getAvsenderID());
            }
        } else {
            if (!erBehandlingAvSedForespørsler(behandlingstema) && !erBehandlingAvSøknadGammel(behandlingstema)) {
                throw new FunksjonellException(
                    String.format("Manuell journalføring av behandlingstema %s støttes ikke", journalfoeringDto.getBehandlingstemaKode())
                );
            }
            if (!erGyldigBehandlingstemaForSakstype(sakstype, behandlingstema)) {
                throw new FunksjonellException("Behandlingstema " + behandlingstema + " er ikke gyldig for sakstype " + sakstype);
            }

            if (behandlingstema == Behandlingstema.ARBEID_I_UTLANDET && !unleash.isEnabled("melosys.folketrygden.mvp")) {
                throw new FunksjonellException("Kan ikke opprette ny sak med behandlingstema " + behandlingstema +
                    "siden 'melosys.folketrygden.mvp' ikke er aktivert i unleash");
            }
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
        boolean manglerForventetTypeEllerTema = !FØRSTEGANG.name().equals(journalfoeringDto.getBehandlingstypeKode())
            || !MEDLEMSKAP_LOVVALG.name().equals(journalfoeringDto.getFagsak().getSakstema());

        if (manglerForventetTypeEllerTema) {
            throw new FunksjonellException("Kan kun sende forvaltningsmelding for behandlingtype: " +
                "FØRSTEGANG og sakstema: MEDLEMSKAP_LOVVALG");
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

    private void opprettJournalføringNySakProsessinstans(Journalpost journalpost, JournalfoeringOpprettDto journalfoeringDto, Sakstyper sakstype,
                                                         Sakstemaer sakstema, Behandlingstema behandlingstema,
                                                         Behandlingstyper behandlingstype) {
        var behandleAlleSakerToggleEnabled = unleash.isEnabled("melosys.behandle_alle_saker");
        ProsessType prosessType;
        if (StringUtils.isNotEmpty(journalfoeringDto.getBrukerID())) {
            prosessType = ProsessType.JFR_NY_SAK_BRUKER;
        } else {
            prosessType = ProsessType.JFR_NY_SAK_VIRKSOMHET;
        }

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(prosessType,
            journalfoeringDto);
        prosessinstans.setData(ProsessDataKey.SAKSTYPE, sakstype);
        if (behandleAlleSakerToggleEnabled) {
            prosessinstans.setData(ProsessDataKey.SAKSTEMA, sakstema);
            prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, behandlingstype);
            prosessinstans.setData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, utledÅrsaktype(journalpost.mottaksKanalErEessi(), sakstema, behandlingstema, behandlingstype));
            prosessinstans.setData(ProsessDataKey.MOTTATT_DATO, LocalDate.ofInstant(journalpost.getForsendelseMottatt(), ZoneId.systemDefault()));
        } else {
            prosessinstans.setData(ProsessDataKey.SAKSTEMA, SakstypeSakstemaKobling.sakstema(sakstype, behandlingstema));
            prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandling.erBehandlingAvSøknadGammel(
                behandlingstema) ? Behandlingstyper.SOEKNAD : Behandlingstyper.SED);
        }
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, behandlingstema);

        if (behandleAlleSakerToggleEnabled
            ? erSakstypeEøs(sakstype) && !SaksbehandlingRegler.harTomFlyt(sakstype, sakstema, behandlingstype,
            behandlingstema)
            : erSakstypeEøs(sakstype) && Behandling.erBehandlingAvSøknadGammel(behandlingstema)
        ) {
            validerSøknadFelter(journalfoeringDto);
            prosessinstans.setData(ProsessDataKey.SØKNADSLAND, journalfoeringDto.getFagsak().getLand());
            prosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, journalfoeringDto.getFagsak().getSoknadsperiode());
        }

        if (StringUtils.isNotEmpty(journalfoeringDto.getArbeidsgiverID())) {
            prosessinstans.setData(ProsessDataKey.ARBEIDSGIVER, journalfoeringDto.getArbeidsgiverID());
        }

        if (StringUtils.isNotEmpty(journalfoeringDto.getRepresentantID())) {
            prosessinstans.setData(ProsessDataKey.REPRESENTANT, journalfoeringDto.getRepresentantID());
        }

        if (StringUtils.isNotEmpty(journalfoeringDto.getRepresentantKontaktPerson())) {
            prosessinstans.setData(ProsessDataKey.REPRESENTANT_KONTAKTPERSON, journalfoeringDto.getRepresentantKontaktPerson());
        }

        if (StringUtils.isNotEmpty(journalfoeringDto.getRepresentererKode())) {
            Representerer representantRepresenterer = Representerer.valueOf(journalfoeringDto.getRepresentererKode());
            prosessinstans.setData(ProsessDataKey.REPRESENTANT_REPRESENTERER, representantRepresenterer);
        }

        prosessinstansService.lagre(prosessinstans);
        log.info("Ny sak bestilt etter journalføring av journalpost {}", journalfoeringDto.getJournalpostID());
    }

    private Behandlingsaarsaktyper utledÅrsaktype(boolean erSed, Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        if (erSed) {
            return Behandlingsaarsaktyper.SED;
        }
        if (erSøknad(sakstema, behandlingstema, behandlingstype)) {
            return Behandlingsaarsaktyper.SØKNAD;
        }
        if (behandlingstype == HENVENDELSE) {
            return Behandlingsaarsaktyper.HENVENDELSE;
        }
        return Behandlingsaarsaktyper.ANNET;
    }

    private static boolean erSøknad(Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        return (sakstema == MEDLEMSKAP_LOVVALG || sakstema == TRYGDEAVGIFT) && (behandlingstema != BESLUTNING_LOVVALG_ANNET_LAND)
            && (behandlingstype == FØRSTEGANG || behandlingstype == NY_VURDERING || behandlingstype == ENDRET_PERIODE);
    }

    @Transactional
    public void journalførOgKnyttTilEksisterendeSak(List<Pair<Behandling, Journalpost>> pairs) {
        pairs.forEach(
            pair -> oppdaterOgFerdigstillJournalpost(pair.getFirst().getFagsak().getSaksnummer(), pair.getSecond()));
    }

    private void oppdaterOgFerdigstillJournalpost(String saksnummer, Journalpost journalpost) {
        JournalpostOppdatering journalpostOppdatering = new JournalpostOppdatering.Builder()
            .medSaksnummer(saksnummer)
            .build();
        joarkFasade.oppdaterOgFerdigstillJournalpost(journalpost.getJournalpostId(), journalpostOppdatering);
    }

    @Transactional
    public void journalførOgKnyttTilEksisterendeSak(JournalfoeringTilordneDto journalfoeringDto) {
        var journalpost = joarkFasade.hentJournalpost(journalfoeringDto.getJournalpostID());
        var saksnummer = journalfoeringDto.getSaksnummer();
        var fagsak = fagsakService.hentFagsak(saksnummer);

        if (journalpost.mottaksKanalErEessi()) {
            validerKanTilknytteJournalpostForSedTilSak(journalpost, saksnummer);
        }

        validerKnyttTilEksisterendeSak(fagsak);
        fellesValidering(journalfoeringDto);

        log.info("{} knytter journalpost {} til eksisterende sak {}", SubjectHandler.getInstance().getUserID(), journalfoeringDto.getJournalpostID(), saksnummer);

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_KNYTT, journalfoeringDto);
        prosessinstans.setBehandling(fagsak.hentSistAktivBehandling());
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, saksnummer);
        prosessinstans.setData(ProsessDataKey.JFR_INGEN_VURDERING, journalfoeringDto.isIngenVurdering());

        prosessinstansService.lagre(prosessinstans);
    }

    private void validerKnyttTilEksisterendeSak(Fagsak fagsak) {
        Behandling sisteBehandling = fagsak.hentSistRegistrertBehandling();
        if (sisteBehandling.erAktiv()) return;

        if (unleash.isEnabled("melosys.behandle_alle_saker")) return;

        if (!fagsak.harMinstEnBehandlingAvType(Behandlingstyper.SOEKNAD)) return;

        throw new FunksjonellException(
            "Saker kun bestående av avsluttede behandlinger med f.eks behandlingstype SED har lov til å knytte til " +
                "eksisterende sak uten å opprette ny behandling. Denne saken inneholder en behandling med behandlingstype SOEKNAD."
        );
    }

    @Transactional
    public void journalførOgOpprettAndregangsBehandling(JournalfoeringTilordneDto journalfoeringDto) {
        var behandleAlleSakerToggleEnabled = unleash.isEnabled("melosys.behandle_alle_saker");
        var journalpost = joarkFasade.hentJournalpost(journalfoeringDto.getJournalpostID());
        var saksnummer = journalfoeringDto.getSaksnummer();
        var fagsak = fagsakService.hentFagsak(saksnummer);
        var behandlingstema = behandleAlleSakerToggleEnabled ? Behandlingstema.valueOf(journalfoeringDto.getBehandlingstemaKode()) : null;
        var behandlingstype = Behandlingstyper.valueOf(journalfoeringDto.getBehandlingstypeKode());

        if (fagsak.hentAktivBehandling() != null) {
            throw new FunksjonellException("Det finnes allerede en aktiv behandling på fagsak " + saksnummer);
        }
        if (journalpost.mottaksKanalErEessi()) {
            validerKanTilknytteJournalpostForSedTilSak(journalpost, saksnummer);
        }

        fellesValidering(journalfoeringDto);
        if (behandleAlleSakerToggleEnabled) {
            Behandling sisteBehandling = fagsak.hentSistRegistrertBehandling();
            validerBehandlingstemaOgBehandlingstype(sisteBehandling, behandlingstema, behandlingstype);
        } else {
            validerBehandlingstype(fagsak.getType(), behandlingstype);
        }

        log.info("{} knytter journalpost {} til sak {} og lager ny vurdering", SubjectHandler.getInstance().getUserID(), journalfoeringDto.getJournalpostID(), saksnummer);

        ProsessType prosessTypeForAndregangsbehandling = finnProsessTypeForAndregangsbehandling(behandlingstype, behandlingstema, fagsak);

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(prosessTypeForAndregangsbehandling, journalfoeringDto);
        if (behandleAlleSakerToggleEnabled) {
            prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, behandlingstema);
            prosessinstans.setData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, utledÅrsaktype(journalpost.mottaksKanalErEessi(), fagsak.getTema(), behandlingstema, behandlingstype));
            prosessinstans.setData(ProsessDataKey.MOTTATT_DATO, LocalDate.ofInstant(journalpost.getForsendelseMottatt(), ZoneId.systemDefault()));
        }
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, behandlingstype);
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, saksnummer);
        prosessinstans.setData(ProsessDataKey.JFR_INGEN_VURDERING, journalfoeringDto.isIngenVurdering());

        prosessinstansService.lagre(prosessinstans);
    }

    private ProsessType finnProsessTypeForAndregangsbehandling(Behandlingstyper behandlingstype, Behandlingstema behandlingstema, Fagsak fagsak) {
        if (!unleash.isEnabled("melosys.behandle_alle_saker")) {
            return ProsessType.JFR_ANDREGANG_REPLIKER_BEHANDLING;
        }
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

    private void validerBehandlingstemaOgBehandlingstype(Behandling sistBehandling, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        var fagsak = sistBehandling.getFagsak();
        lovligeKombinasjonerService.validerBehandlingstema(fagsak.getHovedpartRolle(), fagsak.getType(), fagsak.getTema(), behandlingstema, sistBehandling.getTema());
        lovligeKombinasjonerService.validerBehandlingstype(fagsak.getHovedpartRolle(), fagsak.getType(), fagsak.getTema(), behandlingstema, behandlingstype, null);
    }

    private void validerBehandlingstype(Sakstyper sakstype, Behandlingstyper behandlingstype) {
        if (List.of(Sakstyper.EU_EOS, Sakstyper.TRYGDEAVTALE).contains(sakstype)
            && behandlingstype != Behandlingstyper.NY_VURDERING
        ) {
            throw new FunksjonellException(behandlingstype + " er ikke en lovlig behandlingstype ved knytting av dokument til sak");
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
