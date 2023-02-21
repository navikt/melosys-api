package no.nav.melosys.service.kontroll.feature.ferdigbehandling.data;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.PeriodeOmLovvalg;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.person.Persondata;

public record FerdigbehandlingKontrollData(
    MedlemskapDokument medlemskapDokument,
    Persondata persondata,
    MottatteOpplysningerData mottatteOpplysningerData,
    PeriodeOmLovvalg lovvalgsperiode,
    Lovvalgsperiode opprinneligLovvalgsperiode,
    SaksopplysningerData saksopplysningerData
) {


    public static FerdigbehandlingKontrollData lagKontrollDataForAvslag(Persondata persondata,
                                                                        MottatteOpplysningerData mottatteOpplysningerData,
                                                                        SaksopplysningerData saksopplysningerData) {
        return new FerdigbehandlingKontrollData(null, persondata, mottatteOpplysningerData, null, null, saksopplysningerData);
    }

    public static FerdigbehandlingKontrollData lagKontrollDataForFTRL(Persondata persondata,
                                                                      MottatteOpplysningerData mottatteOpplysningerData,
                                                                      MedlemskapDokument medlemskapDokument) {
        return new FerdigbehandlingKontrollData(medlemskapDokument, persondata, mottatteOpplysningerData, null, null, null);
    }
}
