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
import no.nav.melosys.domain.gsak.Oppgavetype;
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
        o1.setFagomrade(Fagomrade.MED);
        o1.setOppgavetype(Oppgavetype.BEH_SAK_MED);
        o1.setPrioritet(PrioritetType.NORM_MED);
        o1.setGsakSaksnummer("SAK_111");
        o1.setAktivTil(LocalDate.now().plusYears(1));

        Oppgave o2 = new Oppgave();
        o2.setOppgaveId("2");
        o2.setFagomrade(Fagomrade.UFM);
        o2.setOppgavetype(Oppgavetype.BEH_SAK_MK_UFM);
        o2.setPrioritet(PrioritetType.NORM_MED);
        o2.setGsakSaksnummer("SAK_222");
        o2.setAktivTil(LocalDate.now().plusYears(1));

        Oppgave o3 = new Oppgave();
        o3.setOppgaveId("3");
        o3.setFagomrade(Fagomrade.MED);
        o3.setOppgavetype(Oppgavetype.JFR_MED);
        o3.setPrioritet(PrioritetType.HOY_MED);
        o3.setAktivTil(LocalDate.now().plusYears(1));

        Oppgave o4 = new Oppgave();
        o4.setOppgaveId("4");
        o4.setFagomrade(Fagomrade.MED);
        o4.setOppgavetype(Oppgavetype.JFR_MED);
        o4.setPrioritet(PrioritetType.HOY_MED);
        o4.setAktivTil(LocalDate.now().plusYears(1));

        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(o1);
        oppgaver.add(o2);
        oppgaver.add(o3);
        oppgaver.add(o4);

        return oppgaver;
    }

    @Override
    public List<Oppgave> finnOppgaverMedAnsvarligID(String ansvarligID){
        return oppgaver.values().stream().filter(oppgave -> ansvarligID.equals(oppgave.getAnsvarligId())).collect(Collectors.toList());
    }

    @Override
    public Oppgave findOne(String id) {
        return oppgaver.get(id);
    }

    @Override
    public void delete(Oppgave o) {
        if (o.getOppgaveId() != null) {
            oppgaver.remove(o.getOppgaveId());
        }
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

    @Override
    public List<Oppgave> find(String oppgavetype, List<String> sakstyper, List<String> behandlingstyper) {
        if ("BEH_SAK".equals(oppgavetype)) {
            return oppgaver.values().stream().filter(o -> o.erBehandling()).sorted(Comparator.comparing(Oppgave::getAktivTil)).collect(Collectors.toList());
        } else if ("JFR".equals(oppgavetype)) {
            return oppgaver.values().stream().filter(o -> o.erJournalFøring()).sorted(Comparator.comparing(Oppgave::getAktivTil)).collect(Collectors.toList());
        } else {
            throw new IllegalArgumentException(oppgavetype);
        }
    }
}
