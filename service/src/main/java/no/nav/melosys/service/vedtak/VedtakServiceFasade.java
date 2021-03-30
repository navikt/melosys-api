package no.nav.melosys.service.vedtak;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.behandling.BehandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Sakstyper.EU_EOS;
import static no.nav.melosys.domain.kodeverk.Sakstyper.UKJENT;

@Service
public class VedtakServiceFasade {

    private final BehandlingService behandlingService;
    private final EosVedtakService eosVedtakService;
    private final EosVedtakSystemService eosVedtakSystemService;
    private final FtrlVedtakService ftrlVedtakService;

    @Autowired
    public VedtakServiceFasade(BehandlingService behandlingService, EosVedtakService eosVedtakService, EosVedtakSystemService eosVedtakSystemService, FtrlVedtakService ftrlVedtakService) {
        this.behandlingService = behandlingService;
        this.eosVedtakService = eosVedtakService;
        this.eosVedtakSystemService = eosVedtakSystemService;
        this.ftrlVedtakService = ftrlVedtakService;
    }

    @Transactional(rollbackFor = MelosysException.class, noRollbackFor = {ValideringException.class})
    public void fattVedtak(long behandlingID, Behandlingsresultattyper behandlingsresultattype) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);

        eosVedtakSystemService.fattVedtak(behandling, behandlingsresultattype, Vedtakstyper.FØRSTEGANGSVEDTAK);
    }

    @Transactional(rollbackFor = MelosysException.class, noRollbackFor = {ValideringException.class})
    public void fattVedtak(long behandlingID, FattVedtakDto fattVedtakDto) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);

        validerKanFattesVedtakAvTema(behandling);

        Sakstyper sakstype = behandling.getFagsak().getType();

        switch (sakstype) {
            case UKJENT, EU_EOS -> fattVedtakForEos(behandling, (EosFattVedtakDto) fattVedtakDto);
            case FTRL -> fattVedtakForFtrl(behandling, (FtrlFattVedtakDto) fattVedtakDto);
            default -> throw new FunksjonellException("Vedtaksfatting for sakstype " + sakstype + " er ikke støttet.");
        }
    }

    @Transactional(rollbackFor = MelosysException.class, noRollbackFor = {ValideringException.class})
    public void endreVedtak(long behandlingID, EndreVedtakDto endreVedtakDto) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        Sakstyper sakstype = behandling.getFagsak().getType();

        if (List.of(UKJENT, EU_EOS).contains(sakstype)) {
            eosVedtakService.endreVedtak(behandling, endreVedtakDto.getBegrunnelseKode(), endreVedtakDto.getFritekst(), endreVedtakDto.getFritekstSed());
        } else {
            throw new FunksjonellException("Vedtaksendring for sakstype " + sakstype + " er ikke støttet.");
        }
    }

    private void validerKanFattesVedtakAvTema(Behandling behandling) throws FunksjonellException {
        if (!behandling.kanResultereIVedtak()) {
            throw new FunksjonellException("Kan ikke fatte vedtak ved behandlingstema " + behandling.getTema().getBeskrivelse());
        }
    }

    private void fattVedtakForEos(Behandling behandling, EosFattVedtakDto fattVedtakDto) throws MelosysException {
        eosVedtakService.fattVedtak(
            behandling,
            fattVedtakDto.getBehandlingsresultatTypeKode(),
            fattVedtakDto.getFritekst(),
            fattVedtakDto.getFritekstSed(),
            fattVedtakDto.getMottakerinstitusjoner(),
            fattVedtakDto.getVedtakstype(),
            fattVedtakDto.getRevurderBegrunnelse()
        );
    }

    private void fattVedtakForFtrl(Behandling behandling, FtrlFattVedtakDto fattVedtakDto) throws MelosysException {
        ftrlVedtakService.fattVedtak(
            behandling,
            fattVedtakDto.getBehandlingsresultatTypeKode(),
            fattVedtakDto.getVedtakstype(),
            fattVedtakDto.getFritekstInnledning(),
            fattVedtakDto.getFritekstBegrunnelse()
        );
    }
}
