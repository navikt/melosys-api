package no.nav.melosys.integrasjon.gsak.mock;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.gsak.Fagomrade;
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
        o2.setBruker("FJERNET");
        o2.setFagomrade(Fagomrade.MED);
        o2.setOppgavetype(no.nav.melosys.domain.gsak.Oppgavetype.JFR_MED);
        o2.setPrioritet(PrioritetType.HOY_MED);
        o2.setDokumentId("415826177");
        o2.setAktivTil(LocalDate.now().plusYears(1));
        o2.setAnsvarligId("Z990749");

        Oppgave o3 = new Oppgave();
        o3.setOppgaveId("3");
        o3.setBruker("99999999991");
        o3.setFagomrade(Fagomrade.MED);
        o3.setOppgavetype(no.nav.melosys.domain.gsak.Oppgavetype.BEH_SAK_MED);
        o3.setPrioritet(PrioritetType.NORM_MED);
        o3.setGsakSaksnummer("123");
        o3.setAktivTil(LocalDate.now().plusYears(1));
        o3.setAnsvarligId("Z990749");

        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(o1);
        oppgaver.add(o2);
        oppgaver.add(o3);

        return oppgaver;
    }

    @Override
    public void delete(Oppgave o) {
        if (o.getOppgaveId() != null) {
            oppgaver.remove(o.getOppgaveId());
        }
    }

    @Override
    public List<Oppgave> find(Oppgavetype oppgavetype, Tema fagområde, List<FagsakType> sakstyper, List<BehandlingType> behandlingstyper) {
        if (Oppgavetype.BEH_SAK.equals(oppgavetype)) {
            return oppgaver.values().stream().filter(Oppgave::erBehandling).sorted(Comparator.comparing(Oppgave::getAktivTil)).collect(Collectors.toList());
        } else if (Oppgavetype.JFR.equals(oppgavetype)) {
            return oppgaver.values().stream().filter(Oppgave::erJournalFøring).filter(o -> o.getFagomrade().toString().equalsIgnoreCase(fagområde.getKode()))
                .sorted(Comparator.comparing(Oppgave::getAktivTil)).collect(Collectors.toList());
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
