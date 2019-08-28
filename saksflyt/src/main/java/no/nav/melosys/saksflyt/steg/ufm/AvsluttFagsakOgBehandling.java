package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflyt.felles.FagsakOgBehandlingFelles;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("avsluttFagsakOgBehandlingUnntakFraMedlemskap")
public class AvsluttFagsakOgBehandling extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AvsluttFagsakOgBehandling.class);

    private final FagsakOgBehandlingFelles fagsakOgBehandlingFelles;
    private final BehandlingsresultatRepository behandlingsresultatRepository;

    @Autowired
    public AvsluttFagsakOgBehandling(FagsakOgBehandlingFelles fagsakOgBehandlingFelles, BehandlingsresultatRepository behandlingsresultatRepository) {
        this.fagsakOgBehandlingFelles = fagsakOgBehandlingFelles;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_AVSLUTT_BEHANDLING;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        fagsakOgBehandlingFelles.avsluttFagsakOgBehandling(prosessinstans.getBehandling(), Saksstatuser.LOVVALG_AVKLART);
        oppdaterUtfallRegistreringUnntak(prosessinstans.getBehandling().getId());
        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_AVSLUTTET);
    }


    private void oppdaterUtfallRegistreringUnntak(long behandlingId) throws TekniskException {
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findById(behandlingId)
            .orElseThrow(() -> new TekniskException("Finner ikke behandlingsresultat for behandling " + behandlingId));

        behandlingsresultat.setType(Behandlingsresultattyper.REGISTRERT_UNNTAK);
        behandlingsresultat.setUtfallRegistreringUnntak(Utfallregistreringunntak.GODKJENT);
        behandlingsresultatRepository.save(behandlingsresultat);
    }
}
