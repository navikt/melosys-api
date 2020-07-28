package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.service.registeropplysninger.RegisteropplysningerFactory.utledSaksopplysningTyper;

@Component("UnntakFraMedlemskapHentRegisteropplysninger")
public class HentRegisteropplysninger implements StegBehandler {

    private final BehandlingService behandlingService;
    private final TpsFasade tpsFasade;
    private final RegisteropplysningerService registeropplysningerService;

    @Autowired
    public HentRegisteropplysninger(BehandlingService behandlingService, TpsFasade tpsFasade,
                                    RegisteropplysningerService registeropplysningerService) {
        this.behandlingService = behandlingService;
        this.tpsFasade = tpsFasade;
        this.registeropplysningerService = registeropplysningerService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_HENT_REGISTEROPPLYSNINGER;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        String aktørId = prosessinstans.getData(ProsessDataKey.AKTØR_ID);

        String fnr = tpsFasade.hentIdentForAktørId(aktørId);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, fnr);

        SedDokument sedDokument = SaksopplysningerUtils.hentSedDokument(behandling);
        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(behandling.getId())
                .saksopplysningTyper(utledSaksopplysningTyper(behandling.getTema()))
                .fom(sedDokument.getLovvalgsperiode().getFom())
                .tom(sedDokument.getLovvalgsperiode().getTom())
                .fnr(fnr)
                .build());

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_REGISTERKONTROLL);
    }
}
