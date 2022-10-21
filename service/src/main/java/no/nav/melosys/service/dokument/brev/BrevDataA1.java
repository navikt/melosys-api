package no.nav.melosys.service.dokument.brev;

import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;

public class BrevDataA1 extends BrevData {
    public Yrkesgrupper yrkesgruppe;
    public Persondata person;
    public AvklartVirksomhet hovedvirksomhet;

    public List<Arbeidssted> arbeidssteder;
    public Collection<Land_iso2> arbeidsland;
    public boolean erUkjenteEllerAlleEosLand;

    public StrukturertAdresse bostedsadresse;
    public Collection<AvklartVirksomhet> bivirksomheter;
}
