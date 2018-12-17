package no.nav.melosys.service.dokument.brev;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;

public class BrevDataA001 extends BrevData {

    public UtenlandskMyndighet utenlandskMyndighet;

    public PersonDokument personDokument;
    public Bostedsadresse bostedsadresse;
    public String utenlandskIdent;

    public List<Virksomhet> arbeidsgivendeVirkomsheter;
    public List<Virksomhet> selvstendigeVirksomheter;

    public List<Arbeidssted> arbeidssteder;

    // TODO: Kommer fra Joark. Kun relevant når purring er implementer
    public List<LocalDate> tidligereAnmodninger = new ArrayList<>();

    public List<Lovvalgsperiode> lovvalgsperioder;

    public List<Lovvalgsperiode> tidligereLovvalgsperioder = new ArrayList<>();

    public Vilkaarsresultat vilkårsresultat161;

    public Periode ansettelsesperiode;
}
