package no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.kodeverk.Innretningstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Fartsomrader;

public class MaritimtArbeid {
    public String enhetNavn;
    public Fartsomrader fartsomradeKode;
    public String flaggLandkode;
    public String innretningLandkode;
    public Innretningstyper innretningstype;
    @JsonProperty("territorialfarvann")
    public String territorialfarvannLandkode;
}
