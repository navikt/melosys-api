package no.nav.melosys.service.dokument.brev;

import java.time.LocalDate;
import java.util.*;

import no.nav.dok.melosysbrev._000115.BostedsadresseTypeKode;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;

/*
 * Alle feltene er resultatverdier fra funksjoner i brevbygger, som brukes direkte i mapper.
 * Det er derfor brukt Optional på felter som kan være tomme
 */
public class BrevDataA001 extends BrevData {
    public UtenlandskMyndighet utenlandskMyndighet;

    public Persondata persondata;
    public StrukturertAdresse bostedsadresse;
    public BostedsadresseTypeKode bostedsadresseTypeKode;
    public Optional<String> utenlandskIdent;

    public List<AvklartVirksomhet> arbeidsgivendeVirksomheter;
    public List<AvklartVirksomhet> selvstendigeVirksomheter;

    public List<Arbeidssted> arbeidssteder;

    // Kommer fra Joark. Kun relevant når purring er implementer
    public List<LocalDate> tidligereAnmodninger = new ArrayList<>();

    public Collection<Anmodningsperiode> anmodningsperioder;

    public Collection<Lovvalgsperiode> tidligereLovvalgsperioder = new ArrayList<>();

    public Optional<Periode> ansettelsesperiode;

    public Set<VilkaarBegrunnelse> anmodningUtenArt12Begrunnelser;
    public Set<VilkaarBegrunnelse> anmodningBegrunnelser;
    public String anmodningFritekstBegrunnelse;
    public String ytterligereInformasjon;
}
