package no.nav.melosys.saksflyt.steg.ul.svar;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.MedlemsperiodeType;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.UtpekingAvvis;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import no.nav.melosys.integrasjon.eessi.dto.UtpekingAvvisDto;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.SedDataGrunnlagFactory;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlag;
import org.springframework.beans.factory.annotation.Autowired;

public class UtpekLandAvvis extends AbstraktStegBehandler {

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final EessiConsumer eessiConsumer;
    private final SedDataBygger sedDataBygger;
    private final SedDataGrunnlagFactory sedDataGrunnlagFactory;

    @Autowired
    public UtpekLandAvvis(BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService,
                          EessiConsumer eessiConsumer, SedDataBygger sedDataBygger,
                          SedDataGrunnlagFactory sedDataGrunnlagFactory) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.eessiConsumer = eessiConsumer;
        this.sedDataBygger = sedDataBygger;
        this.sedDataGrunnlagFactory = sedDataGrunnlagFactory;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AFL_SVAR_SEND_SED;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        Behandling behandling = prosessinstans.getBehandling();
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
        SedDataGrunnlag dataGrunnlag = sedDataGrunnlagFactory.av(behandling);

        String rinaSaksnummer = SaksopplysningerUtils.hentSedDokument(behandling).getRinaSaksnummer();
        UtpekingAvvis utpekingAvvis = prosessinstans.getData(ProsessDataKey.UTPEKING_AVVIS, UtpekingAvvis.class);

        SedDataDto sedDataDto = sedDataBygger.lagUtkast(dataGrunnlag, behandlingsresultat, MedlemsperiodeType.LOVVALGSPERIODE);
        sedDataDto.setYtterligereInformasjon(utpekingAvvis.getFritekst());
        sedDataDto.setUtpekingAvvis(new UtpekingAvvisDto(
            utpekingAvvis.getNyttLovvalgsland(),
            utpekingAvvis.getBegrunnelse(),
            utpekingAvvis.isEtterspørInformasjon()
        ));

        eessiConsumer.sendSedPåEksisterendeBuc(sedDataDto, rinaSaksnummer, SedType.A004);
    }
}
