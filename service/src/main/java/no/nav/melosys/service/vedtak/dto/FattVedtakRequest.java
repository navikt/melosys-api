package no.nav.melosys.service.vedtak.dto;

import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;

public class FattVedtakRequest {
    private final Behandlingsresultattyper behandlingsresultatTypeKode;
    private final Vedtakstyper vedtakstype;

    protected FattVedtakRequest(Builder<?> builder) {
        this.behandlingsresultatTypeKode = builder.behandlingsresultatTypeKode;
        this.vedtakstype = builder.vedtakstype;
    }

    public Behandlingsresultattyper getBehandlingsresultatTypeKode() {
        return behandlingsresultatTypeKode;
    }

    public Vedtakstyper getVedtakstype() {
        return vedtakstype;
    }

    public abstract static class Builder<T extends Builder<T>> {
        private Behandlingsresultattyper behandlingsresultatTypeKode;
        private Vedtakstyper vedtakstype;

        public abstract T getThis();

        public T medBehandlingsresultat(Behandlingsresultattyper behandlingsresultatTypeKode) {
            this.behandlingsresultatTypeKode = behandlingsresultatTypeKode;
            return this.getThis();
        }

        public T medVedtakstype(Vedtakstyper vedtakstype) {
            this.vedtakstype = vedtakstype;
            return this.getThis();
        }

        public FattVedtakRequest build() {
            return new FattVedtakRequest(this);
        }
    }
}
