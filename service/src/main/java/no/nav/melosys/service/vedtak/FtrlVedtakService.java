package no.nav.melosys.service.vedtak;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FtrlVedtakService {
    private static final Logger log = LoggerFactory.getLogger(FtrlVedtakService.class);
    private static final int FRIST_KLAGE_UKER = 6;

    private final BehandlingsresultatService behandlingsresultatService;
    private final BehandlingService behandlingService;
    private final ProsessinstansService prosessinstansService;

    @Autowired
    public FtrlVedtakService(BehandlingsresultatService behandlingsresultatService, BehandlingService behandlingService, ProsessinstansService prosessinstansService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.behandlingService = behandlingService;
        this.prosessinstansService = prosessinstansService;
    }

    public void fattVedtak(Behandling behandling, FattFtrlVedtakRequest request) throws MelosysException {
        long behandlingID = behandling.getId();

        log.info("Fatter vedtak for (FTRL) sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        behandlingsresultat.setType(request.getBehandlingsresultatTypeKode());

        oppdaterBehandlingsresultat(behandlingsresultat, request.getVedtakstype(), request.getFritekstBegrunnelse());

        if (prosessinstansService.harVedtakInstans(behandlingID)) {
            throw new FunksjonellException("Det finnes allerede en vedtak-prosess for behandling " + behandling);
        }

        behandling.getFagsak().setStatus(Saksstatuser.MEDLEMSKAP_AVKLART);
        behandling.setStatus(Behandlingsstatus.IVERKSETTER_VEDTAK);
        behandlingService.lagre(behandling);
    }

    private void oppdaterBehandlingsresultat(Behandlingsresultat behandlingsresultat,
                                             Vedtakstyper vedtakstype,
                                             String behandlingresultatBegrunnelseFritekst) {
        behandlingsresultat.settVedtakMetadata(vedtakstype, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));
        behandlingsresultat.setBegrunnelseFritekst(behandlingresultatBegrunnelseFritekst);
        behandlingsresultat.setFastsattAvLand(Landkoder.NO);
        behandlingsresultatService.lagre(behandlingsresultat);
    }
}
