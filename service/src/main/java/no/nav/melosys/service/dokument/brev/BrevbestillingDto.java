package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

public class BrevbestillingDto {

    public Aktoersroller mottaker;

    public String fritekst;

    public String begrunnelseKode;

    @Override
    public String toString() {
        return "BrevbestillingDto{" +
            "mottaker=" + mottaker +
            ", fritekst='" + fritekst + '\'' +
            ", begrunnelseKode='" + begrunnelseKode + '\'' +
            '}';
    }
}
