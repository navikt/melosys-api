package no.nav.melosys.service.dokument.brev;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.YrkesgruppeType;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;

/*
 * Alle feltene er resultatverdier fra funksjoner i brevbygger, som brukes direkte i mapper.
 * Det er derfor brukt Optional på felter som kan være tomme
 */
public class BrevDataA001 extends BrevData {

    public UtenlandskMyndighet utenlandskMyndighet;

    public PersonDokument personDokument;
    public Bostedsadresse bostedsadresse;
    public Optional<String> utenlandskIdent;

    public List<Virksomhet> arbeidsgivendeVirkomsheter;
    public List<Virksomhet> selvstendigeVirksomheter;
    public YrkesgruppeType yrkesgruppe;

    public List<Arbeidssted> arbeidssteder;

    // TODO: Kommer fra Joark. Kun relevant når purring er implementer
    public List<LocalDate> tidligereAnmodninger = new ArrayList<>();

    public Collection<Lovvalgsperiode> lovvalgsperioder;

    public Collection<Lovvalgsperiode> tidligereLovvalgsperioder = new ArrayList<>();

    public Vilkaarsresultat vilkårsresultat161;

    public Optional<Periode> ansettelsesperiode;
}
