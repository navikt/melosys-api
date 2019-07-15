package no.nav.melosys.service.dokument.brev;

import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Yrkesgrupper;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;

public class BrevDataA1 extends BrevData {
    public List<AvklartVirksomhet> utenlandskeVirksomheter;
    public List<AvklartVirksomhet> norskeVirksomheter;
    public Set<String> selvstendigeForetak;
    public Yrkesgrupper yrkesgruppe;
    public PersonDokument person;
    public AvklartVirksomhet hovedvirksomhet;

    public List<Arbeidssted> arbeidssteder;

    public StrukturertAdresse bostedsadresse;
}
