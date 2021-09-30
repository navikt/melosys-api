package no.nav.melosys.service.unntaksperiode;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;

public record UnntaksperiodeGodkjenning(boolean varsleUtland,
                                       String fritekst,
                                       Unntaksperiode endretPeriode,
                                       LovvalgBestemmelse lovvalgsbestemmelse) {

    public static Builder builder() {
        return new Builder();
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

        public Builder endretPeriode(Unntaksperiode endretPeriode) {
            this.endretPeriode = endretPeriode;
            return this;
        }

        public Builder lovvalgsbestemmelse(LovvalgBestemmelse lovvalgsbestemmelse) {
            this.lovvalgsbestemmelse = lovvalgsbestemmelse;
            return this;
        }

        public UnntaksperiodeGodkjenning build() {
            return new UnntaksperiodeGodkjenning(varsleUtland, fritekst, endretPeriode, lovvalgsbestemmelse);
        }
    }
}
