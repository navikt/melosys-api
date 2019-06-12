package no.nav.melosys.service.unntaksperiode;

import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.IkkeGodkjentBegrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.apache.commons.lang3.StringUtils;
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
    public void ikkeGodkjennPeriode(Behandling behandling, Set<String> begrunnelser, String fritekst) throws FunksjonellException {
        Set<IkkeGodkjentBegrunnelser> ikkeGodkjentBegrunnelser = tilIkkeGodkjentBegrunnelser(begrunnelser);
        validerBegrunnelser(ikkeGodkjentBegrunnelser, fritekst);
        prosessinstansService.opprettProsessinstansUnntaksperiodeAvvist(behandling, ikkeGodkjentBegrunnelser, fritekst);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void behandlingUnderAvklaring(Behandling behandling) {
        prosessinstansService.opprettProsessinstansUnntaksperiodeUnderAvklaring(behandling);
    }

    private Set<IkkeGodkjentBegrunnelser> tilIkkeGodkjentBegrunnelser(Set<String> begrunnelser) {
        Set<IkkeGodkjentBegrunnelser> ikkeGodkjentBegrunnelser = new HashSet<>();
        for (String b : begrunnelser) {
            ikkeGodkjentBegrunnelser.add(IkkeGodkjentBegrunnelser.valueOf(b));
        }
        return ikkeGodkjentBegrunnelser;
    }

    private void validerBegrunnelser(Set<IkkeGodkjentBegrunnelser> begrunnelser, String fritekst) throws FunksjonellException {

        if (begrunnelser.isEmpty()) {
            throw new FunksjonellException("Ingen begrunnelser for avlag av periode");
        } else if (begrunnelser.contains(IkkeGodkjentBegrunnelser.ANNET) && StringUtils.isEmpty(fritekst)) {
            throw new FunksjonellException("Begrunnelse " + IkkeGodkjentBegrunnelser.ANNET + " krever fritekst!");
        }
    }
}
