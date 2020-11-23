package no.nav.melosys.domain.dokument.inntekt.tillegsinfo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.inntekt.Periode;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BarnepensjonOgUnderholdsbidrag")
public class BarnepensjonOgUnderholdsbidrag extends TilleggsinformasjonDetaljer implements HarPeriode {

    @XmlElement(name = "forsoergersFoedselnummer")
    public String forsørgersFødselnummer;

    public Periode tidsrom;

    @Override
    @JsonIgnore
    public ErPeriode getPeriode() {
        return tidsrom;
    }
}
