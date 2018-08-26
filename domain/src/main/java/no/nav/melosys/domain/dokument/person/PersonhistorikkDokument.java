package no.nav.melosys.domain.dokument.person;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class PersonhistorikkDokument extends SaksopplysningDokument {

    public List<StatsborgerskapPeriode> statsborgerskapListe = new ArrayList<>();

    public List<BostedsadressePeriode> bostedsadressePeriodeListe = new ArrayList<>();

    public List<PostadressePeriode> postadressePeriodeListe = new ArrayList<>();

    public List<MidlertidigPostadresse> midlertidigAdressePeriodeListe = new ArrayList<>();

}
