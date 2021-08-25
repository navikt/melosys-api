package no.nav.melosys.service.unntaksperiode;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;

import java.util.Objects;

public class UnntaksperiodeGodkjenning {

    private final boolean varsleUtland;
    private final String fritekst;
    private final Unntaksperiode endretPeriode;
    private final LovvalgBestemmelse lovvalgsbestemmelse;

    private UnntaksperiodeGodkjenning(Builder builder) {
        this.varsleUtland = builder.varsleUtland;
        this.fritekst = builder.fritekst;
        this.endretPeriode = builder.endretPeriode;
        this.lovvalgsbestemmelse = builder.lovvalgsbestemmelse;
    }

    public boolean isVarsleUtland() {
        return varsleUtland;
    }

    public String getFritekst() {
        return fritekst;
    }

    public Unntaksperiode getEndretPeriode() {
        return endretPeriode;
    }

    public LovvalgBestemmelse getLovvalgsbestemmelse() {
        return lovvalgsbestemmelse;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnntaksperiodeGodkjenning that = (UnntaksperiodeGodkjenning) o;
        return varsleUtland == that.varsleUtland
            && Objects.equals(fritekst, that.fritekst)
            && Objects.equals(endretPeriode, that.endretPeriode)
            && Objects.equals(lovvalgsbestemmelse, that.lovvalgsbestemmelse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(varsleUtland, fritekst, endretPeriode, lovvalgsbestemmelse);
    }

    public static class Builder {
        private boolean varsleUtland;
        private String fritekst;
        private Unntaksperiode endretPeriode;
        private LovvalgBestemmelse lovvalgsbestemmelse;

        public Builder varsleUtland(boolean varsleUtland) {
            this.varsleUtland = varsleUtland;
            return this;
        }

        public Builder fritekst(String fritekst) {
            this.fritekst = fritekst;
            return this;
        }

        public Builder unnntaksperiode(Unntaksperiode endretPeriode) {
            this.endretPeriode = endretPeriode;
            return this;
        }

        public Builder lovvalgsbestemmelse(LovvalgBestemmelse lovvalgsbestemmelse) {
            this.lovvalgsbestemmelse = lovvalgsbestemmelse;
            return this;
        }

        public UnntaksperiodeGodkjenning build() {
            return new UnntaksperiodeGodkjenning(this);
        }
    }
}
