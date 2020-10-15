package no.nav.melosys.domain.behandlingsgrunnlag.soeknad;

import java.time.LocalDate;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.dokument.jaxb.LocalDateXmlAdapter;

public class ArbeidsgiversBekreftelse {

    public Boolean arbeidsgiverBekrefterUtsendelse;
    public Boolean arbeidstakerAnsattUnderUtsendelsen;
    public Boolean erstatterArbeidstakerenUtsendte;
    public Boolean arbeidstakerTidligereUtsendt24Mnd;
    public Boolean arbeidsgiverBetalerArbeidsgiveravgift;
    public Boolean trygdeavgiftTrukketGjennomSkatt;
    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    public LocalDate trygdeavgiftTrukketGjennomSkattDato;
}
