package no.nav.melosys.domain.dokument.soeknad;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Opplysninger om arbeid i Norge
 */
public class ArbeidNorge {
    public Boolean arbeidsforholdOpprettholdIHelePerioden;
    public String arbeidsforholdVikarNavn;
    public String vikarOrgnr;
    public String flyendePersonellHjemmebase;
    public String kontaktNavn;
    public String kontaktEpost;
    public String fullmektigFirma;
    public String fullmektigGateadresse;
    public String fullmektigPostnr;
    public String fullmektigPoststed;
    public String fullmektigRegion;
    public List<String> fullmektigLand;
}
