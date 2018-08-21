package no.nav.melosys.domain.dokument.person;

import java.util.List;
import javax.xml.bind.annotation.*;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class PersonhistorikkDokument extends SaksopplysningDokument {

    public List<StatsborgerskapPeriode> statsborgerskapListe;

    public List<BostedsadressePeriode> bostedsadressePeriodeListe;

    public List<PostadressePeriode> postadressePeriodeListe;

    public List<MidlertidigPostadresse> midlertidigAdressePeriodeListe;

}
