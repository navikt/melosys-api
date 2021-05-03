package no.nav.melosys.service.vedtak;

public class FattFtrlVedtakRequest extends FattVedtakRequest {
    private final String fritekstInnledning;
    private final String fritekstBegrunnelse;

    private FattFtrlVedtakRequest(Builder builder) {
        super(builder);
        this.fritekstInnledning = builder.fritekstInnledning;
        this.fritekstBegrunnelse = builder.fritekstBegrunnelse;
    }

    public String getFritekstInnledning() {
        return fritekstInnledning;
    }

    public String getFritekstBegrunnelse() {
        return fritekstBegrunnelse;
    }

    public static class Builder extends FattVedtakRequest.Builder<Builder> {
        private String fritekstInnledning;
        private String fritekstBegrunnelse;

        @Override
        public Builder getThis() {
            return this;
        }

        public Builder medFritekstInnledning(String fritekstInnledning) {
            this.fritekstInnledning = fritekstInnledning;
            return this;
        }

        public Builder medFritekstBegrunnelse(String fritekstBegrunnelse) {
            this.fritekstBegrunnelse = fritekstBegrunnelse;
            return this;
        }

        public FattFtrlVedtakRequest build() {
            return new FattFtrlVedtakRequest(this);
        }
    }
}
