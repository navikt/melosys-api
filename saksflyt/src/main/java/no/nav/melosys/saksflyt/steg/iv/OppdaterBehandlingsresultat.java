package no.nav.melosys.saksflyt.steg.iv;

import java.time.Instant;
import java.time.LocalDate;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.VedtakMetadata;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
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

import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_AVKLAR_MYNDIGHET;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_OPPDATER_RESULTAT;

/**
 * Oppdaterer behandlingsresultat med vedtaksdato og klagefrist.
 *
 * Transisjoner:
 * IV_OPPDATER_RESULTAT -> IV_AVKLAR_MYNDIGHET eller FEILET_MASKINELT hvis feil
 */
@Component
public class OppdaterBehandlingsresultat extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OppdaterBehandlingsresultat.class);

    static final int FRIST_KLAGE_UKER = 6;

    private final BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public OppdaterBehandlingsresultat(BehandlingsresultatService behandlingsresultatService) {
        this.behandlingsresultatService = behandlingsresultatService;
        log.info("OppdaterBehandlingsresultat initialisert");
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

        if (prosessinstans.getType() == ProsessType.IVERKSETT_VEDTAK) {
            behandlingsresultat.setType(Behandlingsresultattyper.valueOf(prosessinstans.getData(ProsessDataKey.BEHANDLINGSRESULTATTYPE)));
            behandlingsresultat.setFastsattAvLand(Landkoder.NO);
            behandlingsresultat.setBegrunnelseFritekst(prosessinstans.getData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST));
        }

        behandlingsresultat.setEndretAv(prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER));
        VedtakMetadata vedtakMetadata;
        if (behandlingsresultat.getVedtakMetadata() == null) {
            vedtakMetadata = new VedtakMetadata();
            vedtakMetadata.setBehandlingsresultat(behandlingsresultat);
            behandlingsresultat.setVedtakMetadata(vedtakMetadata);
        } else {
            vedtakMetadata = behandlingsresultat.getVedtakMetadata();
        }
        vedtakMetadata.setVedtaksdato(Instant.now());
        LocalDate klagefrist = LocalDate.now().plusWeeks(FRIST_KLAGE_UKER);
        vedtakMetadata.setVedtakKlagefrist(klagefrist);
        vedtakMetadata.setRevurderBegrunnelse(prosessinstans.getData(ProsessDataKey.REVURDER_BEGRUNNELSE));
        vedtakMetadata.setVedtakstype(prosessinstans.getData(ProsessDataKey.VEDTAKSTYPE) == null ? null : Vedtakstyper.valueOf(prosessinstans.getData(ProsessDataKey.VEDTAKSTYPE)));

        behandlingsresultatService.lagre(behandlingsresultat);

        prosessinstans.setSteg(IV_AVKLAR_MYNDIGHET);
        log.info("Oppdatert behandlingsresultat for prosessinstans {}. Klagefrist: {}", prosessinstans.getId(), klagefrist);
    }
}