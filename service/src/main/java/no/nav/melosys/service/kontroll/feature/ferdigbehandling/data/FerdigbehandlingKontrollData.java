package no.nav.melosys.service.kontroll.feature.ferdigbehandling.data;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.PeriodeOmLovvalg;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.person.Persondata;

public record FerdigbehandlingKontrollData(MedlemskapDokument medlemskapDokument,
                                           Persondata persondata,
                                           MottatteOpplysningerData mottatteOpplysningerData,
                                           PeriodeOmLovvalg lovvalgsperiode,
                                           Lovvalgsperiode opprinneligLovvalgsperiode) {


    public static FerdigbehandlingKontrollData lagKontrollDataForAvslag(Persondata persondata,
                                                                        MottatteOpplysningerData mottatteOpplysningerData) {
        return new FerdigbehandlingKontrollData(null, persondata, mottatteOpplysningerData, null, null);
    }

    public static FerdigbehandlingKontrollData lagKontrollDataForFTRL(MedlemskapDokument medlemskapDokument,Persondata persondata,
                                                                        MottatteOpplysningerData mottatteOpplysningerData) {
        return new FerdigbehandlingKontrollData(medlemskapDokument, persondata, mottatteOpplysningerData, null, null);
    }
}
