package no.nav.melosys.service.journalforing;

import java.util.Optional;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.journalforing.dto.*;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JournalfoeringService {

    private static final Logger log = LoggerFactory.getLogger(JournalfoeringService.class);

    private final JoarkFasade joarkFasade;

    private final OppgaveService oppgaveService;

    private final ProsessinstansService prosessinstansService;

    private final EessiService eessiService;

    @Autowired
    public JournalfoeringService(JoarkFasade joarkFasade,
                                 OppgaveService oppgaveService,
                                 ProsessinstansService prosessinstansService, EessiService eessiService) {
        this.joarkFasade = joarkFasade;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
        this.eessiService = eessiService;
    }

    public Journalpost hentJournalpost(String journalpostID) throws FunksjonellException, IntegrasjonException {
        return joarkFasade.hentJournalpost(journalpostID);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void opprettOgJournalfør(JournalfoeringOpprettDto journalfoeringDto) throws MelosysException {
        Journalpost journalpost = hentJournalpost(journalfoeringDto.getJournalpostID());

        if (journalpost.mottaksKanalErEessi() && eessiService.støtterAutomatiskBehandling(journalfoeringDto.getJournalpostID())) {
            opprettProsessinstansSedMottak(journalfoeringDto);
        } else if (Behandlingstyper.ANMODNING_OM_UNNTAK_HOVEDREGEL.getKode().equalsIgnoreCase(journalfoeringDto.getBehandlingstypeKode())) {
            opprettProsessinstansBrevAouMottak(journalfoeringDto);
        } else {
            opprettSakOgJournalfør(journalfoeringDto);
        }

        oppgaveService.ferdigstillOppgave(journalfoeringDto.getOppgaveID());
    }

    private void opprettSakOgJournalfør(JournalfoeringOpprettDto journalfoeringDto) throws MelosysException {
        log.info("{} oppretter ny sak etter journalføring av journalpost {}", SubjectHandler.getInstance().getUserID(), journalfoeringDto.getJournalpostID());

        valider(journalfoeringDto);
        validerOpprettSakFelter(journalfoeringDto);

        Prosessinstans prosessinstans = ProsessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_NY_SAK, journalfoeringDto);

        // Land trenges av regelmodulen for å vurdere inngangsvilkår
        prosessinstans.setData(ProsessDataKey.SØKNADSLAND, journalfoeringDto.getFagsak().getLand());
        // Perioden trenges for å hente saksopplysninger
        prosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, journalfoeringDto.getFagsak().getSoknadsperiode());
        if (StringUtils.isNotEmpty(journalfoeringDto.getArbeidsgiverID())) {
            prosessinstans.setData(ProsessDataKey.ARBEIDSGIVER, journalfoeringDto.getArbeidsgiverID());
        }

        if (StringUtils.isNotEmpty(journalfoeringDto.getRepresentantID())) {
            prosessinstans.setData(ProsessDataKey.REPRESENTANT, journalfoeringDto.getRepresentantID());
        }

        if (StringUtils.isNotEmpty(journalfoeringDto.getRepresentantKontaktPerson())) {
            prosessinstans.setData(ProsessDataKey.REPRESENTANT_KONTAKTPERSON, journalfoeringDto.getRepresentantKontaktPerson());
        }

        prosessinstansService.lagre(prosessinstans);
    }

    private void opprettProsessinstansSedMottak(JournalfoeringOpprettDto journalfoeringDto) throws MelosysException {
        validerBrukerIDFinnes(journalfoeringDto);
        prosessinstansService.opprettProsessinstansSedMottak(journalfoeringDto.getJournalpostID(), journalfoeringDto.getBrukerID());
    }

    private void opprettProsessinstansBrevAouMottak(JournalfoeringOpprettDto journalfoeringDto) throws FunksjonellException {
        validerBrukerIDFinnes(journalfoeringDto);
        validerOpprettSakFelter(journalfoeringDto);
        validerBehandleAnmodningOmUnntakFelter(journalfoeringDto);

        FagsakDto fagsakDto = journalfoeringDto.getFagsak();
        AnmodningOmUnntakDto anmodningOmUnntakDto = journalfoeringDto.getAnmodningOmUnntak();

        Prosessinstans prosessinstans = ProsessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_AOU_BREV, journalfoeringDto);
        prosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, fagsakDto.getSoknadsperiode());
        prosessinstans.setData(ProsessDataKey.LOVVALGSLAND, fagsakDto.getLand());
        prosessinstans.setData(ProsessDataKey.LOVVALGSBESTEMMELSE, anmodningOmUnntakDto.getLovvalgsbestemmelse());
        prosessinstans.setData(ProsessDataKey.UNNTAK_FRA_LOVVALGSLAND, anmodningOmUnntakDto.getUnntakFraLovvalgsland());
        prosessinstans.setData(ProsessDataKey.UNNTAK_FRA_LOVVALGSBESTEMMELSE, anmodningOmUnntakDto.getUnntakFraLovvalgsbestemmelse());

        prosessinstans.setSteg(ProsessSteg.JFR_AOU_BREV_OPPRETT_FAGSAK_OG_BEHANDLING);
        prosessinstansService.lagre(prosessinstans);
    }

    private void validerBrukerIDFinnes(JournalfoeringOpprettDto journalfoeringDto) throws FunksjonellException {
        if (StringUtils.isEmpty(journalfoeringDto.getBrukerID())) {
            throw new FunksjonellException("BrukerID er påkrevd!");
        }
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void tilordneSakOgJournalfør(JournalfoeringTilordneDto journalfoeringDto) throws FunksjonellException, TekniskException {
        String saksnummer = journalfoeringDto.getSaksnummer();
        log.info("{} knytter journalpost {} til sak {}", SubjectHandler.getInstance().getUserID(), journalfoeringDto.getJournalpostID(), saksnummer);

        valider(journalfoeringDto);
        if (StringUtils.isEmpty(journalfoeringDto.getSaksnummer())) {
            throw new FunksjonellException("Saksnummer mangler");
        }

        Prosessinstans prosessinstans = ProsessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_KNYTT, journalfoeringDto);

        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, saksnummer);
        prosessinstans.setData(ProsessDataKey.JFR_INGEN_VURDERING, journalfoeringDto.isIngenVurdering());

        prosessinstansService.lagre(prosessinstans);
        oppgaveService.ferdigstillOppgave(journalfoeringDto.getOppgaveID());
    }

    // Denne er package-visible kun for at det skal være lettere å teste den isolert
    void valider(JournalfoeringDto journalfoeringDto) throws FunksjonellException {
        if (StringUtils.isEmpty(journalfoeringDto.getJournalpostID())) {
            throw new FunksjonellException("JournalpostID mangler");
        }
        if (StringUtils.isEmpty(journalfoeringDto.getDokumentID())) {
            throw new FunksjonellException("DokumentID mangler");
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
        if (StringUtils.isEmpty(journalfoeringDto.getBrukerID())) {
            throw new FunksjonellException("BrukerID mangler");
        }
        if (StringUtils.isEmpty(journalfoeringDto.getHoveddokumentTittel())) {
            throw new FunksjonellException("Dokumenttittel mangler");
        }
    }

    private void validerOpprettSakFelter(JournalfoeringOpprettDto journalfoeringDto) throws FunksjonellException {
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
        if (journalfoeringDto.getFagsak().getLand().size() > 1) { // Melosys støtter bare ett land i Leveranse 1.
            throw new FunksjonellException("Kun ett land er støttet i denne versjonen av Melosys");
        }
    }

    private void validerBehandleAnmodningOmUnntakFelter(JournalfoeringOpprettDto journalfoeringDto) throws FunksjonellException {
        if (journalfoeringDto.getAnmodningOmUnntak() == null) {
            throw new FunksjonellException("Opplysninger for å opprette behandling av anmodning om unntak mangler");
        }
        if (StringUtils.isEmpty(journalfoeringDto.getAnmodningOmUnntak().getLovvalgsbestemmelse())) {
            throw new FunksjonellException("Lovvalgsbestemmelse mangler");
        }
        if (StringUtils.isEmpty(journalfoeringDto.getAnmodningOmUnntak().getUnntakFraLovvalgsbestemmelse())) {
            throw new FunksjonellException("Unntak fra lovvalgsbestemmelse mangler");
        }
        if (StringUtils.isEmpty(journalfoeringDto.getAnmodningOmUnntak().getUnntakFraLovvalgsland())) {
            throw new FunksjonellException("Unntak fra lovvalgsland mangler");
        }
    }

    public void journalførSed(JournalfoeringSedDto journalfoeringSedDto) throws MelosysException {
        validerJournalfoerSed(journalfoeringSedDto);
        prosessinstansService.opprettProsessinstansSedMottak(journalfoeringSedDto.getJournalpostID(), journalfoeringSedDto.getBrukerID());
    }

    private void validerJournalfoerSed(JournalfoeringSedDto journalfoeringSedDto) throws MelosysException {

        if (!eessiService.støtterAutomatiskBehandling(journalfoeringSedDto.getJournalpostID())) {
            throw new FunksjonellException("Sed tilknyttet journalpost " + journalfoeringSedDto.getJournalpostID()
                + " støtter ikke automatisk behandling!");
        } else if (StringUtils.isEmpty(journalfoeringSedDto.getBrukerID())) {
            throw new FunksjonellException("BrukerID er påkrevd!");
        }
    }

    public Optional<Behandlingstyper> finnBehandlingstypeForSedTilknyttetJournalpost(String journalpostID) throws MelosysException {
        return eessiService.finnBehandlingstypeForSedTilknyttetJournalpost(journalpostID);
    }
}
