package no.nav.melosys.domain.eessi.melding;

import java.util.Objects;

public class AnmodningUnntak {
    private String unntakFraLovvalgsland;
    private String unntakFraLovvalgsbestemmelse;

    public String getUnntakFraLovvalgsland() {
        return unntakFraLovvalgsland;
    }

    public void setUnntakFraLovvalgsland(String unntakFraLovvalgsland) {
        this.unntakFraLovvalgsland = unntakFraLovvalgsland;
    }

    public String getUnntakFraLovvalgsbestemmelse() {
        return unntakFraLovvalgsbestemmelse;
    }

    public void setUnntakFraLovvalgsbestemmelse(String unntakFraLovvalgsbestemmelse) {
        this.unntakFraLovvalgsbestemmelse = unntakFraLovvalgsbestemmelse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnmodningUnntak)) return false;
        AnmodningUnntak that = (AnmodningUnntak) o;
        return Objects.equals(getUnntakFraLovvalgsland(), that.getUnntakFraLovvalgsland()) &&
            Objects.equals(getUnntakFraLovvalgsbestemmelse(), that.getUnntakFraLovvalgsbestemmelse());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUnntakFraLovvalgsland(), getUnntakFraLovvalgsbestemmelse());
    }
}
