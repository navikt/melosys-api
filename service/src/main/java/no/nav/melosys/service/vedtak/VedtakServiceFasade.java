package no.nav.melosys.service.vedtak;

import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.behandling.BehandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Sakstyper.EU_EOS;

@Service
public class VedtakServiceFasade {

    private final BehandlingService behandlingService;
    private final EosVedtakService eosVedtakService;
    private final EosVedtakSystemService eosVedtakSystemService;

    @Autowired
    public VedtakServiceFasade(BehandlingService behandlingService, EosVedtakService eosVedtakService, EosVedtakSystemService eosVedtakSystemService) {
        this.behandlingService = behandlingService;
        this.eosVedtakService = eosVedtakService;
        this.eosVedtakSystemService = eosVedtakSystemService;
    }

    @Transactional(rollbackFor = MelosysException.class, noRollbackFor = {ValideringException.class})
    public void fattVedtak(long behandlingID, Behandlingsresultattyper behandlingsresultattype) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);

        eosVedtakSystemService.fattVedtak(behandling, behandlingsresultattype, null, null,
            null, Vedtakstyper.FØRSTEGANGSVEDTAK, null);
    }

    @Transactional(rollbackFor = MelosysException.class, noRollbackFor = {ValideringException.class})
    public void fattVedtak(long behandlingID, Behandlingsresultattyper behandlingsresultatType,
                           String fritekst, String fritekstSed, Set<String> mottakerinstitusjoner,
                           Vedtakstyper vedtakstype, String revurderBegrunnelse) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);

        validerKanFattesVedtakAvTema(behandling);

        Sakstyper sakstype = behandling.getFagsak().getType();

        if (sakstype == EU_EOS) {
            eosVedtakService.fattVedtak(behandling, behandlingsresultatType, fritekst, fritekstSed, mottakerinstitusjoner, vedtakstype, revurderBegrunnelse);
        } else {
            throw new FunksjonellException("Vedtaksfatting for sakstype " + sakstype + " er ikke støttet.");
        }
    }

    @Transactional(rollbackFor = MelosysException.class, noRollbackFor = {ValideringException.class})
    public void endreVedtak(long behandlingID, Endretperiode endretperiode, String fritekst, String fritekstSed) throws FunksjonellException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        Sakstyper sakstype = behandling.getFagsak().getType();

        if (sakstype == EU_EOS) {
            eosVedtakService.endreVedtak(behandling, endretperiode, fritekst, fritekstSed);
        } else {
            throw new FunksjonellException("Vedtaksendring for sakstype " + sakstype + " er ikke støttet.");
        }
    }

    private void validerKanFattesVedtakAvTema(Behandling behandling) throws FunksjonellException {
        if (!behandling.kanResultereIVedtak()) {
            throw new FunksjonellException("Kan ikke fatte vedtak ved behandlingstema " + behandling.getTema().getBeskrivelse());
        }
    }
}
