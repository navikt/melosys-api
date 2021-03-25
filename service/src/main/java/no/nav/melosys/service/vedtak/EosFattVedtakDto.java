package no.nav.melosys.service.vedtak;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public class EosFattVedtakDto extends FattVedtakDto {
    private final String fritekst;
    private final String fritekstSed;
    private final Set<String> mottakerinstitusjoner;
    private final String revurderBegrunnelse;

    private EosFattVedtakDto(Builder builder) {
        super(builder);
        this.fritekst = builder.fritekst;
        this.fritekstSed = builder.fritekstSed;
        this.mottakerinstitusjoner = builder.mottakerinstitusjoner;
        this.revurderBegrunnelse = builder.revurderBegrunnelse;
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

    public String getRevurderBegrunnelse() {
        return revurderBegrunnelse;
    }

    public static class Builder extends FattVedtakDto.Builder<Builder> {
        private String fritekst;
        private String fritekstSed;
        private Set<String> mottakerinstitusjoner;
        private String revurderBegrunnelse;

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

        public Builder medMottakerInstitusjoner(String... mottakerInstituisjon) {
            if (this.mottakerinstitusjoner == null) {
                this.mottakerinstitusjoner = new HashSet<>();
            }
            this.mottakerinstitusjoner.addAll(asList(mottakerInstituisjon));
            return this;
        }

        public Builder medRevurderBegrunnelse(String revurderBegrunnelse) {
            this.revurderBegrunnelse = revurderBegrunnelse;
            return this;
        }

        public EosFattVedtakDto build() {
            return new EosFattVedtakDto(this);
        }
    }
}
