package no.nav.melosys.saksflyt.steg.aou.inn;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("AnmodningUnntakMottakHentRegisteropplysninger")
public class HentRegisteropplysninger extends AbstraktStegBehandler {

    private final RegisteropplysningerService registeropplysningerService;
    private final BehandlingService behandlingService;
    private final TpsFasade tpsFasade;

    @Autowired
    public HentRegisteropplysninger(RegisteropplysningerService registeropplysningerService, BehandlingService behandlingService, TpsFasade tpsFasade) {
        this.registeropplysningerService = registeropplysningerService;
        this.behandlingService = behandlingService;
        this.tpsFasade = tpsFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_HENT_REGISTEROPPLYSNINGER;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        String aktørId = prosessinstans.getData(ProsessDataKey.AKTØR_ID);

        String fnr = tpsFasade.hentIdentForAktørId(aktørId);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, fnr);

        SedDokument sedDokument = SaksopplysningerUtils.hentSedDokument(behandling);
        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(behandling.getId())
                .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .personopplysninger()
                    .personhistorikkopplysninger()
                    .medlemskapsopplysninger()
                    .inntektsopplysninger()
                    .utbetalingsopplysninger()
                    .arbeidsforholdopplysninger()
                    .organisasjonsopplysninger()
                    .build())
                .fom(sedDokument.getLovvalgsperiode().getFom())
                .tom(sedDokument.getLovvalgsperiode().getTom())
                .fnr(fnr)
                .build());

        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_REGISTERKONTROLL);
    }
}
