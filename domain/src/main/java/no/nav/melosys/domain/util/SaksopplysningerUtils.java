package no.nav.melosys.domain.util;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.TekniskException;

public final class SaksopplysningerUtils {

    private SaksopplysningerUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Henter det første dokumentet som finnes for en gitt behandling og en gitt saksopplysningType.
     */
    public static Optional<SaksopplysningDokument> hentDokument(Behandling behandling, SaksopplysningType saksopplysningType) {
        if (behandling == null) {
            return Optional.empty();
        }
        return behandling.getSaksopplysninger().stream()
            .filter(saksopplysning -> saksopplysning.getType().equals(saksopplysningType))
            .findFirst().map(Saksopplysning::getDokument);
    }

    public static PersonDokument hentPersonDokument(Behandling behandling) throws TekniskException {
        Optional<SaksopplysningDokument> saksopplysning = hentDokument(behandling, SaksopplysningType.PERSOPL);
        return (PersonDokument) saksopplysning
                .orElseThrow(() -> new TekniskException("Finner ikke persondokument"));
    }

    public static PersonhistorikkDokument hentPersonhistorikkDokument(Behandling behandling) throws TekniskException {
        Optional<SaksopplysningDokument> saksopplysning = hentDokument(behandling, SaksopplysningType.PERSHIST);
        return (PersonhistorikkDokument) saksopplysning
            .orElseThrow(() -> new TekniskException("Finner ikke personhistorikkDokument"));
    }

    public static SoeknadDokument hentSøknadDokument(Behandling behandling) throws TekniskException {
        Optional<SaksopplysningDokument> saksopplysning = hentDokument(behandling, SaksopplysningType.SØKNAD);
        return (SoeknadDokument) saksopplysning
                .orElseThrow(() -> new TekniskException("Finner ikke søknaddokument"));
    }

    public static MedlemskapDokument hentMedlemskapDokument(Behandling behandling) throws TekniskException {
        Optional<SaksopplysningDokument> saksopplysning = hentDokument(behandling, SaksopplysningType.MEDL);
        return (MedlemskapDokument) saksopplysning
                .orElseThrow(() -> new TekniskException("Finner ikke medlemskapdokument"));
    }

    public static ArbeidsforholdDokument hentArbeidsforholdDokument(Behandling behandling) throws TekniskException {
        Optional<SaksopplysningDokument> saksopplysning = hentDokument(behandling, SaksopplysningType.ARBFORH);
        return (ArbeidsforholdDokument) saksopplysning
                .orElseThrow(() -> new TekniskException("Finner ikke arbeidsforholddokument"));
    }

    public static SedDokument hentSedDokument(Behandling behandling) throws TekniskException {
        Optional<SaksopplysningDokument> saksopplysning = hentDokument(behandling, SaksopplysningType.SEDOPPL);
        return (SedDokument) saksopplysning
                .orElseThrow(() -> new TekniskException("Finner ikke seddokument"));
    }

    public static InntektDokument hentInntektDokument(Behandling behandling) throws TekniskException {
        Optional<SaksopplysningDokument> saksopplysning = hentDokument(behandling, SaksopplysningType.INNTK);
        return (InntektDokument) saksopplysning
                .orElseThrow(() -> new TekniskException("Finner ikke inntektdokument"));
    }

    public static String hentSammensattNavn(Behandling behandling) {
        try {
            PersonDokument personDokument = hentPersonDokument(behandling);
            return personDokument.sammensattNavn;
        } catch (TekniskException e) {
            throw new IllegalStateException(e);
        }
    }
}
