package no.nav.melosys.domain.dokument.inntekt.tillegsinfo;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.inntekt.Periode;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BarnepensjonOgUnderholdsbidrag")
public class BarnepensjonOgUnderholdsbidrag extends TilleggsinformasjonDetaljer implements HarPeriode {

    public String forsoergersFoedselnummer;

    public Periode tidsrom;

    @Override
    public ErPeriode getPeriode() {
        return tidsrom;
    }
}
