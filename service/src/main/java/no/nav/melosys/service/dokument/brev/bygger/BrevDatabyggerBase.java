package no.nav.melosys.service.dokument.brev.bygger;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;
import no.nav.melosys.service.kodeverk.KodeverkService;

public abstract class BrevDatabyggerBase {
    final protected KodeverkService kodeverkService;
    final protected LovvalgsperiodeService lovvalgsperiodeService;
    final protected AvklartefaktaService avklartefaktaService;

    protected PersonDokument person;
    protected SoeknadDokument søknad;

    protected Behandling behandling;

    protected Set<String> avklarteOrganisasjoner;

    protected BrevDatabyggerBase(KodeverkService kodeverkService,
        LovvalgsperiodeService lovvalgsperiodeService,
        AvklartefaktaService avklartefaktaService) {
        this.kodeverkService = kodeverkService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.avklartefaktaService = avklartefaktaService;
    }

    protected abstract List<Virksomhet> hentAlleNorskeAvklarteVirksomheter() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException;

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

    protected Collection<Lovvalgsperiode> hentLovvalgsperioder() throws TekniskException {
        Collection<Lovvalgsperiode> lovvalgsperioder = lovvalgsperiodeService.hentLovvalgsperioder(behandling.getId());
        if (lovvalgsperioder.isEmpty()) {
            throw new TekniskException("Trenger minst en lovvalgsperiode");
        }

        Lovvalgsperiode valgtLovvalgsperiode = lovvalgsperioder.iterator().next();
        boolean lovvalgsperiodeIkkeGyldig = lovvalgsperioder.stream()
            .anyMatch(periode -> !validerPeriode(periode, valgtLovvalgsperiode));
        if (lovvalgsperiodeIkkeGyldig) {
            throw new TekniskException("A001 kan ha flere  lovvalgsperioder, men ikke med ulike Land eller unntak");
        }
        return lovvalgsperioder;
    }

    private boolean validerPeriode(Lovvalgsperiode p1, Lovvalgsperiode p2) {
        return p1.getLovvalgsland() == p2.getLovvalgsland() &&
            p1.getUnntakFraBestemmelse() != null &&
            p1.getUnntakFraBestemmelse() == p2.getUnntakFraBestemmelse() &&
            p1.getUnntakFraLovvalgsland() != null &&
            p1.getUnntakFraLovvalgsland() == p2.getUnntakFraLovvalgsland();
    }
}
