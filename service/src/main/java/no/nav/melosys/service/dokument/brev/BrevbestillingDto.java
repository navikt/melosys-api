package no.nav.melosys.service.dokument.brev;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.RolleType;

public class BrevbestillingDto {

    public RolleType mottaker;

    public String fritekst;

    @JsonProperty("begrunnelse")
    public String begrunnelseKode;
}
