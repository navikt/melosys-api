package no.nav.melosys.service.vedtak;

import java.util.List;

import no.nav.melosys.service.dokument.brev.KopiMottaker;

public class FattFtrlVedtakRequest extends FattVedtakRequest {
    private final String innledningFritekst;
    private final String begrunnelseFritekst;
    private final String ektefelleFritekst;
    private final String barnFritekst;
    private final List<KopiMottaker> kopiMottakere;
    private final String bestillersId;
    private final String nyVurderingBakgrunn;

    private FattFtrlVedtakRequest(Builder builder) {
        super(builder);
        this.innledningFritekst = builder.innledningFritekst;
        this.begrunnelseFritekst = builder.begrunnelseFritekst;
        this.ektefelleFritekst = builder.ektefelleFritekst;
        this.barnFritekst = builder.barnFritekst;
        this.kopiMottakere = builder.kopiMottakere;
        this.bestillersId = builder.bestillersId;
        this.nyVurderingBakgrunn = builder.nyVurderingBakgrunn;
    }

    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public String getEktefelleFritekst() {
        return ektefelleFritekst;
    }

    public String getBarnFritekst() {
        return barnFritekst;
    }

    public List<KopiMottaker> getKopiMottakere() {
        return kopiMottakere;
    }

    public String getBestillersId() {
        return bestillersId;
    }

    public String getNyVurderingBakgrunn() {
        return nyVurderingBakgrunn;
    }

    public static class Builder extends FattVedtakRequest.Builder<Builder> {
        private String innledningFritekst;
        private String begrunnelseFritekst;
        private String ektefelleFritekst;
        private String barnFritekst;
        private List<KopiMottaker> kopiMottakere;
        private String bestillersId;
        private String nyVurderingBakgrunn;


        @Override
        public Builder getThis() {
            return this;
        }

        public Builder medInnledningFritekst(String innledningFritekst) {
            this.innledningFritekst = innledningFritekst;
            return this;
        }

        public Builder medBegrunnelseFritekst(String begrunnelseFritekst) {
            this.begrunnelseFritekst = begrunnelseFritekst;
            return this;
        }

        public Builder medEktefelleFritekst(String ektefelleFritekst) {
            this.ektefelleFritekst = ektefelleFritekst;
            return this;
        }

        public Builder medBarnFritekst(String barnFritekst) {
            this.barnFritekst = barnFritekst;
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

        public Builder medNyVurderingBakgrunn(String nyVurderingBakgrunn) {
            this.nyVurderingBakgrunn = nyVurderingBakgrunn;
            return this;
        }

        public FattFtrlVedtakRequest build() {
            return new FattFtrlVedtakRequest(this);
        }
    }
}
