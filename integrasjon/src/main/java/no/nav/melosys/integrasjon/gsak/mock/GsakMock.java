package no.nav.melosys.integrasjon.gsak.mock;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.gsak.Fagomrade;
import no.nav.melosys.domain.Oppgavetype;
import no.nav.melosys.domain.gsak.PrioritetType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

//FIXME Fjernes når GSAK leverer nye REST tjenester
@Component
@Profile("mocking")
public class GsakMock implements OppgaveMockRepository {

    private Map<String, Oppgave> oppgaver;

    private int oppgaveNr;

    public GsakMock() {
        oppgaver = new HashMap<>();
        nyOppgaveListe().forEach(o -> {oppgaver.put(o.getOppgaveId(), o); oppgaveNr++;});
    }

    private List<Oppgave> nyOppgaveListe() {
        Oppgave o1 = new Oppgave();
        o1.setOppgaveId("1");
        o1.setBruker("99999999991");
        o1.setFagomrade(Fagomrade.MED);
        o1.setOppgavetype(no.nav.melosys.domain.gsak.Oppgavetype.BEH_SAK_MED);
        o1.setPrioritet(PrioritetType.NORM_MED);
        o1.setGsakSaksnummer("123");
        o1.setAktivTil(LocalDate.now().plusYears(1));

        Oppgave o2 = new Oppgave();
        o2.setOppgaveId("2");
        o2.setBruker("99999999991");
        o2.setFagomrade(Fagomrade.UFM);
        o2.setOppgavetype(no.nav.melosys.domain.gsak.Oppgavetype.BEH_SAK_MK_UFM);
        o2.setPrioritet(PrioritetType.NORM_MED);
        o2.setGsakSaksnummer("123");
        o2.setAktivTil(LocalDate.now().plusYears(1));

        Oppgave o5 = new Oppgave();
        o5.setOppgaveId("5");
        o5.setBruker("FJERNET");
        o5.setFagomrade(Fagomrade.MED);
        o5.setOppgavetype(no.nav.melosys.domain.gsak.Oppgavetype.JFR_MED);
        o5.setPrioritet(PrioritetType.HOY_MED);
        o5.setDokumentId("415782379");
        o5.setAktivTil(LocalDate.now().plusYears(1));
        o5.setAnsvarligId("Z990749");

        Oppgave o6 = new Oppgave();
        o6.setOppgaveId("6");
        o6.setBruker("99999999991");
        o6.setFagomrade(Fagomrade.MED);
        o6.setOppgavetype(no.nav.melosys.domain.gsak.Oppgavetype.BEH_SAK_MED);
        o6.setPrioritet(PrioritetType.NORM_MED);
        o6.setGsakSaksnummer("123");
        o6.setAktivTil(LocalDate.now().plusYears(1));
        o6.setAnsvarligId("Z990749");

        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(o1);
        oppgaver.add(o2);
        oppgaver.add(o5);
        oppgaver.add(o6);

        return oppgaver;
    }

    @Override
    public void delete(Oppgave o) {
        if (o.getOppgaveId() != null) {
            oppgaver.remove(o.getOppgaveId());
        }
    }

    @Override
    public List<Oppgave> find(Oppgavetype oppgavetype, List<String> sakstyper, List<String> behandlingstyper) {
        if (Oppgavetype.BEH_SAK.equals(oppgavetype)) {
            return oppgaver.values().stream().filter(Oppgave::erBehandling).sorted(Comparator.comparing(Oppgave::getAktivTil)).collect(Collectors.toList());
        } else if (Oppgavetype.JFR.equals(oppgavetype)) {
            return oppgaver.values().stream().filter(Oppgave::erJournalFøring).sorted(Comparator.comparing(Oppgave::getAktivTil)).collect(Collectors.toList());
        } else {
            throw new IllegalArgumentException(oppgavetype.toString());
        }
    }

    @Override
    public List<Oppgave> finnOppgaverMedAnsvarligID(String ansvarligID){
        return oppgaver.values().stream().filter(oppgave -> ansvarligID.equals(oppgave.getAnsvarligId())).collect(Collectors.toList());
    }

    @Override
    public List<Oppgave> finnOppgaverMedBruker(String brukerIdent) {
        return oppgaver.values().stream().filter(oppgave -> brukerIdent.equals(oppgave.getBruker())).collect(Collectors.toList());
    }

    @Override
    public Oppgave findOne(String id) {
        return oppgaver.get(id);
    }

    @Override
    public void fjernTildeling() {
        oppgaver.values().forEach(o -> o.setAnsvarligId(null));
    }

    @Override
    public synchronized String save(Oppgave o) {
        if (o.getOppgaveId() == null) {
            oppgaveNr++;
            String oppgaveId = Integer.toString(oppgaveNr);
            o.setOppgaveId(oppgaveId);
            oppgaver.put(oppgaveId, o);
            return oppgaveId;
        } else {
            oppgaver.put(o.getOppgaveId(), o);
            return o.getOppgaveId();
        }
    }
}
