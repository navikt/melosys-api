package no.nav.melosys.integrasjon.gsak;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.felles.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave.OpprettOppgaveRequest;
import no.nav.melosys.integrasjon.gsak.mock.OppgaveMockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("mocking")
public class GsakMockService implements GsakFasade {

    private OppgaveMockRepository oppgaveRepo;

    @Autowired
    public GsakMockService(OppgaveMockRepository oppgaveRepo) {
        this.oppgaveRepo = oppgaveRepo;
    }

    @Override
    public void ferdigstillOppgave(String oppgaveId) throws SikkerhetsbegrensningException, TekniskException {
        Oppgave oppgave = hentOppgave(oppgaveId);
        oppgaveRepo.delete(oppgave);
    }

    @Override
    public List<Oppgave> finnUtildelteOppgaverEtterFrist(String oppavetype, List<String> fagområdeKodeListe, List<String> sakstyper, List<String> behandlingstyper) throws IntegrasjonException {
        if ("BEH_SAK".equals(oppavetype) || "JFR".equals(oppavetype)) {
            return oppgaveRepo.find(oppavetype, sakstyper, behandlingstyper);
        } else {
            throw new  IllegalArgumentException(oppavetype);
        }
    }

    @Override
    public Oppgave hentOppgave(String oppgaveId) {
        return oppgaveRepo.findOne(oppgaveId);
    }

    @Override
    public String opprettOppgave(OpprettOppgaveRequest request) throws SikkerhetsbegrensningException {
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
        oppgave.setSaksnummer(request.getSaksnummer());
        oppgave.setUnderkategori(request.getUnderkategoriKode());

        return oppgaveRepo.save(oppgave);
    }

    @Override
    public void leggTilbakeOppgave(Oppgave oppgave) throws IntegrasjonException, SikkerhetsbegrensningException, TekniskException {
        oppgave.setAnsvarligId(null);
    }

    @Override
    public String opprettSak(Long fagsakId, String fnr) throws IntegrasjonException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void tildelOppgave(String oppgaveId, String saksbehandlerID) {
        Oppgave oppgave = hentOppgave(oppgaveId);
        oppgave.setAnsvarligId(saksbehandlerID);
    }

}
