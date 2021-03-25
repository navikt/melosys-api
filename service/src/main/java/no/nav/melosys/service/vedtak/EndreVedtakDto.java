package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;

public class EndreVedtakDto {
    private Endretperiode begrunnelseKode;
    private String fritekst;
    private String fritekstSed;

    private EndreVedtakDto(Builder builder) {
        this.begrunnelseKode = builder.begrunnelseKode;
        this.fritekst = builder.fritekst;
        this.fritekstSed = builder.fritekstSed;
    }

    public Endretperiode getBegrunnelseKode() {
        return begrunnelseKode;
    }

    public String getFritekst() {
        return fritekst;
    }

    public String getFritekstSed() {
        return fritekstSed;
    }

    public static final class Builder {
        private Endretperiode begrunnelseKode;
        private String fritekst;
        private String fritekstSed;

        public Builder medBegrunnelseKode(Endretperiode begrunnelseKode) {
            this.begrunnelseKode = begrunnelseKode;
            return this;
        }

        public Builder medFritekst(String fritekst) {
            this.fritekst = fritekst;
            return this;
        }

        public Builder medFritekstSed(String fritekstSed) {
            this.fritekstSed = fritekstSed;
            return this;
        }

        public EndreVedtakDto build() {
            return new EndreVedtakDto(this);
        }
    }
}