package no.nav.melosys.service.journalforing;

import java.util.List;
import java.util.Optional;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
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
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.SakstypeSakstemaKobling;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.Behandling.erBehandlingAvSedForespørsler;
import static no.nav.melosys.domain.Behandling.erBehandlingAvSøknad;
import static no.nav.melosys.domain.Fagsak.erSakstypeEøs;
import static no.nav.melosys.service.sak.SakstypeBehandlingstemaKobling.erGyldigBehandlingstemaForSakstype;

@Service
public class JournalfoeringService {
    private static final Logger log = LoggerFactory.getLogger(JournalfoeringService.class);

    private final JoarkFasade joarkFasade;
    private final ProsessinstansService prosessinstansService;
    private final EessiService eessiService;
    private final FagsakService fagsakService;
    private final PersondataFasade persondataFasade;
    private final Unleash unleash;

    public JournalfoeringService(JoarkFasade joarkFasade,
                                 ProsessinstansService prosessinstansService,
                                 EessiService eessiService,
                                 FagsakService fagsakService,
                                 PersondataFasade persondataFasade,
                                 Unleash unleash) {
        this.joarkFasade = joarkFasade;
        this.prosessinstansService = prosessinstansService;
        this.eessiService = eessiService;
        this.fagsakService = fagsakService;
        this.persondataFasade = persondataFasade;
        this.unleash = unleash;
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

        if (journalpost.mottaksKanalErEessi()) {
            validerKanOppretteSakFraSed(journalpost);
        }

        if (erBehandlingAvSedForespørsler(journalfoeringDto.getBehandlingstemaKode()) || erBehandlingAvSøknad(journalfoeringDto.getBehandlingstemaKode())) {
            opprettSakOgJournalfør(journalfoeringDto);
        } else {
            throw new FunksjonellException(
                String.format("Manuell journalføring av behandlingstema %s støttes ikke", journalfoeringDto.getBehandlingstemaKode())
            );
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

    private void opprettSakOgJournalfør(JournalfoeringOpprettDto journalfoeringDto) {
        log.info("{} oppretter ny sak etter journalføring av journalpost {}", SubjectHandler.getInstance().getUserID(), journalfoeringDto.getJournalpostID());

        valider(journalfoeringDto);

        final Sakstyper sakstype = Sakstyper.valueOf(journalfoeringDto.getFagsak().getSakstype());
        final Behandlingstema behandlingstema = Behandlingstema.valueOf(journalfoeringDto.getBehandlingstemaKode());

        if (!erGyldigBehandlingstemaForSakstype(sakstype, behandlingstema)) {
            throw new FunksjonellException("Behandlingstema " + behandlingstema + " er ikke gyldig for sakstype " + sakstype);
        }

        if (behandlingstema == Behandlingstema.ARBEID_I_UTLANDET && !unleash.isEnabled("melosys.folketrygden.mvp")) {
            throw new FunksjonellException("Kan ikke opprette ny sak med behandlingstema " + behandlingstema +
                "siden 'melosys.folketrygden.mvp' ikke er aktivert i unleash");
        }

        ProsessType prosessType;
        if (StringUtils.isNotEmpty(journalfoeringDto.getBrukerID())) {
            prosessType = ProsessType.JFR_NY_SAK_BRUKER;
        } else {
            prosessType = ProsessType.JFR_NY_SAK_VIRKSOMHET;
        }

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(prosessType, journalfoeringDto);
        prosessinstans.setData(ProsessDataKey.SAKSTYPE, sakstype);
        if (unleash.isEnabled("melosys.behandle_alle_saker")) {
            var sakstema = Sakstemaer.valueOf(journalfoeringDto.getFagsak().getSakstema());
            var behandlingstype = Behandlingstyper.valueOf(journalfoeringDto.getBehandlingstypeKode());
            prosessinstans.setData(ProsessDataKey.SAKSTEMA, sakstema);
            prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, behandlingstype);
        } else {
            prosessinstans.setData(ProsessDataKey.SAKSTEMA, SakstypeSakstemaKobling.sakstema(sakstype, behandlingstema));
            prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, erBehandlingAvSøknad(behandlingstema) ? Behandlingstyper.SOEKNAD : Behandlingstyper.SED);
        }
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, behandlingstema);

        if (erSakstypeEøs(sakstype) && erBehandlingAvSøknad(behandlingstema)) {
            validerOpprettSakForSøknadBehandlingFelter(journalfoeringDto);
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
        valider(journalfoeringDto);

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

        if (!fagsak.harMinstEnBehandlingAvType(Behandlingstyper.SOEKNAD)) return;

        throw new FunksjonellException(
            "Saker kun bestående av avsluttede behandlinger med f.eks behandlingstype SED har lov til å knytte til " +
                "eksisterende sak uten å opprette ny behandling. Denne saken inneholder en behandling med behandlingstype SOEKNAD."
        );
    }

