package no.nav.melosys.service.dokument.brev.bygger;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;
import no.nav.melosys.service.kodeverk.KodeverkService;

public abstract class BrevDatabyggerBase {
    final protected KodeverkService kodeverkService;

    protected PersonDokument person;
    protected SoeknadDokument søknad;

    protected Set<String> avklarteOrganisasjoner;

    protected BrevDatabyggerBase(KodeverkService kodeverkService1) {
        this.kodeverkService = kodeverkService1;
    }

    protected Bostedsadresse hentBostedsadresse() {
        Bostedsadresse adresse = person.bostedsadresse;
        adresse.setPoststed(kodeverkService.dekod(FellesKodeverk.POSTNUMMER, adresse.getPostnr(), LocalDate.now()));
        return adresse;
    }

    protected Set<String> hentAvklarteSelvstendigeForetakOrgnumre() {
        Set<String> organisasjonsnumre = søknad.selvstendigArbeid.hentAlleOrganisasjonsnumre()
                .collect(Collectors.toSet());

        organisasjonsnumre.retainAll(avklarteOrganisasjoner);
        return organisasjonsnumre;
    }

    protected List<Arbeidssted> hentArbeidssteder() throws TekniskException {
        List<Virksomhet> utenlandskeVirksomheter = hentUtenlandskeVirksomheter();
        if (utenlandskeVirksomheter.size() != 1) {
            throw new TekniskException("Krever utsendelse til én og kun én virksomhet i utlandet");
        }

        // I Lev1 er det kun én utenlandsk arbeidsgiver.
        // Det er derfor ok å bruke dette navnet på fysisk arbeidssted
        Virksomhet utenlandskVirksomhet = utenlandskeVirksomheter.get(0);

        List<Arbeidssted> arbeidssteder = hentFysiskearbeidsstederMedNavn(utenlandskVirksomhet.navn);
        arbeidssteder.addAll(hentIkkeFysiskeArbeidssteder());

        if (!arbeidssteder.isEmpty()) {
            return arbeidssteder;
        }

        // Brevet krever alltid minst et arbeidssted - selv når det ikke er oppgitt i søknad
        return Arrays.asList(utledArbeidsstedFraVirksomhet(utenlandskVirksomhet));
    }

    protected List<Virksomhet> hentUtenlandskeVirksomheter() {
        // Lev1 har alltid kun et utenlandsk foretak.
        // Det er derfor ikke nødvendig med filtrering av avklarte foretak
        return søknad.foretakUtland.stream()
                .map(Virksomhet::new)
                .collect(Collectors.toList());
    }

    private List<Arbeidssted> hentFysiskearbeidsstederMedNavn(String navnPåTilhørendeVirksomhet) {
        return søknad.arbeidUtland.stream()
                .map(au -> new Arbeidssted(navnPåTilhørendeVirksomhet, au.adresse))
                .collect(Collectors.toList());
    }

    private List<Arbeidssted> hentIkkeFysiskeArbeidssteder() {
        List<Arbeidssted> ikkeFysiskArbeidssteder = new ArrayList<>();
        //TODO: hente ut maritimt arbeid
        return ikkeFysiskArbeidssteder;
    }

    private Arbeidssted utledArbeidsstedFraVirksomhet(Virksomhet virksomhet) {
        return new Arbeidssted(virksomhet.navn, virksomhet.adresse.landKode);
    }
}
