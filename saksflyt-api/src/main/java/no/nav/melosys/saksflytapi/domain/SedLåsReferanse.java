package no.nav.melosys.saksflytapi.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public class SedLåsReferanse {

    private final String rinaSaksnummer;
    private final String sedID;
    private final String sedVersjon;

    private static final Pattern pattern = Pattern.compile("[^_]*_[^_]*_\\d+$");

    public SedLåsReferanse(String referanse) {
        if (!erGyldigReferanse(referanse)) {
            throw new IllegalArgumentException(referanse + " er ikke gyldig SED-referanse");
        }

        String[] ref = referanse.split("_");
        this.rinaSaksnummer = ref[0];
        this.sedID = ref[1];
        this.sedVersjon = ref[2];
    }

    public String getReferanse() {
        return rinaSaksnummer;
    }

    public String getIdentifikator() {
        return String.format("%s_%s", sedID, sedVersjon);
    }

    public static boolean erGyldigReferanse(String referanse) {
        return referanse != null && pattern.matcher(referanse).find();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SedLåsReferanse that = (SedLåsReferanse) o;
        return Objects.equals(rinaSaksnummer, that.rinaSaksnummer) && Objects.equals(sedID, that.sedID) && Objects.equals(sedVersjon, that.sedVersjon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rinaSaksnummer, sedID, sedVersjon);
    }

    @Override
    public String toString() {
        return String.format("%s_%s_%s", rinaSaksnummer, sedID, sedVersjon);
    }
}
