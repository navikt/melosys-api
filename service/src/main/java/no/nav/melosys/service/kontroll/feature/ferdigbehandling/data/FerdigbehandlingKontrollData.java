package no.nav.melosys.service.kontroll.feature.ferdigbehandling.data;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.PeriodeOmLovvalg;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.person.Persondata;

public record FerdigbehandlingKontrollData(MedlemskapDokument medlemskapDokument,
                                           Persondata persondata,
                                           BehandlingsgrunnlagData behandlingsgrunnlagData,
                                           PeriodeOmLovvalg lovvalgsperiode,
                                           Lovvalgsperiode opprinneligLovvalgsperiode) {


    public static FerdigbehandlingKontrollData lagKontrollDataForAvslag(Persondata persondata,
                                                                        BehandlingsgrunnlagData behandlingsgrunnlagData) {
        return new FerdigbehandlingKontrollData(null, persondata, behandlingsgrunnlagData, null, null);
    }
}
