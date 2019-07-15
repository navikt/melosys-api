package no.nav.melosys.service.dokument.brev;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;

/*
 * Alle feltene er resultatverdier fra funksjoner i brevbygger, som brukes direkte i mapper.
 * Det er derfor brukt Optional på felter som kan være tomme
 */
public class BrevDataA001 extends BrevData {

    public UtenlandskMyndighet utenlandskMyndighet;

    public PersonDokument personDokument;
    public StrukturertAdresse bostedsadresse;
    public Optional<String> utenlandskIdent;

    public List<AvklartVirksomhet> arbeidsgivendeVirkomsheter;
    public List<AvklartVirksomhet> selvstendigeVirksomheter;

    public List<Arbeidssted> arbeidssteder;

    // Kommer fra Joark. Kun relevant når purring er implementer
    public List<LocalDate> tidligereAnmodninger = new ArrayList<>();

    public Collection<Lovvalgsperiode> lovvalgsperioder;

    public Collection<Lovvalgsperiode> tidligereLovvalgsperioder = new ArrayList<>();

    public Vilkaarsresultat vilkårsresultat161;

    public Optional<Periode> ansettelsesperiode;
}
