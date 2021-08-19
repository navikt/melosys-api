package no.nav.melosys.service.unntaksperiode;

public class UnntaksperiodeGodkjenning {

    private final boolean varsleUtland;
    private final String fritekst;
    private final Unntaksperiode endretPeriode;
    private final String lovvalgsbestemmelse;

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

    public String getLovvalgsbestemmelse() {
        return lovvalgsbestemmelse;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean varsleUtland;
        private String fritekst;
        private Unntaksperiode endretPeriode;
        private String lovvalgsbestemmelse;

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

        public Builder lovvalgsbestemmelse(String lovvalgsbestemmelse) {
            this.lovvalgsbestemmelse = lovvalgsbestemmelse;
            return this;
        }

        public UnntaksperiodeGodkjenning build() {
            return new UnntaksperiodeGodkjenning(this);
        }
    }
}
