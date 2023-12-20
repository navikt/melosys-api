package no.nav.melosys.service.kontroll.feature.ferdigbehandling.data;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.PeriodeOmLovvalg;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.person.Persondata;

import java.util.List;

public record FerdigbehandlingKontrollData(
    MedlemskapDokument medlemskapDokument,
    Persondata persondata,
    MottatteOpplysningerData mottatteOpplysningerData,
    PeriodeOmLovvalg lovvalgsperiode,
    Lovvalgsperiode opprinneligLovvalgsperiode,
    SaksopplysningerData saksopplysningerData,
    Behandlingstema behandlingstema,
    Aktoer fullmektig,
    OrganisasjonDokument organisasjonDokument,
    Persondata persondataTilFullmektig,
    List<Medlemskapsperiode> medlemskapsperioder
    ) {

    public static FerdigbehandlingKontrollData lagKontrollDataForAvslag(Persondata persondata,
                                                                        MottatteOpplysningerData mottatteOpplysningerData,
                                                                        SaksopplysningerData saksopplysningerData, Aktoer fullmektig, OrganisasjonDokument organisasjonDokument, Persondata persondataTilFullmektig) {
        return new FerdigbehandlingKontrollData(null, persondata, mottatteOpplysningerData, null, null, saksopplysningerData, null, fullmektig, organisasjonDokument, persondataTilFullmektig, null);
    }

    public static FerdigbehandlingKontrollData lagKontrollDataForFTRL(Persondata persondata,
                                                                      MottatteOpplysningerData mottatteOpplysningerData,
                                                                      MedlemskapDokument medlemskapDokument,
                                                                      Aktoer fullmektig,
                                                                      OrganisasjonDokument organisasjonDokument,
                                                                      Persondata persondataTilFullmektig,
                                                                      List<Medlemskapsperiode> medlemskapsperioder) {
        return new FerdigbehandlingKontrollData(medlemskapDokument, persondata, mottatteOpplysningerData, null, null, null, null, fullmektig, organisasjonDokument, persondataTilFullmektig, medlemskapsperioder);
    }
}
