package no.nav.melosys.service.dokument;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;
import javax.transaction.Transactional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.adresse.PersonAdresse;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.atteststorbritannia.*;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Person;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.stereotype.Component;

@Component
public class TrygdeavtaleAttestMapper {

    private final AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final DokgenMapperDatahenter dokgenMapperDatahenter;
    private final PersondataFasade persondataFasade;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    public TrygdeavtaleAttestMapper(AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService,
                                    AvklarteVirksomheterService avklarteVirksomheterService,
                                    DokgenMapperDatahenter dokgenMapperDatahenter,
                                    PersondataFasade registerOppslagService,
                                    LovvalgsperiodeService lovvalgsperiodeService) {
        this.avklarteMedfolgendeFamilieService = avklarteMedfolgendeFamilieService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.dokgenMapperDatahenter = dokgenMapperDatahenter;
        this.persondataFasade = registerOppslagService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    @Transactional
    public AttestStorbritannia map(DokgenBrevbestilling brevbestilling) {
        var behandlingId = brevbestilling.getBehandlingId();
        var behandling = brevbestilling.getBehandling();
        var persondokument = brevbestilling.getPersondokument();
        var lovvalgsperioder = lovvalgsperiodeService.hentLovvalgsperioder(behandlingId);

        return new AttestStorbritannia.Builder(brevbestilling)
            .medfolgendeFamiliemedlemmer(new MedfolgendeFamiliemedlemmer(
                mapEktefelle(behandlingId),
                mapBarn(behandlingId)
            ))
            .arbeidsgiverNorge(getArbeidsgiverNorge(behandling))
            .arbeidstaker(new Arbeidstaker(
                persondokument.getSammensattNavn(),
                toInstant(persondokument.getFødselsdato()),
                persondokument.hentFolkeregisterident(),
                persondokument.hentGjeldendePostadresse().adresselinjer()))
            .representantUK(new RepresentantUK(
                "Mrs. London", // TODO: Det blir fylt inn via sidemeny. Hent data når det er tilgjenglig.
                List.of())
            )
            .utsendelse(getUtsendelse(lovvalgsperioder, persondokument))
            .build();
    }

    private Utsendelse getUtsendelse(Collection<Lovvalgsperiode> lovvalgsperioder, Persondata persondata) {
        if (lovvalgsperioder.size() != 1) {
            throw new FunksjonellException("Det kan bare være en lovvalgsperiode for trygdeavtale. Fant "
                + lovvalgsperioder.size()
            );
        }
        var lovvalgsperiode = lovvalgsperioder.iterator().next();

        var bestemmelse = lovvalgsperiode.getBestemmelse();

        return new Utsendelse.Builder()
            .artikkel((Lovvalgbestemmelser_trygdeavtale_uk) bestemmelse)
            .oppholdsadresseUK(finnGyldigAdresse(persondata, lovvalgsperiode))
            .startdato(toInstant(lovvalgsperiode.getFom()))
            .sluttdato(toInstant(lovvalgsperiode.getTom()))
            .build();
    }

    static List<String> finnGyldigAdresse(Persondata persondata, Lovvalgsperiode lovvalgsperiode) {
        var optionalPersonAdresse = Stream.of(
                persondata.finnBostedsadresse(),
                persondata.finnOppholdsadresse(),
                persondata.finnKontaktadresse())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(personAdresse -> sjekkAdresseMotLand(personAdresse.strukturertAdresse()))
            .filter(personAdresse -> sjekkOmAdresseGyldighetErInnenforLovalgsperiode(personAdresse, lovvalgsperiode))
            .findFirst();
        return optionalPersonAdresse.isPresent() ? optionalPersonAdresse.get().strukturertAdresse().toList() : List.of();
    }

    private static boolean sjekkAdresseMotLand(StrukturertAdresse adresse) {
        return adresse.getLandkode().equals(Landkoder.GB.getKode());
    }

    static boolean sjekkOmAdresseGyldighetErInnenforLovalgsperiode(PersonAdresse personAdresse, Lovvalgsperiode lovvalgsperiode) {
        if (personAdresse.gyldigFraOgMed() == null) return false;
        if (personAdresse.gyldigTilOgMed() == null) return false;

        if (lovvalgsperiode.getTom().isBefore(personAdresse.gyldigFraOgMed())) return false;
        return !lovvalgsperiode.getFom().isAfter(personAdresse.gyldigTilOgMed());
    }

    private ArbeidsgiverNorge getArbeidsgiverNorge(Behandling behandling) {
        var avklartVirksomhets = avklarteVirksomheterService.hentNorskeArbeidsgivere(behandling);
        if (avklartVirksomhets.size() != 1) {
            throw new FunksjonellException("Fant " + avklartVirksomhets.size() + " avklarte virksomheter for behandling: " + behandling + ". Må være 1 for trygdeavtale");
        }
        AvklartVirksomhet norskeArbeidsgiver = avklartVirksomhets.get(0);
        return new ArbeidsgiverNorge(norskeArbeidsgiver.navn, norskeArbeidsgiver.adresse.toList());
    }

    private static Instant toInstant(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private List<Person> mapBarn(long behandlingID) {
        var avklarteMedfolgendeBarn = avklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(behandlingID);
        var barnOmfattetAvNorskTrygd = avklarteMedfolgendeBarn.barnOmfattetAvNorskTrygd;
        if (barnOmfattetAvNorskTrygd.isEmpty()) {
            return List.of();
        }
        var medfølgendeBarn = avklarteMedfolgendeFamilieService.hentMedfølgendeBarn(behandlingID);
        return barnOmfattetAvNorskTrygd.stream().map(omfattetFamilie -> tilPerson(medfølgendeBarn, omfattetFamilie.getUuid())).toList();
    }

    private Person mapEktefelle(long behandlingID) {
        var avklarteMedfolgendeFamilie = avklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(behandlingID);
        var familieOmfattetAvNorskTrygd = avklarteMedfolgendeFamilie.getFamilieOmfattetAvNorskTrygd();
        if (familieOmfattetAvNorskTrygd.isEmpty()) {
            return null;
        }
        var omfattetFamilie = familieOmfattetAvNorskTrygd.iterator().next();
        var medfolgendeEktefelle = avklarteMedfolgendeFamilieService.hentMedfølgendEktefelle(behandlingID);
        String uuid = omfattetFamilie.getUuid();
        return tilPerson(medfolgendeEktefelle, uuid);
    }

    private Person tilPerson(Map<String, MedfolgendeFamilie> medfolgendeFamilieMap, String uuid) {
        var medfolgendeFamilie = Optional.of(medfolgendeFamilieMap.get(uuid))
            .orElseThrow(() -> new FunksjonellException("Avklart medfølgende familie " + uuid + " finnes ikke i behandlingsgrunnlaget"));
        var sammensattNavn = medfolgendeFamilie.getFnr() != null ? dokgenMapperDatahenter.hentSammensattNavn(medfolgendeFamilie.getFnr()) : medfolgendeFamilie.getNavn();
        var instant = getFødselDato(medfolgendeFamilie.getFnr());
        return new Person(sammensattNavn, instant, medfolgendeFamilie.getFnr(), null);
    }

    private Instant getFødselDato(String fnr) {
        var persondata = persondataFasade.hentPerson(fnr);
        return toInstant(persondata.getFødselsdato());
    }
}
