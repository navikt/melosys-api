package no.nav.melosys.service.unntaksperiode;

import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.IkkeGodkjentBegrunnelser;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnntaksperiodeService {

    private final ProsessinstansService prosessinstansService;

    public UnntaksperiodeService(ProsessinstansService prosessinstansService) {
        this.prosessinstansService = prosessinstansService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void godkjennPeriode(Behandling behandling) {
        prosessinstansService.opprettProsessinstansGodkjennUnntaksperiode(behandling);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void avvisPeriode(Behandling behandling, Set<IkkeGodkjentBegrunnelser> begrunnelser, String fritekst) {
        prosessinstansService.opprettProsessinstansUnntaksperiodeAvvist(behandling, begrunnelser, fritekst);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void behandlingUnderAvklaring(Behandling behandling) {
        //TODO: MELOSYS-2678
    }
}
