package no.nav.melosys.service.jobb;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
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
    private final MedlFasade medlFasade;

    public AvsluttArt13BehandlingService(BehandlingService behandlingService, FagsakService fagsakService, BehandlingsresultatService behandlingsresultatService, MedlFasade medlFasade) {
        this.behandlingService = behandlingService;
        this.fagsakService = fagsakService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlFasade = medlFasade;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void avsluttBehandling(long behandlingID) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        fagsakService.avsluttFagsakOgBehandling(behandling.getFagsak(), Saksstatuser.LOVVALG_AVKLART, behandling);

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        Lovvalgsperiode lovvalgsperiode = behandlingsresultat.hentValidertLovvalgsperiode();

        if (lovvalgsperiode.getMedlPeriodeID() == null) {
            throw new FunksjonellException("Behandling "+ behandling.getId()
                + " har en lovvalgsperiode som ikke er registrert i medl. Kan ikke avslutte art13 behandling automatisk");
        }

        medlFasade.oppdaterPeriodeEndelig(lovvalgsperiode, behandling.erBehandlingAvSøknad() ? KildedokumenttypeMedl.HENV_SOKNAD : KildedokumenttypeMedl.SED);
        log.info("Behandling {} avsluttet og satt til endelig i Medl", behandling.getId());
    }
}