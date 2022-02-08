package no.nav.melosys.service.vedtak;

import java.util.Set;

public class FattEosVedtakRequest extends FattVedtakRequest {
    private final String fritekst;
    private final String fritekstSed;
    private final Set<String> mottakerinstitusjoner;
    private final String nyVurderingBakgrunn;

    private FattEosVedtakRequest(Builder builder) {
        super(builder);
        this.fritekst = builder.fritekst;
        this.fritekstSed = builder.fritekstSed;
        this.mottakerinstitusjoner = builder.mottakerinstitusjoner;
        this.nyVurderingBakgrunn = builder.nyVurderingBakgrunn;
    }

    public String getFritekst() {
        return fritekst;
    }

    public String getFritekstSed() {
        return fritekstSed;
    }

    public Set<String> getMottakerinstitusjoner() {
        return mottakerinstitusjoner;
    }

    public String getNyVurderingBakgrunn() {
        return nyVurderingBakgrunn;
    }

    public static class Builder extends FattVedtakRequest.Builder<Builder> {
        private String fritekst;
        private String fritekstSed;
        private Set<String> mottakerinstitusjoner;
        private String nyVurderingBakgrunn;

        @Override
        public Builder getThis() {
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

        public Builder medMottakerInstitusjoner(Set<String> mottakerInstitusjoner) {
            this.mottakerinstitusjoner = mottakerInstitusjoner;
            return this;
        }

        public Builder medNyVurderingBakgrunn(String nyVurderingBakgrunn) {
            this.nyVurderingBakgrunn = nyVurderingBakgrunn;
            return this;
        }

        public FattEosVedtakRequest build() {
            return new FattEosVedtakRequest(this);
        }
    }
}
