package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.service.dokument.brev.KopiMottaker;

import java.util.List;
import java.util.Set;

public class FattVedtakRequest {
    private final Behandlingsresultattyper behandlingsresultatTypeKode;
    private final Vedtakstyper vedtakstype;
    private final String fritekst;
    private final String fritekstSed;
    private final Set<String> mottakerinstitusjoner;
    private final String innledningFritekst;
    private final String begrunnelseFritekst;
    private final String ektefelleFritekst;
    private final String barnFritekst;
    private final List<KopiMottaker> kopiMottakere;
    private final String bestillersId;
    private final String nyVurderingBakgrunn;

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

    protected FattVedtakRequest(Builder builder) {
        this.behandlingsresultatTypeKode = builder.behandlingsresultatTypeKode;
        this.vedtakstype = builder.vedtakstype;
        this.fritekst = builder.fritekst;
        this.fritekstSed = builder.fritekstSed;
        this.mottakerinstitusjoner = builder.mottakerinstitusjoner;
        this.innledningFritekst = builder.innledningFritekst;
        this.begrunnelseFritekst = builder.begrunnelseFritekst;
        this.ektefelleFritekst = builder.ektefelleFritekst;
        this.barnFritekst = builder.barnFritekst;
        this.kopiMottakere = builder.kopiMottakere;
        this.bestillersId = builder.bestillersId;
        this.nyVurderingBakgrunn = builder.nyVurderingBakgrunn;
    }

    public Behandlingsresultattyper getBehandlingsresultatTypeKode() {
        return behandlingsresultatTypeKode;
    }

    public Vedtakstyper getVedtakstype() {
        return vedtakstype;
    }

    public static class Builder {
        private Behandlingsresultattyper behandlingsresultatTypeKode;
        private Vedtakstyper vedtakstype;
        private String fritekst;
        private String fritekstSed;
        private Set<String> mottakerinstitusjoner;
        private String nyVurderingBakgrunn;
        private String innledningFritekst;
        private String begrunnelseFritekst;
        private String ektefelleFritekst;
        private String barnFritekst;
        private List<KopiMottaker> kopiMottakere;
        private String bestillersId;

        public Builder medBehandlingsresultat(Behandlingsresultattyper behandlingsresultatTypeKode) {
            this.behandlingsresultatTypeKode = behandlingsresultatTypeKode;
            return this;
        }

        public Builder medVedtakstype(Vedtakstyper vedtakstype) {
            this.vedtakstype = vedtakstype;
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

        public String getFritekst() {
            return fritekst;
        }

        public void setFritekst(final String fritekst) {
            this.fritekst = fritekst;
        }

        public String getFritekstSed() {
            return fritekstSed;
        }

        public void setFritekstSed(String fritekstSed) {
            this.fritekstSed = fritekstSed;
        }

        public Set<String> getMottakerinstitusjoner() {
            return mottakerinstitusjoner;
        }

        public void setMottakerinstitusjoner(Set<String> mottakerinstitusjoner) {
            this.mottakerinstitusjoner = mottakerinstitusjoner;
        }

        public String getNyVurderingBakgrunn() {
            return nyVurderingBakgrunn;
        }

        public void setNyVurderingBakgrunn(String nyVurderingBakgrunn) {
            this.nyVurderingBakgrunn = nyVurderingBakgrunn;
        }

        public Builder medMottakerInstitusjoner(Set<String> mottakerInstitusjoner) {
            this.mottakerinstitusjoner = mottakerInstitusjoner;
            return this;
        }

        public Builder medNyVurderingBakgrunn(String nyVurderingBakgrunn) {
            this.nyVurderingBakgrunn = nyVurderingBakgrunn;
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

        public FattVedtakRequest build() {
            return new FattVedtakRequest(this);
        }
    }
}
