package no.nav.melosys.service.journalforing;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.journalforing.dto.JournalfoeringDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto;
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

    @Autowired
    public JournalfoeringService(JoarkFasade joarkFasade,
                                 OppgaveService oppgaveService,
                                 ProsessinstansService prosessinstansService) {
        this.joarkFasade = joarkFasade;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
    }

    public Journalpost hentJournalpost(String journalpostID) throws FunksjonellException, IntegrasjonException {
        return joarkFasade.hentJournalpost(journalpostID);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void opprettSakOgJournalfør(JournalfoeringOpprettDto journalfoeringDto) throws FunksjonellException, TekniskException {
        log.info("{} oppretter ny sak etter journalføring av journalpost {}", SubjectHandler.getInstance().getUserID(), journalfoeringDto.getJournalpostID());

        valider(journalfoeringDto);
        validerOpprettSakFelter(journalfoeringDto);
        
        Prosessinstans prosessinstans = ProsessinstansService.lagJournalføringProsessinstans(ProsessType.JFR_NY_SAK, journalfoeringDto);

        // Land trenges av regelmodulen får å vurdere inngangsvilkår
        prosessinstans.setData(ProsessDataKey.OPPHOLDSLAND, journalfoeringDto.getFagsak().getLand());
        // Perioden trenges for å hente saksopplysninger
        prosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, journalfoeringDto.getFagsak().getSoknadsperiode());
        if (!StringUtils.isEmpty(journalfoeringDto.getArbeidsgiverID())) {
            prosessinstans.setData(ProsessDataKey.ARBEIDSGIVER, journalfoeringDto.getArbeidsgiverID());
        }

        if (!StringUtils.isEmpty(journalfoeringDto.getRepresentantID())) {
            prosessinstans.setData(ProsessDataKey.REPRESENTANT, journalfoeringDto.getRepresentantID());
        }

        if (!StringUtils.isEmpty(journalfoeringDto.getRepresentantKontaktPerson())) {
            prosessinstans.setData(ProsessDataKey.REPRESENTANT_KONTAKTPERSON, journalfoeringDto.getRepresentantKontaktPerson());
        }

        prosessinstansService.lagre(prosessinstans);
        oppgaveService.ferdigstillOppgave(journalfoeringDto.getOppgaveID());
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
        if (journalfoeringDto.getFagsak().getSoknadsperiode() == null) {
            throw new FunksjonellException("Søknadsperiode mangler");
        }
        if (journalfoeringDto.getFagsak().getSoknadsperiode().getFom() == null) {
            throw new FunksjonellException("Søknadsperiodes fra og med dato mangler");
        }
        if (journalfoeringDto.getFagsak().getSoknadsperiode().getTom() == null) {
            throw new FunksjonellException("Søknadsperiodes til og med dato mangler");
        }
        if (journalfoeringDto.getFagsak().getLand() == null || journalfoeringDto.getFagsak().getLand().isEmpty()) {
            throw new FunksjonellException("Land mangler");
        }
        if (journalfoeringDto.getFagsak().getLand().contains(null)) {
            throw new FunksjonellException("Et oppholdsland er null!");
        }
    }
}
