package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.vedtak.dto.FattEosVedtakRequest;
import no.nav.melosys.service.vedtak.dto.FattFtrlVedtakRequest;
import no.nav.melosys.service.vedtak.dto.FattVedtakRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Sakstyper.EU_EOS;
import static no.nav.melosys.domain.kodeverk.Sakstyper.FTRL;

@Service
public class VedtakServiceFasade {

    private final BehandlingService behandlingService;
    private final EosVedtakService eosVedtakService;
    private final EosVedtakSystemService eosVedtakSystemService;
    private final FtrlVedtakService ftrlVedtakService;

    @Autowired
    public VedtakServiceFasade(BehandlingService behandlingService, EosVedtakService eosVedtakService,
                               EosVedtakSystemService eosVedtakSystemService, FtrlVedtakService ftrlVedtakService) {
        this.behandlingService = behandlingService;
        this.eosVedtakService = eosVedtakService;
        this.eosVedtakSystemService = eosVedtakSystemService;
        this.ftrlVedtakService = ftrlVedtakService;
    }

    @Transactional(rollbackFor = MelosysException.class, noRollbackFor = {ValideringException.class})
    public void fattVedtak(long behandlingID, Behandlingsresultattyper behandlingsresultattype) throws MelosysException {
        var behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);

        eosVedtakSystemService.fattVedtak(behandling, behandlingsresultattype, Vedtakstyper.FØRSTEGANGSVEDTAK);
    }

    @Transactional(rollbackFor = MelosysException.class, noRollbackFor = {ValideringException.class})
    public void fattVedtak(long behandlingID, FattVedtakRequest fattVedtakRequest) throws MelosysException {
        var behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);

        validerKanFattesVedtakAvTema(behandling);

        Sakstyper sakstype = behandling.getFagsak().getType();

        switch (sakstype) {
            case EU_EOS -> eosVedtakService.fattVedtak(behandling, (FattEosVedtakRequest) fattVedtakRequest);
            case FTRL -> ftrlVedtakService.fattVedtak(behandling, (FattFtrlVedtakRequest) fattVedtakRequest);
            default -> throw new FunksjonellException("Vedtaksfatting for sakstype " + sakstype + " er ikke støttet.");
        }
    }

    @Transactional(rollbackFor = MelosysException.class, noRollbackFor = {ValideringException.class})
    public void endreVedtak(long behandlingID, Endretperiode endretperiode, String fritekst, String fritekstSed) throws FunksjonellException {
        var behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        Sakstyper sakstype = behandling.getFagsak().getType();

        if (sakstype == EU_EOS) {
            eosVedtakService.endreVedtak(behandling, endretperiode, fritekst, fritekstSed);
        } else {
            throw new FunksjonellException("Vedtaksendring for sakstype " + sakstype + " er ikke støttet.");
        }
    }

    @Transactional
    public void publiserFattetVedtak(long behandlingId) throws FunksjonellException {
        var behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingId);
        Sakstyper sakstype = behandling.getFagsak().getType();

        switch (sakstype) {
            case FTRL -> ftrlVedtakService.publiserFattetVedtak(behandling);
            default -> throw new FunksjonellException("Publisering av fattet vedtak for sakstype " + sakstype + " er ikke støttet");
        }
    }

    private void validerKanFattesVedtakAvTema(Behandling behandling) throws FunksjonellException {
        if (!behandling.kanResultereIVedtak()) {
            throw new FunksjonellException("Kan ikke fatte vedtak ved behandlingstema " + behandling.getTema().getBeskrivelse());
        }
    }
}
