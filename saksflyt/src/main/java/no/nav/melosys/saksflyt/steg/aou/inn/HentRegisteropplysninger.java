package no.nav.melosys.saksflyt.steg.aou.inn;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.saksflyt.felles.HentOpplysningerFelles;
import no.nav.melosys.saksflyt.felles.RegisteropplysningerRequest;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.BehandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakHentRegisteropplysninger")
public class HentRegisteropplysninger extends AbstraktStegBehandler {

    private final HentOpplysningerFelles hentOpplysningerFelles;
    private final BehandlingService behandlingService;
    private final TpsFasade tpsFasade;

    @Autowired
    public HentRegisteropplysninger(HentOpplysningerFelles hentOpplysningerFelles, BehandlingService behandlingService, TpsFasade tpsFasade) {
        this.hentOpplysningerFelles = hentOpplysningerFelles;
        this.behandlingService = behandlingService;
        this.tpsFasade = tpsFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return null; // todo - slette gamle steg og legg til nytt
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        String aktørId = prosessinstans.getData(ProsessDataKey.AKTØR_ID);

        String fnr = tpsFasade.hentIdentForAktørId(aktørId);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, fnr);

        SedDokument sedDokument = SaksopplysningerUtils.hentSedDokument(behandling);
        hentOpplysningerFelles.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandling(behandling)
                .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .personopplysninger()
                    .medlemskapsopplysninger()
                    .inntektsopplysninger()
                    .utbetalingsopplysninger()
                    .arbeidsforholdopplysninger()
                    .build())
                .fom(sedDokument.getLovvalgsperiode().getFom())
                .tom(sedDokument.getLovvalgsperiode().getTom())
                .fnr(fnr)
                .build());

        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_REGISTERKONTROLL);
    }
}
