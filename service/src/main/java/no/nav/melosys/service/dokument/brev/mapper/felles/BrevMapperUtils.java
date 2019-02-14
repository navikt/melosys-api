package no.nav.melosys.service.dokument.brev.mapper.felles;

import java.time.LocalDate;
import java.util.Set;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.TekniskException;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatype.ARBEIDSLAND;
import static no.nav.melosys.domain.util.SaksopplysningerUtils.hentPersonDokument;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.convertToXMLGregorianCalendarRemoveTimezone;

public class BrevMapperUtils {

    // Slå opp arbeidsland i avklartefakta, fall tilbake på søknaden (kan overkjøres av saksbehandler for sokkel/skip).
    public static String hentArbeidsLand(Behandling behandling, Behandlingsresultat resultat) {
        return resultat.finnAvklartFaktum(ARBEIDSLAND).map(Avklartefakta::getSubjekt)
            .orElseGet(() -> hentArbeidslandFraSøknaden(behandling));
    }

    private static String hentArbeidslandFraSøknaden(Behandling behandling) {
        try {
            SoeknadDokument soeknadDokument = SaksopplysningerUtils.hentSøknadDokument(behandling);
            ArbeidUtland arbeidUtland = soeknadDokument.arbeidUtland.stream().findFirst().orElseThrow(() -> new IllegalArgumentException("arbeidUtland mangler"));
            return arbeidUtland.adresse.landKode;
        } catch (TekniskException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String hentSammensattNavn(Behandling behandling) {
        try {
            PersonDokument personDokument = hentPersonDokument(behandling);
            return personDokument.sammensattNavn;
        } catch (TekniskException e) {
            throw new IllegalStateException(e);
        }
    }

    public static XMLGregorianCalendar lagXmlDato(LocalDate dato) {
        try {
            return convertToXMLGregorianCalendarRemoveTimezone(dato);
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Kan ikke lage DatatypeConverterFactory.", e);
        }
    }

    public static void validerLovvalgsperioder(Set<Lovvalgsperiode> perioder) {
        if (perioder.size() != 1) {
            throw new UnsupportedOperationException(String.format("Antall lovvalgsperioder (%s) ulik 1 støttes ikke i første versjon av Melosys.",
                perioder.size()));
        }
    }

}
