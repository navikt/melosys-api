package no.nav.melosys.service.dokument.brev;

import java.util.Objects;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

public class BrevbestillingDto {

    public Aktoersroller mottaker;

    public String fritekst;

    public String begrunnelseKode;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrevbestillingDto that = (BrevbestillingDto) o;
        return mottaker == that.mottaker &&
            Objects.equals(fritekst, that.fritekst) &&
            Objects.equals(begrunnelseKode, that.begrunnelseKode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mottaker, fritekst, begrunnelseKode);
    }

    @Override
    public String toString() {
        return "BrevbestillingDto{" +
            "mottaker=" + mottaker +
            ", fritekst='" + fritekst + '\'' +
            ", begrunnelseKode='" + begrunnelseKode + '\'' +
            '}';
    }
}
