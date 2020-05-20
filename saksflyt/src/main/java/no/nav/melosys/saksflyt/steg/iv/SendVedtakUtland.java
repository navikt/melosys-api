package no.nav.melosys.saksflyt.steg.iv;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.AbstraktSendUtland;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.MYNDIGHET;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.ATTEST_A1;


@Component
public class SendVedtakUtland extends AbstraktSendUtland {
    private BehandlingService behandlingService;
    private BrevBestiller brevBestiller;
    private final SaksopplysningerService saksopplysningerService;

    @Autowired
    public SendVedtakUtland(@Qualifier("system") EessiService eessiService,
                            BehandlingService behandlingService,
                            BehandlingsresultatService behandlingsresultatService,
                            BrevBestiller brevBestiller,
                            SaksopplysningerService saksopplysningerService) {
        super(eessiService, behandlingsresultatService);
        this.behandlingService = behandlingService;
        this.brevBestiller = brevBestiller;
        this.saksopplysningerService = saksopplysningerService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.IV_SEND_SED;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {

        Behandling behandling = prosessinstans.getBehandling();
        if (behandling.erNorgeUtpekt()) {
            sendA012Sed(behandling.getId(), prosessinstans.getData(ProsessDataKey.YTTERLIGERE_INFO_SED));
        } else {
            super.sendUtland(avklarBucType(behandling), prosessinstans);
        }

        if (erArtikkel11(prosessinstans)) {
            prosessinstans.setSteg(ProsessSteg.IV_AVSLUTT_BEHANDLING);
        } else {
            prosessinstans.setSteg(ProsessSteg.IV_OPPRETT_AVGIFTSOPPGAVE);
        }

    }

    private void sendA012Sed(long behandlingID, String ytterligereInformasjon) throws MelosysException {
        SedDokument sedDokument = saksopplysningerService.hentSedOpplysninger(behandlingID);

        if (sedDokument.getErElektronisk()) {
            eessiService.sendGodkjenningArbeidFlereLand(behandlingID, ytterligereInformasjon);
        } else {
            throw new UnsupportedOperationException("Sending av brev-A012 er ikke implementert");
        }
    }

    private Brevbestilling lagBrevBestilling(Prosessinstans prosessinstans) throws IkkeFunnetException {
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        return new Brevbestilling.Builder().medDokumentType(ATTEST_A1)
            .medAvsender(hentSaksbehandler(prosessinstans))
            .medMottakere(Mottaker.av(MYNDIGHET))
            .medBehandling(behandling)
            .medBegrunnelseKode(hentBegrunnelseKode(prosessinstans))
            .build();
    }

    @Override
    protected void sendBrev(Prosessinstans prosessinstans) throws MelosysException {
        brevBestiller.bestill(lagBrevBestilling(prosessinstans));
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
            .map(BucType::fraBestemmelse)
            .orElseThrow(() -> new TekniskException("Finner ikke lovvalgsbestemmelse for behandling " + behandling.getId()));
    }
}
