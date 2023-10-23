package no.nav.melosys.domain.dokument.soap.inntekt.tillegsinfo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.soap.inntekt.Periode;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Etterbetalingsperiode")
public class Etterbetalingsperiode extends TilleggsinformasjonDetaljer implements HarPeriode {

    public Periode etterbetalingsperiode;

    @Override
    @JsonIgnore
    public ErPeriode getPeriode() {
        return etterbetalingsperiode;
    }
}
