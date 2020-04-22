package no.nav.melosys.service.behandling.jobb;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvsluttArt13BehandlingService {

    private static final Logger log = LoggerFactory.getLogger(AvsluttArt13BehandlingService.class);

    private final BehandlingService behandlingService;
    private final FagsakService fagsakService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final MedlPeriodeService medlPeriodeService;

    public AvsluttArt13BehandlingService(BehandlingService behandlingService, FagsakService fagsakService, BehandlingsresultatService behandlingsresultatService, MedlPeriodeService medlPeriodeService) {
        this.behandlingService = behandlingService;
        this.fagsakService = fagsakService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void avsluttBehandling(long behandlingID) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        fagsakService.avsluttFagsakOgBehandling(behandling.getFagsak(), Saksstatuser.LOVVALG_AVKLART);

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        Lovvalgsperiode lovvalgsperiode = behandlingsresultat.hentValidertLovvalgsperiode();

        if (lovvalgsperiode.getMedlPeriodeID() == null) {
            throw new FunksjonellException("Behandling "+ behandling.getId()
                + " har en lovvalgsperiode som ikke er registrert i medl. Kan ikke avslutte art13 behandling automatisk");
        }

        medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode, !behandling.erBehandlingAvSøknad());
        log.info("Behandling {} avsluttet og satt til endelig i Medl", behandling.getId());
    }
}