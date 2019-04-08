package no.nav.melosys.saksflyt.agent.hs;

import java.util.Map;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Henleggelsesgrunner;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
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
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws IkkeFunnetException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Long behandlingID = prosessinstans.getBehandling().getId();

        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Kan ikke oppdatere behandlingsresultatet for henleggelsen fordi behandling " + behandlingID + " ikke finnes."));
        behandlingsresultat.setType(Behandlingsresultattyper.HENLEGGELSE);
        behandlingsresultat.setEndretAv(prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER));
        behandlingsresultat.setHenleggelsesgrunn(prosessinstans.getData(ProsessDataKey.BEGRUNNELSEKODE, Henleggelsesgrunner.class));
        behandlingsresultat.setHenleggelseFritekst(prosessinstans.getData(ProsessDataKey.FRITEKST));
        behandlingsresultatRepository.save(behandlingsresultat);

        log.info("Oppdatert behandlingsresultat for prosessinstans {}. Satt til henleggelse", prosessinstans.getId());
        prosessinstans.setSteg(HS_HENLEGG_SAK);
    }
}
