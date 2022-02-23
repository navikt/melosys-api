package no.nav.melosys.service.vedtak;


import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;

public class FattAvslagRequest {
    private final String fritekst;
    private final Behandlingsresultattyper behandlingsresultattype;
    private final Vedtakstyper vedtakstype;

    private FattAvslagRequest(Builder builder) {
        this.fritekst = builder.fritekst;
        this.behandlingsresultattype = builder.behandlingsresultattype;
        this.vedtakstype = builder.vedtakstype;
    }

    public String getFritekst() {
        return fritekst;
    }

    public Behandlingsresultattyper getBehandlingsresultattype() {
        return behandlingsresultattype;
    }

    public Vedtakstyper getVedtakstype() {
        return vedtakstype;
    }

    public static class Builder {
        private String fritekst;
        private Behandlingsresultattyper behandlingsresultattype;
        private Vedtakstyper vedtakstype;

        public Builder getThis() {
            return this;
        }

        public Builder medFritekst(String fritekst) {
            this.fritekst = fritekst;
            return this;
        }

        public FattAvslagRequest build() {
            return new FattAvslagRequest(this);
        }

        public Builder medBehandlingsresultattype(Behandlingsresultattyper behandlingsresultattype) {
            this.behandlingsresultattype = behandlingsresultattype;
            return this;
        }

        public Builder medVedtakstype(Vedtakstyper vedtakstype) {
            this.vedtakstype = vedtakstype;
            return this;
        }
    }
}
