package no.nav.melosys.service.dokument.brev;

import java.util.Optional;

import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;

public class BrevDataAvslagYrkesaktiv extends BrevData {
    public String arbeidsland;
    public AvklartVirksomhet hovedvirksomhet;
    public Yrkesaktivitetstyper yrkesaktivitet;
    public Optional<AnmodningsperiodeSvar> anmodningsperiodeSvar;
    public Vilkaarsresultat art16Vilkaar;
    public boolean erArt16UtenArt12;

    public BrevDataAvslagYrkesaktiv(BrevbestillingDto brevbestillingDto, String saksbehandler) {
        super(brevbestillingDto, saksbehandler);
    }
}
