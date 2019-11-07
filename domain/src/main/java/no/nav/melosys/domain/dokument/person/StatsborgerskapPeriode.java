package no.nav.melosys.domain.dokument.person;

import java.time.LocalDateTime;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.jaxb.LocalDateTimeXmlAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
public class StatsborgerskapPeriode implements HarPeriode {
    public Periode periode;

    // Attributtet endretAv er på formen "KODE_SYSTEM_KILDE" eller "ENDRET_AV, KODE_SYSTEM_KILDE"
    public String endretAv;

    @XmlJavaTypeAdapter(LocalDateTimeXmlAdapter.class)
    public LocalDateTime endringstidspunkt;

    public Land statsborgerskap;

    @Override
    @JsonIgnore
    public ErPeriode getPeriode() {
        return periode;
    }
}
