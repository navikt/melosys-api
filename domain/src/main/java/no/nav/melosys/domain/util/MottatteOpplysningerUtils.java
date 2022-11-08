package no.nav.melosys.domain.util;

import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Bostedsland;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import org.apache.commons.lang3.StringUtils;

/**
 * Metoder for å trekke ut opplysninger fra et {@code MottatteOpplysningerData}.
 */
public final class MottatteOpplysningerUtils {

    private MottatteOpplysningerUtils() {
        throw new UnsupportedOperationException();
    }

    public static Periode hentPeriode(MottatteOpplysningerData soeknadDokument) {
        return soeknadDokument.periode;
    }

    /**
     * Returnerer søknadsland som landkoder,
     * og sjekker at det er minst et søknadsland oppgitt i søknad
     */
    public static List<Land_iso2> hentSøknadslandkoder(MottatteOpplysningerData grunnlagdata) {
        Soeknadsland soeknadsland = hentSøknadsland(grunnlagdata);
        List<String> søknadslandkoder = soeknadsland.landkoder;
        if (søknadslandkoder.isEmpty() && !soeknadsland.erUkjenteEllerAlleEosLand) {
            throw new IllegalStateException("Søknad mangler søknadsland og land er ikke markert som ukjente eller alle Eøs-land.");
        }
        return søknadslandkoder.stream()
            .map(Land_iso2::valueOf)
            .toList();
    }

    public static Soeknadsland hentSøknadsland(MottatteOpplysningerData grunnlagdata) {
        return grunnlagdata.soeknadsland;
    }

    public static StrukturertAdresse hentBostedsadresse(MottatteOpplysningerData grunnlagdata) {
        StrukturertAdresse oppgittAdresse = grunnlagdata.bosted.oppgittAdresse;
        if ((StringUtils.isNotEmpty(oppgittAdresse.getGatenavn()) ||
            StringUtils.isNotEmpty(oppgittAdresse.getHusnummerEtasjeLeilighet()) ||
            StringUtils.isNotEmpty(oppgittAdresse.getRegion()) ||
            StringUtils.isNotEmpty(oppgittAdresse.getPostnummer()) ||
            StringUtils.isNotEmpty(oppgittAdresse.getPoststed())) &&
            StringUtils.isNotEmpty(oppgittAdresse.getLandkode())) {
            return oppgittAdresse;
        } else {
            return null;
        }
    }

    public static Optional<Bostedsland> hentOppgittBostedsland(MottatteOpplysningerData grunnlagdata) {
        return Optional.ofNullable(grunnlagdata.bosted.oppgittAdresse.getLandkode()).map(Bostedsland::new);
    }
}
