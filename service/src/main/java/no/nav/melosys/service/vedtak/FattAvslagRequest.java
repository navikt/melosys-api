package no.nav.melosys.service.vedtak;


public class FattAvslagRequest extends FattVedtakRequest {
    private final String fritekst;

    private FattAvslagRequest(Builder builder) {
        super(builder);
        this.fritekst = builder.fritekst;
    }

    public String getFritekst() {
        return fritekst;
    }

    public static class Builder extends FattVedtakRequest.Builder<Builder> {
        private String fritekst;

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
    }
}
