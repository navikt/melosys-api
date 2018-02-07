package no.nav.melosys.domain.dokument.soeknad;

import java.time.LocalDate;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.dokument.jaxb.LocalDateXmlAdapter;

public class ArbeidsgiversBekreftelse {

    public boolean arbeidsgiverBekrefterUtsendelse;
    public boolean arbeidstakerAnsattUnderUtsendelsen;
    public boolean erstatterArbeidstakerenUtsendte;
    public boolean arbeidstakerTidligereUtsendt24Mnd;
    public boolean arbeidsgiverBetalerArbeidsgiveravgift;
    public boolean trygdeavgiftTrukketGjennomSkatt;
    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    public LocalDate trygdeavgiftTrukketGjennomSkattDato;
}
