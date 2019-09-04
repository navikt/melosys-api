package no.nav.melosys.domain.dokument.utbetaling;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

@XmlAccessorType(XmlAccessType.FIELD)
public class Utbetaling {

    @XmlElementWrapper(name = "ytelser")
    @XmlElement(name = "ytelse")
    public List<Ytelse> ytelser = new ArrayList<>();
}
