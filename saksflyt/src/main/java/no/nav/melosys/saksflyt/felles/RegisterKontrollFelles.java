package no.nav.melosys.saksflyt.felles;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Unntak_periode_begrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.kontroll.ufm.UfmKontrollService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RegisterKontrollFelles {
    private static final Logger log = LoggerFactory.getLogger(RegisterKontrollFelles.class);

    private final BehandlingService behandlingService;
    private final UfmKontrollService ufmKontrollService;
    private final AvklartefaktaService avklartefaktaService;

    @Autowired
    public RegisterKontrollFelles(BehandlingService behandlingService, UfmKontrollService ufmKontrollService, AvklartefaktaService avklartefaktaService) {
        this.behandlingService = behandlingService;
        this.ufmKontrollService = ufmKontrollService;
        this.avklartefaktaService = avklartefaktaService;
    }

    public void utførKontrollerOgRegistrerFeil(long behandlingId) throws TekniskException, FunksjonellException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        List<Unntak_periode_begrunnelser> registrerteTreff = ufmKontrollService.utførKontroller(behandling);
        registrerFeil(behandlingId, registrerteTreff);
    }

    private void registrerFeil(long behandlingId, List<Unntak_periode_begrunnelser> registrerteTreff) throws IkkeFunnetException {
        boolean funnetTreff = !registrerteTreff.isEmpty();
        avklartefaktaService.leggTilAvklarteFakta(behandlingId, Avklartefaktatyper.VURDERING_UNNTAK_PERIODE,
            Avklartefaktatyper.VURDERING_UNNTAK_PERIODE.name(), null, funnetTreff ? "TRUE" : "FALSE");

        log.info("Treff ved validering av periode for behandling {}. Treffbegrunnelse: {}", behandlingId, registrerteTreff);
        for (Unntak_periode_begrunnelser begrunnelse : registrerteTreff) {
            avklartefaktaService.leggTilRegistrering(behandlingId, Avklartefaktatyper.VURDERING_UNNTAK_PERIODE, begrunnelse.getKode());
        }
    }
}
