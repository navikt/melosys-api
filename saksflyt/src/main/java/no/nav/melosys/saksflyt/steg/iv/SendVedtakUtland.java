package no.nav.melosys.saksflyt.steg.iv;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.AbstraktSendUtland;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.MYNDIGHET;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.ATTEST_A1;


@Component
public class SendVedtakUtland extends AbstraktSendUtland {
    private static final Logger log = LoggerFactory.getLogger(SendVedtakUtland.class);

    private BehandlingService behandlingService;
    private BrevBestiller brevBestiller;

    @Autowired
    public SendVedtakUtland(EessiService eessiService,
                            BehandlingService behandlingService,
                            BehandlingsresultatService behandlingsresultatService,
                            BrevBestiller brevBestiller) {
        super(eessiService, behandlingsresultatService);
        this.behandlingService = behandlingService;
        this.brevBestiller = brevBestiller;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.IV_SEND_SED;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException {
        try {
            Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
            super.utfør(prosessinstans);

            Brevbestilling A1_Myndighet = new Brevbestilling.Builder().medDokumentType(ATTEST_A1)
                .medAvsender(hentSaksbehandler(prosessinstans, new Behandlingsresultat()))
                .medMottakere(Mottaker.av(MYNDIGHET))
                .medBehandling(behandling)
                .medBegrunnelseKode(hentBegrunnelseKode(prosessinstans)).build();
            brevBestiller.bestill(A1_Myndighet);


            if (erArtikkel11(prosessinstans)) {
                prosessinstans.setSteg(ProsessSteg.IV_AVSLUTT_BEHANDLING);
            } else {
                prosessinstans.setSteg(ProsessSteg.IV_OPPRETT_AVGIFTSOPPGAVE);
            }
        } catch (Exception ex) {
            log.error("Kan ikke opprette og sende sed for behandling {}", prosessinstans.getBehandling().getId(), ex);
            prosessinstans.setSteg(ProsessSteg.FEILET_MASKINELT);
        }
    }

    @Override
    protected boolean skalSendeSed(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.erInnvilgelse() &&
            behandlingsresultat.hentValidertLovvalgsperiode().getBestemmelse() != Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1;
    }
    
    private boolean erArtikkel11(Prosessinstans prosessinstans) throws IkkeFunnetException {
        return behandlingsresultatService.hentBehandlingsresultat(prosessinstans.getBehandling().getId())
                .hentValidertLovvalgsperiode()
                .erArtikkel11();
    }

    private String hentBegrunnelseKode(Prosessinstans prosessinstans) {
        Endretperiode endretPeriodeBegrunnelseKode = prosessinstans.getData(ProsessDataKey.BEGRUNNELSEKODE, Endretperiode.class);
        String begrunnelseKode = null;
        if (endretPeriodeBegrunnelseKode != null) {
            begrunnelseKode = endretPeriodeBegrunnelseKode.getKode();
        }
        return begrunnelseKode;
    }

    private String hentSaksbehandler(Prosessinstans prosessinstans, Behandlingsresultat behandlingsresultat) {
        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);
        if (StringUtils.isEmpty(saksbehandler) && behandlingsresultat.erAutomatisert()) {
            saksbehandler = prosessinstans.getBehandling().getFagsak().getRegistrertAv();
        }
        return saksbehandler;
    }
}
