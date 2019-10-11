package no.nav.melosys.service.dokument.sed.datagrunnlag;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class SedDataGrunnlagMedSoknad extends BrevDataGrunnlag implements SedDataGrunnlag {
    public SedDataGrunnlagMedSoknad(Behandling behandling, KodeverkService kodeverkService, AvklarteVirksomheterService avklarteVirksomheterService, AvklartefaktaService avklartefaktaService) throws TekniskException {
        super(behandling, kodeverkService, avklarteVirksomheterService, avklartefaktaService);
    }
}
