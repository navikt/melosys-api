package no.nav.melosys.domain.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Produserbaredokumenter;

public final class Brevbestilling {
    private final Produserbaredokumenter dokumentType;
    private final String avsender;
    private final Mottaker mottaker;
    private final Behandling behandling;
    private final String begrunnelseKode;

    private Brevbestilling(Produserbaredokumenter dokumentType, String avsender, Mottaker mottaker, Behandling behandling, String begrunnelseKode) {
        this.dokumentType = dokumentType;
        this.avsender = avsender;
        this.mottaker = mottaker;
        this.behandling = behandling;
        this.begrunnelseKode = begrunnelseKode;
    }

    public static class Builder {
        private Produserbaredokumenter dokumentType;
        private String avsender;
        private Mottaker mottaker;
        private Behandling behandling;
        private String begrunnelseKode;

        public Builder medDokumentType(Produserbaredokumenter dokumentType) {
            this.dokumentType = dokumentType;
            return this;
        }

        public Builder medAvsender(String avsender) {
            this.avsender = avsender;
            return this;
        }

        public Builder medMottaker(Mottaker mottaker) {
            this.mottaker = mottaker;
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

        public Brevbestilling build() {
            return new Brevbestilling(dokumentType, avsender, mottaker, behandling, begrunnelseKode);
        }
    }

    public Produserbaredokumenter getDokumentType() {
        return dokumentType;
    }

    public String getAvsender() {
        return avsender;
    }

    public Mottaker getMottaker() {
        return mottaker;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public String getBegrunnelseKode() {
        return begrunnelseKode;
    }
}
