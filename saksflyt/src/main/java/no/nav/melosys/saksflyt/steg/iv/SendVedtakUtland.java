package no.nav.melosys.saksflyt.steg.iv;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.util.LovvalgBestemmelseUtils;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.AbstraktSendUtland;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.MYNDIGHET;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.ATTEST_A1;


@Component
public class SendVedtakUtland extends AbstraktSendUtland {
    private BehandlingService behandlingService;

    @Autowired
    public SendVedtakUtland(EessiService eessiService,
                            BehandlingService behandlingService,
                            BehandlingsresultatService behandlingsresultatService,
                            BrevBestiller brevBestiller,
                            LandvelgerService landvelgerService) {
        super(eessiService, brevBestiller, behandlingsresultatService, landvelgerService);
        this.behandlingService = behandlingService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.IV_SEND_SED;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {

        super.sendUtland(avklarBucType(prosessinstans.getBehandling()), prosessinstans);
        if (erArtikkel11(prosessinstans)) {
            prosessinstans.setSteg(ProsessSteg.IV_AVSLUTT_BEHANDLING);
        } else {
            prosessinstans.setSteg(ProsessSteg.IV_OPPRETT_AVGIFTSOPPGAVE);
        }

    }

    @Override
    protected Brevbestilling lagBrevBestilling(Prosessinstans prosessinstans) throws IkkeFunnetException {
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        return new Brevbestilling.Builder().medDokumentType(ATTEST_A1)
            .medAvsender(hentSaksbehandler(prosessinstans))
            .medMottakere(Mottaker.av(MYNDIGHET))
            .medBehandling(behandling)
            .medBegrunnelseKode(hentBegrunnelseKode(prosessinstans))
            .build();
    }

    @Override
    protected boolean skalSendesUtland(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.erInnvilgelse() &&
            behandlingsresultat.hentValidertLovvalgsperiode().getBestemmelse() != Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1;
    }

    private boolean erArtikkel11(Prosessinstans prosessinstans) throws IkkeFunnetException {
        return behandlingsresultatService.hentBehandlingsresultat(prosessinstans.getBehandling().getId())
            .hentValidertLovvalgsperiode()
            .erArtikkel11();
    }

    private BucType avklarBucType(Behandling behandling) throws IkkeFunnetException, TekniskException {
        return behandlingsresultatService.hentBehandlingsresultat(behandling.getId())
            .getLovvalgsperioder().stream().findFirst()
            .map(Lovvalgsperiode::getBestemmelse)
            .map(LovvalgBestemmelseUtils::hentBucTypeFraBestemmelse)
            .orElseThrow(() -> new TekniskException("Finner ikke lovvalgsbestemmelse for behandling " + behandling.getId()));
    }
}
