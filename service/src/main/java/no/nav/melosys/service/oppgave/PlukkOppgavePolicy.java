package no.nav.melosys.service.oppgave;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsstatus;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.FagsakRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PlukkOppgavePolicy {

    private static final Logger log =  LoggerFactory.getLogger(PlukkOppgavePolicy.class);

    private final FagsakRepository fagsakRepository;

    @Autowired
    public PlukkOppgavePolicy(FagsakRepository fagsakRepository) {
        this.fagsakRepository = fagsakRepository;
    }

    Optional<Oppgave> plukkOppgave(List<Oppgave> oppgaver) {
        return oppgaver.stream().min(høyestTilLavestPrioritet);
    }

    List<Oppgave> plukkOppgaverVenterIkkeForDokumentasjon(List<Oppgave> oppgaver) throws TekniskException {
        List<Oppgave> oppgaverValgt = new ArrayList<>();

        for (Oppgave oppgave : oppgaver) {
            Fagsak fagsak = fagsakRepository.findBySaksnummer(oppgave.getSaksnummer());
            Behandling behandling = fagsak.getAktivBehandling();

            if (!Behandlingsstatus.erVenterForDokumentasjon(behandling.getStatus())
                || ((behandling.getDokumentasjonSvarfristDato() != null)
                && behandling.getDokumentasjonSvarfristDato().isBefore(Instant.now()))) {
                oppgaverValgt.add(oppgave);
            }
        }
        return oppgaverValgt;
    }

    private static final Comparator<Oppgave> høyestTilLavestPrioritet = (a, b) -> {
        // Merk: Bryter med konvensjonen (a == b og b == c → a == c), men dette er ok.
        int res = 0;
        if (a.getPrioritet() == b.getPrioritet())
            res = 0;
        else if (a.getPrioritet() == PrioritetType.HOY)
            res = -1;
        else if (b.getPrioritet() == PrioritetType.HOY)
            res = 1;
        else if (a.getPrioritet() == PrioritetType.NORM)
            res = -1;
        else if (b.getPrioritet() == PrioritetType.NORM)
            res = 1;
        if (res == 0) {
            if (a.getFristFerdigstillelse() == null || b.getFristFerdigstillelse() == null)
                return 0;
            return a.getFristFerdigstillelse().compareTo(b.getFristFerdigstillelse());
        }
        return res;
    };

}
