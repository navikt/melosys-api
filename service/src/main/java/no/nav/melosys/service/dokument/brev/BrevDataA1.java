package no.nav.melosys.service.dokument.brev;

import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Yrkesgrupper;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;

public class BrevDataA1 extends BrevData {
    public List<Virksomhet> utenlandskeVirksomheter;
    public List<Virksomhet> norskeVirksomheter;
    public Set<String> selvstendigeForetak;
    public Yrkesgrupper yrkesgruppe;
    public PersonDokument person;
    public Virksomhet hovedvirksomhet;

    public List<Arbeidssted> arbeidssteder;

    public Bostedsadresse bostedsadresse;
}
