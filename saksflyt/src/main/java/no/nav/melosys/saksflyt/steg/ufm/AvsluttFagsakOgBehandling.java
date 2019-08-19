package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.UtfallRegistreringUnntak;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflyt.felles.OppdaterFagsakOgBehandling;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("avsluttFagsakOgBehandlingUnntakFraMedlemskap")
public class AvsluttFagsakOgBehandling extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AvsluttFagsakOgBehandling.class);

    private final OppdaterFagsakOgBehandling oppdaterFagsakOgBehandling;
    private final BehandlingsresultatRepository behandlingsresultatRepository;

    @Autowired
    public AvsluttFagsakOgBehandling(OppdaterFagsakOgBehandling oppdaterFagsakOgBehandling, BehandlingsresultatRepository behandlingsresultatRepository) {
        this.oppdaterFagsakOgBehandling = oppdaterFagsakOgBehandling;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_AVSLUTT_BEHANDLING;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        oppdaterFagsakOgBehandling.oppdaterFagsakOgBehandlingStatuser(behandling, Saksstatuser.LOVVALG_AVKLART, Behandlingsstatus.AVSLUTTET);

        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findById(behandling.getId())
            .orElseThrow(() -> new TekniskException("Finner ikke behandlingsresultat for behandling " + behandling.getId()));

        behandlingsresultat.setType(Behandlingsresultattyper.REGISTRERT_UNNTAK);
        behandlingsresultat.setUtfallRegistreringUnntak(UtfallRegistreringUnntak.GODKJENT);
        behandlingsresultatRepository.save(behandlingsresultat);

        log.info("Periode regisrert og behandling avsluttet for fagsak {}, behandling {}", behandling.getFagsak().getSaksnummer(), behandling.getId());
        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_AVSLUTTET);
    }
}