    @Transactional
    public void journalførOgOpprettNyVurdering(JournalfoeringTilordneDto journalfoeringDto) {
        var journalpost = joarkFasade.hentJournalpost(journalfoeringDto.getJournalpostID());
        var saksnummer = journalfoeringDto.getSaksnummer();
        var fagsak = fagsakService.hentFagsak(saksnummer);
        var behandlingstype = Behandlingstyper.valueOf(journalfoeringDto.getBehandlingstypeKode());

        if (fagsak.hentAktivBehandling() != null) {
            throw new FunksjonellException("Det finnes allerede en aktiv behandling på fagsak " + saksnummer);
        }
        if (journalpost.mottaksKanalErEessi()) {
            validerKanTilknytteJournalpostForSedTilSak(journalpost, saksnummer);
        }

        valider(journalfoeringDto);
        validerBehandlingstype(fagsak.getType(), behandlingstype);

        log.info("{} knytter journalpost {} til sak {} og lager ny vurdering", SubjectHandler.getInstance().getUserID(), journalfoeringDto.getJournalpostID(), saksnummer);

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_NY_VURDERING, journalfoeringDto);
        if (unleash.isEnabled("melosys.behandle_alle_saker")) {
            var behandlingstema = Behandlingstema.valueOf(journalfoeringDto.getBehandlingstemaKode());
            prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, behandlingstema);
        }
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, behandlingstype);
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, saksnummer);
        prosessinstans.setData(ProsessDataKey.JFR_INGEN_VURDERING, journalfoeringDto.isIngenVurdering());

        prosessinstansService.lagre(prosessinstans);
    }

    private void validerKanTilknytteJournalpostForSedTilSak(Journalpost journalpost, String tilknyttTilSaksnummer) {
        final MelosysEessiMelding melosysEessiMelding = eessiService.hentSedTilknyttetJournalpost(journalpost.getJournalpostId());
        validerSkalIkkeBehandlesAutomatisk(melosysEessiMelding);

        Optional<Fagsak> tilknyttetFagsak = finnSakTilknyttetSed(melosysEessiMelding);
        if (tilknyttetFagsak.isPresent() && !tilknyttetFagsak.get().getSaksnummer().equals(tilknyttTilSaksnummer)) {
            throw new FunksjonellException(String.format("RINA-sak %s er allerede tilknyttet %s", melosysEessiMelding.getRinaSaksnummer(), tilknyttetFagsak.get().getSaksnummer()));
        }
    }

    private void validerBehandlingstype(Sakstyper sakstype, Behandlingstyper behandlingstype) {
        if (sakstype == Sakstyper.EU_EOS && erUgyldigBehandlingstypeForEuEøs(behandlingstype)) {
            throw new FunksjonellException(behandlingstype + " er ikke en lovlig behandlingstype ved knytting av dokument til sak");
        }
        if (sakstype == Sakstyper.TRYGDEAVTALE && behandlingstype != Behandlingstyper.NY_VURDERING) {
            throw new FunksjonellException(behandlingstype + " er ikke en lovlig behandlingstype ved knytting av dokument til sak");
        }
    }

    private boolean erUgyldigBehandlingstypeForEuEøs(Behandlingstyper behandlingstype) {
        return behandlingstype != Behandlingstyper.ENDRET_PERIODE && behandlingstype != Behandlingstyper.NY_VURDERING;
    }

    private void valider(JournalfoeringDto journalfoeringDto) {
        if (StringUtils.isEmpty(journalfoeringDto.getJournalpostID())) {
            throw new FunksjonellException("JournalpostID mangler");
        }
        if (StringUtils.isEmpty(journalfoeringDto.getOppgaveID())) {
            throw new FunksjonellException("OppgaveID mangler");
        }
        if (StringUtils.isEmpty(journalfoeringDto.getAvsenderID())) {
            throw new FunksjonellException("AvsenderID mangler");
        }
        if (StringUtils.isEmpty(journalfoeringDto.getAvsenderNavn())) {
            throw new FunksjonellException("AvsenderNavn mangler");
        }
        if (journalfoeringDto.getAvsenderType() == null) {
            throw new FunksjonellException("AvsenderType mangler");
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

    private void validerOpprettSakForSøknadBehandlingFelter(JournalfoeringOpprettDto journalfoeringDto) {
        if (journalfoeringDto.getFagsak() == null) {
            throw new FunksjonellException("Opplysninger for å opprette en søknad mangler");
        }
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
}
