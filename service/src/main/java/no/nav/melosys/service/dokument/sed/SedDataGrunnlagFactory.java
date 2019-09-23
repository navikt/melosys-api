package no.nav.melosys.service.dokument.sed;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlag;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagMedSoknad;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagUtenSoknad;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SedDataGrunnlagFactory {
    private final AvklartefaktaService avklartefaktaService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final KodeverkService kodeverkService;

    @Autowired
    public SedDataGrunnlagFactory(AvklartefaktaService avklartefaktaService,
                                       AvklarteVirksomheterSystemService avklarteVirksomheterService,
                                       KodeverkService kodeverkService) {
        this.avklartefaktaService = avklartefaktaService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.kodeverkService = kodeverkService;
    }

    public SedDataGrunnlag av(Behandling behandling) throws TekniskException {
        Optional<SoeknadDokument> søknad = SaksopplysningerUtils.finnSøknadDokument(behandling);
        if (søknad.isPresent()) {
            return new SedDataGrunnlagMedSoknad(behandling, kodeverkService, avklarteVirksomheterService, avklartefaktaService);
        } else {
            return new SedDataGrunnlagUtenSoknad(behandling, kodeverkService);
        }
    }
}
