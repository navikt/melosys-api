package no.nav.melosys.service.unntaksperiode;

import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnntaksperiodeService {
    private final BehandlingService behandlingService;
    private final OppgaveService oppgaveService;
    private final ProsessinstansService prosessinstansService;

    @Autowired
    public UnntaksperiodeService(BehandlingService behandlingService, OppgaveService oppgaveService, ProsessinstansService prosessinstansService) {
        this.behandlingService = behandlingService;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void godkjennPeriode(Behandling behandling) throws FunksjonellException, TekniskException {
        prosessinstansService.opprettProsessinstansGodkjennUnntaksperiode(behandling);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void ikkeGodkjennPeriode(Behandling behandling, Set<String> begrunnelser, String fritekst) throws FunksjonellException, TekniskException {
        Set<Ikke_godkjent_begrunnelser> ikkeGodkjentBegrunnelser = tilIkkeGodkjentBegrunnelser(begrunnelser);
        validerBegrunnelser(ikkeGodkjentBegrunnelser, fritekst);
        prosessinstansService.opprettProsessinstansUnntaksperiodeAvvist(behandling, ikkeGodkjentBegrunnelser, fritekst);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void behandlingUnderAvklaring(Behandling behandling) throws FunksjonellException {
        prosessinstansService.opprettProsessinstansUnntaksperiodeUnderAvklaring(behandling);
        behandlingService.oppdaterStatus(behandling.getId(), Behandlingsstatus.AVVENT_DOK_UTL);
    }

    private Set<Ikke_godkjent_begrunnelser> tilIkkeGodkjentBegrunnelser(Set<String> begrunnelser) {
        Set<Ikke_godkjent_begrunnelser> ikkeGodkjentBegrunnelser = new HashSet<>();
        for (String b : begrunnelser) {
            ikkeGodkjentBegrunnelser.add(Ikke_godkjent_begrunnelser.valueOf(b));
        }
        return ikkeGodkjentBegrunnelser;
    }

    private void validerBegrunnelser(Set<Ikke_godkjent_begrunnelser> begrunnelser, String fritekst) throws FunksjonellException {

        if (begrunnelser.isEmpty()) {
            throw new FunksjonellException("Ingen begrunnelser for avlag av periode");
        } else if (begrunnelser.contains(Ikke_godkjent_begrunnelser.ANNET) && StringUtils.isEmpty(fritekst)) {
            throw new FunksjonellException("Begrunnelse " + Ikke_godkjent_begrunnelser.ANNET + " krever fritekst!");
        }
    }
}
