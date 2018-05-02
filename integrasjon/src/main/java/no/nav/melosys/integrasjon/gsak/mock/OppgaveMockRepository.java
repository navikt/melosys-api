package no.nav.melosys.integrasjon.gsak.mock;

import java.util.List;

import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.Oppgavetype;

//FIXME Fjernes når GSAK leverer nye REST tjenester
public interface OppgaveMockRepository {

    Oppgave findOne(String id);

    void delete(Oppgave o);

    String save(Oppgave o);

    List<Oppgave> find(Oppgavetype oppavetype, List<String> sakstyper, List<String> behandlingstyper);

    List<Oppgave> finnOppgaverMedAnsvarligID(String ansvarligID);

    List<Oppgave> finnOppgaverMedBruker(String brukerIdent);

    void fjernTildeling();
}
