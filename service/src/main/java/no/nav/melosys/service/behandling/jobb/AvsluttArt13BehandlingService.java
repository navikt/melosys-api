package no.nav.melosys.service.behandling.jobb;

import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Utpekingsperiode;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.service.kontroll.PeriodeKontroller.datoEldreEnn2Mnd;

@Service
public class AvsluttArt13BehandlingService {

    private static final Logger log = LoggerFactory.getLogger(AvsluttArt13BehandlingService.class);

    private final BehandlingService behandlingService;
    private final FagsakService fagsakService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final MedlPeriodeService medlPeriodeService;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    public AvsluttArt13BehandlingService(BehandlingService behandlingService, FagsakService fagsakService, BehandlingsresultatService behandlingsresultatService, MedlPeriodeService medlPeriodeService, LovvalgsperiodeService lovvalgsperiodeService) {
        this.behandlingService = behandlingService;
        this.fagsakService = fagsakService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void avsluttBehandlingHvisToMndPassert(long behandlingID) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());

        if (toMndHarPassertSidenSaksbehandling(behandling, behandlingsresultat)) {
            avsluttBehandling(behandling, behandlingsresultat);
        }
    }

    private void avsluttBehandling(Behandling behandling, Behandlingsresultat behandlingsresultat) throws FunksjonellException, TekniskException {

        log.info("To måneder har passert siden saksbehandling for behandling {}. Forsøker å avslutte den", behandling.getId());
        Lovvalgsperiode lovvalgsperiode = hentEllerLagLovvalgsperiode(behandlingsresultat);

        if (!lovvalgsperiode.erArtikkel13()) {
            throw new FunksjonellException("Behandling skal ikke avsluttes automatisk da perioden er av bestemmelse"
                + lovvalgsperiode.getBestemmelse());
        } else if (lovvalgsperiode.getMedlPeriodeID() == null) {
            throw new FunksjonellException("Behandling " + behandling.getId()
                + " har en medlemskapsperiode som ikke er registrert i medl. Kan ikke avslutte art13 behandling automatisk");
        }

        fagsakService.avsluttFagsakOgBehandling(behandling.getFagsak(), behandling, Saksstatuser.LOVVALG_AVKLART);

        medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode, !behandling.erBehandlingAvSøknad());
        log.info("Behandling {} avsluttet og satt til endelig i Medl", behandling.getId());
    }

    private boolean toMndHarPassertSidenSaksbehandling(Behandling behandling, Behandlingsresultat behandlingsresultat) throws FunksjonellException {
        if (behandling.kanResultereIVedtak() && !behandlingsresultat.harUtpektAnnetLand()) {

            if (!behandlingsresultat.harVedtak()) {
                throw new FunksjonellException("Behandling " + behandling.getId() +
                    " har ikke et vedtak og status kan da ikke settes til AVSLUTTET");
            }

            return datoEldreEnn2Mnd(behandlingsresultat.getVedtakMetadata().getVedtaksdato());
        }

        return datoEldreEnn2Mnd(behandlingsresultat.getEndretDato());
    }

    private Lovvalgsperiode hentEllerLagLovvalgsperiode(Behandlingsresultat behandlingsresultat) {
        if (behandlingsresultat.harUtpektAnnetLand()) {
            return opprettLovvalgsperiode(behandlingsresultat.getId(), behandlingsresultat.hentValidertUtpekingsperiode());
        } else {
            return behandlingsresultat.hentValidertLovvalgsperiode();
        }
    }


    private Lovvalgsperiode opprettLovvalgsperiode(long behandlingID, Utpekingsperiode utpekingsperiode) {
        return lovvalgsperiodeService.lagreLovvalgsperioder(behandlingID, Collections.singleton(Lovvalgsperiode.av(utpekingsperiode)))
            .stream().findFirst().orElseThrow(() -> new IllegalStateException("Feil ved lagring av lovvalgsperiode"));
    }
}