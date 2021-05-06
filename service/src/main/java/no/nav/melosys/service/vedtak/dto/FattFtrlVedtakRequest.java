package no.nav.melosys.service.vedtak.dto;

public class FattFtrlVedtakRequest extends FattVedtakRequest {
    private final String fritekstBegrunnelse;

    private FattFtrlVedtakRequest(Builder builder) {
        super(builder);
        this.fritekstBegrunnelse = builder.fritekstBegrunnelse;
    }

    public String getFritekstBegrunnelse() {
        return fritekstBegrunnelse;
    }

    public static class Builder extends FattVedtakRequest.Builder<Builder> {
        private String fritekstBegrunnelse;

        @Override
        public Builder getThis() {
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
