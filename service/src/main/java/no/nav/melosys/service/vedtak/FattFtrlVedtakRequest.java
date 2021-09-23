package no.nav.melosys.service.vedtak;

import java.util.List;

import no.nav.melosys.service.dokument.brev.KopiMottaker;

public class FattFtrlVedtakRequest extends FattVedtakRequest {
    private final String fritekstInnledning;
    private final String fritekstBegrunnelse;
    private final String fritekstEktefelle;
    private final String fritekstBarn;
    private final List<KopiMottaker> kopiMottakere;
    private final String bestillersId;

    private FattFtrlVedtakRequest(Builder builder) {
        super(builder);
        this.fritekstInnledning = builder.fritekstInnledning;
        this.fritekstBegrunnelse = builder.fritekstBegrunnelse;
        this.fritekstEktefelle = builder.fritekstEktefelle;
        this.fritekstBarn = builder.fritekstBarn;
        this.kopiMottakere = builder.kopiMottakere;
        this.bestillersId = builder.bestillersId;
    }

    public String getFritekstInnledning() {
        return fritekstInnledning;
    }

    public String getFritekstBegrunnelse() {
        return fritekstBegrunnelse;
    }

    public String getFritekstEktefelle() {
        return fritekstEktefelle;
    }

    public String getFritekstBarn() {
        return fritekstBarn;
    }

    public List<KopiMottaker> getKopiMottakere() {
        return kopiMottakere;
    }

    public String getBestillersId() {
        return bestillersId;
    }

    public static class Builder extends FattVedtakRequest.Builder<Builder> {
        private String fritekstInnledning;
        private String fritekstBegrunnelse;
        private String fritekstEktefelle;
        private String fritekstBarn;
        private List<KopiMottaker> kopiMottakere;
        private String bestillersId;

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

        public Builder medFritekstEktefelle(String fritekstEktefelle) {
            this.fritekstEktefelle = fritekstEktefelle;
            return this;
        }

        public Builder medFritekstBarn(String fritekstBarn) {
            this.fritekstBarn = fritekstBarn;
            return this;
        }

        public Builder medKopiMottakere(List<KopiMottaker> kopiMottakere) {
            this.kopiMottakere = kopiMottakere;
            return this;
        }

        public Builder medBestillersId(String bestillersId) {
            this.bestillersId = bestillersId;
            return this;
        }

        public FattFtrlVedtakRequest build() {
            return new FattFtrlVedtakRequest(this);
        }
    }
}
