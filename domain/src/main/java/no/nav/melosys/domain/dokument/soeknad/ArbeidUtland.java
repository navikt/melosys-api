package no.nav.melosys.domain.dokument.soeknad;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.organisasjon.adresse.Gateadresse;

/**
 * Opplysninger om arbeid i utlandet
 */
public class ArbeidUtland {
    public List<Land> arbeidsland = new ArrayList<>();
    public Periode arbeidsperiode;
    public BigDecimal arbeidsandelNorge;
    public BigDecimal arbeidsandelUtland;
    public Gateadresse arbeidsstedUtland; // TODO kan Gateadresse brukes? SED må sjekkes
    public String bostedsland;
    public Boolean erstatterTidligereUtsendt;
}
