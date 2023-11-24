package no.nav.melosys.domain.dokument.person;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.person.adresse.BostedsadressePeriode;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.adresse.PostadressePeriode;



public class PersonhistorikkDokument implements SaksopplysningDokument {

    public List<StatsborgerskapPeriode> statsborgerskapListe = new ArrayList<>();

    public List<BostedsadressePeriode> bostedsadressePeriodeListe = new ArrayList<>();

    public List<PostadressePeriode> postadressePeriodeListe = new ArrayList<>();

    public List<MidlertidigPostadresse> midlertidigAdressePeriodeListe = new ArrayList<>();

}
