package no.nav.melosys.saksflyt.felles;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.unntaksperiode.kontroll.RegisterkontrollService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RegisterKontrollFelles {
    private static final Logger log = LoggerFactory.getLogger(RegisterKontrollFelles.class);

    private final BehandlingService behandlingService;
    private final RegisterkontrollService registerkontrollService;
    private final AvklartefaktaService avklartefaktaService;

    @Autowired
    public RegisterKontrollFelles(BehandlingService behandlingService, RegisterkontrollService registerkontrollService, AvklartefaktaService avklartefaktaService) {
        this.behandlingService = behandlingService;
        this.registerkontrollService = registerkontrollService;
        this.avklartefaktaService = avklartefaktaService;
    }

    public void utførKontrollerOgRegistrerFeil(long behandlingId) throws TekniskException, FunksjonellException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        List<Unntak_periode_begrunnelser> registrerteTreff = registerkontrollService.utførKontroller(behandling);
        registrerFeil(behandlingId, registrerteTreff);
    }

    private void registrerFeil(long behandlingId, List<Unntak_periode_begrunnelser> registrerteTreff) throws IkkeFunnetException {
        boolean funnetTreff = !registrerteTreff.isEmpty();
        avklartefaktaService.leggTilAvklarteFakta(behandlingId, Avklartefaktatype.VURDERING_UNNTAK_PERIODE,
            Avklartefaktatype.VURDERING_UNNTAK_PERIODE.name(), null, funnetTreff ? "TRUE" : "FALSE");

        log.info("Treff ved validering av periode for behandling {}. Treffbegrunnelse: {}", behandlingId, registrerteTreff);
        for (Unntak_periode_begrunnelser begrunnelse : registrerteTreff) {
            avklartefaktaService.leggTilRegistrering(behandlingId, Avklartefaktatype.VURDERING_UNNTAK_PERIODE, begrunnelse.getKode());
        }
    }
}
