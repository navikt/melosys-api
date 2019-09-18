package no.nav.melosys.saksflyt.steg.vs;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.BehandlingsresultatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.VS_SEND_ORIENTERINGSBREV;

/**
 * Oppdaterer behandlingsresultat
 *
 * Transisjoner:
 * VS_OPPDATER_RESULTAT -> VS_AVSLUTT_FAGSAK eller FEILET_MASKINELT hvis feil
 */
@Component("VideresendSoknadOppdaterBehandlingsresultat")
public class OppdaterBehandlingsresultat extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OppdaterBehandlingsresultat.class);

    private final BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public OppdaterBehandlingsresultat(BehandlingsresultatService behandlingsresultatService) {
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.VS_OPPDATER_RESULTAT;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws IkkeFunnetException {
        Long behandlingID = prosessinstans.getBehandling().getId();
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        behandlingsresultat.setType(Behandlingsresultattyper.HENLEGGELSE);
        behandlingsresultat.setEndretAv(prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER));
        behandlingsresultat.setBegrunnelseFritekst(prosessinstans.getData(ProsessDataKey.FRITEKST));

        log.info("Oppdatert behandlingsresultat for prosessinstans {}. Satt til henleggelse", prosessinstans.getId());
        prosessinstans.setSteg(VS_SEND_ORIENTERINGSBREV);
    }
}