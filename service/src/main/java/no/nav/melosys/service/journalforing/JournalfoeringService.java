package no.nav.melosys.service.journalforing;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.journalforing.dto.*;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.Behandling.erBehandlingAvSedForespørsler;
import static no.nav.melosys.domain.Behandling.erBehandlingAvSøknad;

@Service
public class JournalfoeringService {
    private static final Logger log = LoggerFactory.getLogger(JournalfoeringService.class);

    private final JoarkFasade joarkFasade;
    private final OppgaveService oppgaveService;
    private final ProsessinstansService prosessinstansService;
    private final EessiService eessiService;
    private final FagsakService fagsakService;
    private final TpsFasade tpsFasade;

    @Autowired
    public JournalfoeringService(JoarkFasade joarkFasade,
                                 OppgaveService oppgaveService,
                                 ProsessinstansService prosessinstansService,
                                 EessiService eessiService,
                                 FagsakService fagsakService,
                                 TpsFasade tpsFasade) {
        this.joarkFasade = joarkFasade;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
        this.eessiService = eessiService;
        this.fagsakService = fagsakService;
        this.tpsFasade = tpsFasade;
    }

    public Journalpost hentJournalpost(String journalpostID) throws FunksjonellException, IntegrasjonException {
        return joarkFasade.hentJournalpost(journalpostID);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void opprettOgJournalfør(JournalfoeringOpprettDto journalfoeringDto) throws MelosysException {
        Journalpost journalpost = hentJournalpost(journalfoeringDto.getJournalpostID());

        if (journalpost.isErFerdigstilt()) {
            throw new FunksjonellException("Journalposten er allerede ferdigstilt!");
        }

        if (journalpost.mottaksKanalErEessi()) {
            validerKanOppretteSakFraSed(journalpost);
        }

        if (erBehandlingAvSedForespørsler(journalfoeringDto.getBehandlingstemaKode()) || erBehandlingAvSøknad(journalfoeringDto.getBehandlingstemaKode())){
            opprettSakOgJournalfør(journalfoeringDto);
        } else {
            throw new FunksjonellException(
                String.format("Manuell journalføring av behandlingstema %s støttes ikke", journalfoeringDto.getBehandlingstemaKode())
            );
        }

        oppgaveService.ferdigstillOppgave(journalfoeringDto.getOppgaveID());
    }

    private void validerKanOppretteSakFraSed(Journalpost journalpost) throws MelosysException {

        final MelosysEessiMelding melosysEessiMelding = eessiService.hentSedTilknyttetJournalpost(journalpost.getJournalpostId());
        validerSkalIkkeBehandlesAutomatisk(melosysEessiMelding);

        Optional<Fagsak> tilknyttetFagsak = finnSakTilknyttetSed(melosysEessiMelding);
        if (tilknyttetFagsak.isPresent()) {
             throw new FunksjonellException(String.format("RINA-sak %s er allerede tilknyttet %s", melosysEessiMelding.getRinaSaksnummer(), tilknyttetFagsak.get().getSaksnummer()));
        }
    }

    private Optional<Fagsak> finnSakTilknyttetSed(MelosysEessiMelding melosysEessiMelding) throws MelosysException {
        final Optional<Long> tilknyttetArkivsak = eessiService.finnSakForRinasaksnummer(melosysEessiMelding.getRinaSaksnummer());
        return tilknyttetArkivsak.flatMap(fagsakService::finnFagsakFraArkivsakID);
    }

    private void validerSkalIkkeBehandlesAutomatisk(MelosysEessiMelding melosysEessiMelding) throws FunksjonellException {
        if (eessiService.støtterAutomatiskBehandling(melosysEessiMelding)) {
            throw new FunksjonellException("Journalpost med id " + melosysEessiMelding.getJournalpostId() + " skal ikke journalføres manuelt");
        }
    }

    private void opprettSakOgJournalfør(JournalfoeringOpprettDto journalfoeringDto) throws MelosysException {
        log.info("{} oppretter ny sak etter journalføring av journalpost {}", SubjectHandler.getInstance().getUserID(), journalfoeringDto.getJournalpostID());

        valider(journalfoeringDto);
        if (erBehandlingAvSøknad(journalfoeringDto.getBehandlingstemaKode())) {
            validerOpprettSakForSøknadBehandlingFelter(journalfoeringDto);
        }

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_NY_SAK, journalfoeringDto);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.valueOf(journalfoeringDto.getBehandlingstemaKode()));
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, erBehandlingAvSøknad(journalfoeringDto.getBehandlingstemaKode()) ? Behandlingstyper.SOEKNAD : Behandlingstyper.SED);

        if (erBehandlingAvSøknad(journalfoeringDto.getBehandlingstemaKode())){
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

    @Transactional(rollbackFor = MelosysException.class)
    public void tilordneSakOgJournalfør(JournalfoeringTilordneDto journalfoeringDto) throws MelosysException {

        final Journalpost journalpost = joarkFasade.hentJournalpost(journalfoeringDto.getJournalpostID());
        final String saksnummer = journalfoeringDto.getSaksnummer();
        final Fagsak fagsak = fagsakService.hentFagsak(saksnummer);

        if (journalpost.mottaksKanalErEessi()) {
            validerKanTilknytteJournalpostForSedTilSak(journalpost, saksnummer);
        }

        Behandlingstyper behandlingstype =  StringUtils.isNotEmpty(journalfoeringDto.getBehandlingstypeKode())
            ? Behandlingstyper.valueOf(journalfoeringDto.getBehandlingstypeKode()) : null;

        log.info("{} knytter journalpost {} til sak {}", SubjectHandler.getInstance().getUserID(), journalfoeringDto.getJournalpostID(), saksnummer);

        valider(journalfoeringDto);
        if (StringUtils.isEmpty(journalfoeringDto.getSaksnummer())) {
            throw new FunksjonellException("Saksnummer mangler");
        } else if (behandlingstype != null) {
            if (behandlingstype != Behandlingstyper.ENDRET_PERIODE) {
                throw new FunksjonellException(behandlingstype + " er ikke en lovlig behandlingstype ved knytting av dokument til sak");
            } else if (fagsak.hentAktivBehandling() != null) {
                throw new FunksjonellException("Det finnes allerede en aktiv behandling på fagsak " + saksnummer);
            }
        }

        ProsessType prosessType;
        if (behandlingstype != null) {
            prosessType = ProsessType.JFR_NY_BEHANDLING;
        } else {
            prosessType = ProsessType.JFR_KNYTT;
        }

        Prosessinstans prosessinstans = prosessinstansService.lagJournalføringProsessinstans(prosessType, journalfoeringDto);

        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, behandlingstype);
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, saksnummer);
        prosessinstans.setData(ProsessDataKey.JFR_INGEN_VURDERING, journalfoeringDto.isIngenVurdering());

        prosessinstansService.lagre(prosessinstans);
        oppgaveService.ferdigstillOppgave(journalfoeringDto.getOppgaveID());
    }

    private void validerKanTilknytteJournalpostForSedTilSak(Journalpost journalpost, String tilknyttTilSaksnummer) throws MelosysException {
        final MelosysEessiMelding melosysEessiMelding = eessiService.hentSedTilknyttetJournalpost(journalpost.getJournalpostId());
        validerSkalIkkeBehandlesAutomatisk(melosysEessiMelding);

        Optional<Fagsak> tilknyttetFagsak = finnSakTilknyttetSed(melosysEessiMelding);
        if (tilknyttetFagsak.isPresent() && !tilknyttetFagsak.get().getSaksnummer().equals(tilknyttTilSaksnummer)) {
            throw new FunksjonellException(String.format("RINA-sak %s er allerede tilknyttet %s", melosysEessiMelding.getRinaSaksnummer(), tilknyttetFagsak.get().getSaksnummer()));
        }
    }

    private void valider(JournalfoeringDto journalfoeringDto) throws FunksjonellException {
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
        if (StringUtils.isEmpty(journalfoeringDto.getBrukerID())) {
            throw new FunksjonellException("BrukerID mangler");
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

    private void validerOpprettSakForSøknadBehandlingFelter(JournalfoeringOpprettDto journalfoeringDto) throws FunksjonellException {
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
        if (journalfoeringDto.getFagsak().getLand() == null || journalfoeringDto.getFagsak().getLand().isEmpty()) {
            throw new FunksjonellException("Land mangler");
        }
        if (journalfoeringDto.getFagsak().getLand().contains(null)) {
            throw new FunksjonellException("Et søknadsland er null!");
        }
        validerAntallLand(journalfoeringDto);
    }

    private void validerAntallLand(JournalfoeringOpprettDto journalfoeringDto) throws FunksjonellException {
        String behandlingstemaKode = journalfoeringDto.getBehandlingstemaKode();
        int antallLand = journalfoeringDto.getFagsak().getLand().size();

        if (Behandling.erBehandlingAvSøknadArbeidIFlereLand(behandlingstemaKode) && antallLand < 2) {
            throw new FunksjonellException("Det er påkrevd med to eller flere land for behandlingstema " + behandlingstemaKode);
        } else if (Behandling.erBehandlingAvSøknadUtsendtArbeidstaker(behandlingstemaKode) && antallLand != 1) {
            throw new FunksjonellException("Kun ett søknadsland er tillatt for behandlingstema " + behandlingstemaKode);
        }

    }

    @Transactional(rollbackFor = MelosysException.class)
    public void journalførSed(JournalfoeringSedDto journalfoeringSedDto) throws MelosysException {
        validerJournalfoerSed(journalfoeringSedDto);
        MelosysEessiMelding eessiMelding = eessiService.hentSedTilknyttetJournalpost(journalfoeringSedDto.getJournalpostID());
        prosessinstansService.opprettProsessinstansSedMottak(eessiMelding, tpsFasade.hentAktørIdForIdent(journalfoeringSedDto.getBrukerID()));
        oppgaveService.ferdigstillOppgave(journalfoeringSedDto.getOppgaveID());
    }

    private void validerJournalfoerSed(JournalfoeringSedDto journalfoeringSedDto) throws MelosysException {

        if(StringUtils.isEmpty(journalfoeringSedDto.getJournalpostID())) {
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

    public Optional<Behandlingstema> finnBehandlingstemaForSedTilknyttetJournalpost(String journalpostID) throws MelosysException {
        return eessiService.finnBehandlingstemaForSedTilknyttetJournalpost(journalpostID);
    }
}
