package no.nav.melosys.service.kontroll.feature.ferdigbehandling.data;

import java.util.List;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.person.Persondata;

public record FerdigbehandlingKontrollData(
    MedlemskapDokument medlemskapDokument,
    Persondata persondata,
    MottatteOpplysningerData mottatteOpplysningerData,
    Lovvalgsperiode lovvalgsperiode,
    Lovvalgsperiode opprinneligLovvalgsperiode,
    SaksopplysningerData saksopplysningerData,
    Behandlingstema behandlingstema,
    Aktoer representant,
    OrganisasjonDokument organisasjonDokument,
    Persondata persondataRepresentant,
    List<Medlemskapsperiode> medlemskapsperioder
    ) {

    public static FerdigbehandlingKontrollData lagKontrollDataForAvslag(Persondata persondata,
                                                                        MottatteOpplysningerData mottatteOpplysningerData,
                                                                        SaksopplysningerData saksopplysningerData, Aktoer representant, OrganisasjonDokument organisasjonDokument, Persondata persondataRepresentant) {
        return new FerdigbehandlingKontrollData(null, persondata, mottatteOpplysningerData, null, null, saksopplysningerData, null, representant, organisasjonDokument, persondataRepresentant, null);
    }

    public static FerdigbehandlingKontrollData lagKontrollDataForFTRL(Persondata persondata,
                                                                      MottatteOpplysningerData mottatteOpplysningerData,
                                                                      MedlemskapDokument medlemskapDokument,
                                                                      Aktoer representant,
                                                                      OrganisasjonDokument organisasjonDokument,
                                                                      Persondata persondataRepresentant,
                                                                      List<Medlemskapsperiode> medlemskapsperioder) {
        return new FerdigbehandlingKontrollData(medlemskapDokument, persondata, mottatteOpplysningerData, null, null, null, null, representant, organisasjonDokument, persondataRepresentant, medlemskapsperioder);
    }
}
