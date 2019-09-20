package no.nav.melosys.domain.brev;

import java.util.Arrays;
import java.util.Collection;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

public final class Brevbestilling {
    private final Produserbaredokumenter dokumentType;
    private final String avsender;
    private final Collection<Mottaker> mottaker;
    private final Behandling behandling;
    private final String begrunnelseKode;
    private final String fritekst;

    private Brevbestilling(Produserbaredokumenter dokumentType,
                           String avsender,
                           Collection<Mottaker> mottaker,
                           Behandling behandling,
                           String begrunnelseKode,
                           String fritekst) {
        this.dokumentType = dokumentType;
        this.avsender = avsender;
        this.mottaker = mottaker;
        this.behandling = behandling;
        this.begrunnelseKode = begrunnelseKode;
        this.fritekst = fritekst;
    }

    public static class Builder {
        private Produserbaredokumenter dokumentType;
        private String avsender;
        private Collection<Mottaker> mottakere;
        private Behandling behandling;
        private String begrunnelseKode;
        private String fritekst;

        public Builder medDokumentType(Produserbaredokumenter dokumentType) {
            this.dokumentType = dokumentType;
            return this;
        }

        public Builder medAvsender(String avsender) {
            this.avsender = avsender;
            return this;
        }

        public Builder medMottakere(Mottaker... mottaker) {
            this.mottakere = Arrays.asList(mottaker);
            return this;
        }

        public Builder medBehandling(Behandling behandling) {
            this.behandling = behandling;
            return this;
        }

        public Builder medBegrunnelseKode(String begrunnelseKode) {
            this.begrunnelseKode = begrunnelseKode;
            return this;
        }

        public Builder medFritekst(String fritekst) {
            this.fritekst = fritekst;
            return this;
        }

        public Brevbestilling build() {
            return new Brevbestilling(dokumentType, avsender, mottakere, behandling, begrunnelseKode, fritekst);
        }
    }

    public Produserbaredokumenter getDokumentType() {
        return dokumentType;
    }

    public String getAvsender() {
        return avsender;
    }

    public Collection<Mottaker> getMottakere() {
        return mottaker;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public String getBegrunnelseKode() {
        return begrunnelseKode;
    }

    public String getFritekst() {
        return fritekst;
    }
}
