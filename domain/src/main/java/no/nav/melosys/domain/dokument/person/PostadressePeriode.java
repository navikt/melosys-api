package no.nav.melosys.domain.dokument.person;

import java.time.LocalDateTime;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.jaxb.LocalDateTimeXmlAdapter;

public class PostadressePeriode implements HarPeriode {

    public Periode periode;

    @XmlJavaTypeAdapter(LocalDateTimeXmlAdapter.class)
    public LocalDateTime endringstidspunkt;

    public UstrukturertAdresse postadresse;

    @Override
    @JsonIgnore
    public ErPeriode getPeriode() {
        return periode;
    }
}
