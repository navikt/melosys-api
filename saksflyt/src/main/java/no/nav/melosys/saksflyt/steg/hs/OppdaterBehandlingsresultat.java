package no.nav.melosys.saksflyt.steg.hs;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Henleggelsesgrunner;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.HS_HENLEGG_SAK;

/**
 * Oppdaterer behandlingsresultat
 *
 * Transisjoner:
 * HS_OPPDATER_RESULTAT -> HENLEGG_SAK eller FEILET_MASKINELT hvis feil
 */
@Component("HenleggSakOppdaterBehandlingsresultat")
public class OppdaterBehandlingsresultat extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OppdaterBehandlingsresultat.class);

    private final BehandlingsresultatRepository behandlingsresultatRepository;

    @Autowired
    public OppdaterBehandlingsresultat(BehandlingsresultatRepository behandlingsresultatRepository) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        log.info("OppdaterBehandlingsresultat initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.HS_OPPDATER_RESULTAT;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws IkkeFunnetException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Long behandlingID = prosessinstans.getBehandling().getId();

        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Kan ikke oppdatere behandlingsresultatet for henleggelsen fordi behandling " + behandlingID + " ikke finnes."));
        behandlingsresultat.setType(Behandlingsresultattyper.HENLEGGELSE);
        behandlingsresultat.setEndretAv(prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER));
        behandlingsresultat.setBegrunnelseFritekst(prosessinstans.getData(ProsessDataKey.FRITEKST));

        BehandlingsresultatBegrunnelse begrunnelse = new BehandlingsresultatBegrunnelse();
        begrunnelse.setBehandlingsresultat(behandlingsresultat);
        begrunnelse.setKode(prosessinstans.getData(ProsessDataKey.BEGRUNNELSEKODE, Henleggelsesgrunner.class).getKode());
        behandlingsresultat.getBehandlingsresultatBegrunnelser().add(begrunnelse);

        behandlingsresultatRepository.save(behandlingsresultat);

        log.info("Oppdatert behandlingsresultat for prosessinstans {}. Satt til henleggelse", prosessinstans.getId());
        prosessinstans.setSteg(HS_HENLEGG_SAK);
    }
}
