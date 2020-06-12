package no.nav.melosys.saksflyt.steg.iv;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_OPPDATER_RESULTAT;

/**
 * Oppdaterer behandlingsresultat med vedtaksdato og klagefrist.
 */
@Component
public class OppdaterBehandlingsresultat extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OppdaterBehandlingsresultat.class);

    static final int FRIST_KLAGE_UKER = 6;

    private final BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public OppdaterBehandlingsresultat(BehandlingsresultatService behandlingsresultatService) {
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_OPPDATER_RESULTAT;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Long behandlingID = prosessinstans.getBehandling().getId();

        if (prosessinstans.getBehandling().erNorgeUtpekt()) {
            behandlingsresultatService.oppdaterUtfallUtpeking(behandlingID, Utfallregistreringunntak.GODKJENT);
        }

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        if (prosessinstans.getType() != ProsessType.IVERKSETT_VEDTAK_FORKORT_PERIODE) {
            behandlingsresultat.setFastsattAvLand(Landkoder.NO);
            String fritekst = prosessinstans.getData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST);
            behandlingsresultat.setBegrunnelseFritekst(fritekst);
        }

        Vedtakstyper vedtakstype = Vedtakstyper.valueOf(prosessinstans.getData(ProsessDataKey.VEDTAKSTYPE));
        String revurderBegrunnelse = prosessinstans.getData(ProsessDataKey.REVURDER_BEGRUNNELSE);
        LocalDate klagefrist = LocalDate.now().plusWeeks(OppdaterBehandlingsresultat.FRIST_KLAGE_UKER);
        behandlingsresultat.fornyeVedtakMetadata(vedtakstype, revurderBegrunnelse, klagefrist);

        behandlingsresultatService.lagre(behandlingsresultat);

        prosessinstans.setSteg(ProsessSteg.IV_OPPRETT_AVGIFTSOPPGAVE);

        log.info("Oppdatert resultat og vedtak for for prosessinstans {}.", prosessinstans.getId());
    }

}