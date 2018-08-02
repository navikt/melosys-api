package no.nav.melosys.integrasjon.gsak;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.gsak.Fagomrade;
import no.nav.melosys.domain.gsak.PrioritetType;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave.OpprettOppgaveRequest;
import no.nav.melosys.integrasjon.gsak.mock.OppgaveMockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

//FIXME Fjernes når GSAK leverer nye REST tjenester
@Service
@Profile("mocking")
public class GsakMockService implements GsakFasade {

    private OppgaveMockRepository oppgaveRepo;

    @Autowired
    public GsakMockService(OppgaveMockRepository oppgaveRepo) {
        this.oppgaveRepo = oppgaveRepo;
    }

    @Override
    public void ferdigstillOppgave(String oppgaveId) throws TekniskException {
        Oppgave oppgave = hentOppgave(oppgaveId);
        oppgaveRepo.delete(oppgave);
    }

    @Override
    public List<Oppgave> finnUtildelteOppgaverEtterFrist(Oppgavetype oppgavetype, Tema fagområde, List<FagsakType> sakstyper, List<BehandlingType> behandlingstyper) throws IntegrasjonException {
        if (oppgavetype == Oppgavetype.BEH_SAK || oppgavetype == Oppgavetype.JFR) {
            return oppgaveRepo.find(oppgavetype, fagområde, sakstyper, behandlingstyper).stream().filter(o -> o.getAnsvarligId() == null).collect(Collectors.toList());
        } else {
            throw new  IllegalArgumentException(oppgavetype.toString());
        }
    }

    @Override
    public List<Oppgave> finnOppgaveListeMedAnsvarlig(String ansvarligId) throws IntegrasjonException {
        return oppgaveRepo.finnOppgaverMedAnsvarligID(ansvarligId);
    }

    @Override
    public List<Oppgave> finnOppgaveListeMedBruker(String brukerIdent) throws IntegrasjonException {
        return oppgaveRepo.finnOppgaverMedBruker(brukerIdent);
    }

    @Override
    public Oppgave hentOppgave(String oppgaveId) {
        return oppgaveRepo.findOne(oppgaveId);
    }

    @Override
    public void leggTilbakeOppgave(Oppgave oppgave) throws IntegrasjonException, TekniskException {
        oppgave.setAnsvarligId(null);
        oppgaveRepo.save(oppgave);
    }

    @Override
    public String opprettOppgave(OpprettOppgaveRequest request) {
        Oppgave oppgave = new Oppgave();

        if (request.getAktivTil().isPresent()) {
            oppgave.setAktivTil(request.getAktivTil().get());
        } else  {
            oppgave.setAktivTil(LocalDate.now().plusYears(1));
        }

        oppgave.setDokumentId(request.getDokumentId());
        oppgave.setFagomrade(request.getFagområde());
        oppgave.setOppgavetype(request.getOppgaveType());
        oppgave.setPrioritet(request.getPrioritetType());
        oppgave.setGsakSaksnummer(request.getSaksnummer());
        oppgave.setUnderkategori(request.getUnderkategoriKode());

        return oppgaveRepo.save(oppgave);
    }

    @Override
    public String opprettOppgave(String ansvarligID, String oppgavetype, String brukerID, String journalpostID, String saksnummer) {
        Oppgave oppgave = new Oppgave();
        oppgave.setAnsvarligId(ansvarligID);

        oppgave.setAktivTil(LocalDate.now().plusYears(1));
        oppgave.setBruker(brukerID);
        oppgave.setDokumentId(journalpostID);
        oppgave.setFagomrade(Fagomrade.MED);
        if (Oppgavetype.JFR.getKode().equals(oppgavetype)) {
            oppgave.setOppgavetype(no.nav.melosys.domain.gsak.Oppgavetype.JFR_MED);
        } else if (Oppgavetype.BEH_SAK.getKode().equals(oppgavetype)) {
            oppgave.setOppgavetype(no.nav.melosys.domain.gsak.Oppgavetype.BEH_SAK_MED);
        } else {
            throw new TekniskException(oppgavetype + " støttes ikke.");
        }
        oppgave.setPrioritet(PrioritetType.NORM_MED);
        oppgave.setGsakSaksnummer(saksnummer);

        return oppgaveRepo.save(oppgave);
    }

    @Override
    public void fjernTildeling() {
        oppgaveRepo.fjernTildeling();
    }

    @Override
    public String opprettSak(String saksnummer, BehandlingType behandlingType, String fnr) throws IntegrasjonException {
        // Sak mockes ikke
        return saksnummer;
    }

    @Override
    public void tildelOppgave(String oppgaveId, String saksbehandlerID) {
        Oppgave oppgave = hentOppgave(oppgaveId);
        oppgave.setAnsvarligId(saksbehandlerID);
        oppgaveRepo.save(oppgave);
    }

}
