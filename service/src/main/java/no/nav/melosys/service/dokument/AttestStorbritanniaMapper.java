package no.nav.melosys.service.dokument;

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
public class AttestStorbritanniaMapper {

    private final AvklarteMedfolgendeFamilieService avklarteMedfølgendeFamilieService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final DokgenMapperDatahenter dokgenMapperDatahenter;
    private final PersondataFasade persondataFasade;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    public AttestStorbritanniaMapper(AvklarteMedfolgendeFamilieService avklarteMedfølgendeFamilieService,
                                     AvklarteVirksomheterService avklarteVirksomheterService,
                                     DokgenMapperDatahenter dokgenMapperDatahenter,
                                     PersondataFasade registerOppslagService,
                                     LovvalgsperiodeService lovvalgsperiodeService) {
        this.avklarteMedfølgendeFamilieService = avklarteMedfølgendeFamilieService;
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
            .arbeidsgiverNorge(lagArbeidsgiverNorge(behandling))
            .arbeidstaker(new Arbeidstaker(
                persondokument.getSammensattNavn(),
                persondokument.getFødselsdato(),
                persondokument.hentFolkeregisterident(),
                persondokument.hentGjeldendePostadresse().adresselinjer()))
            .representantUK(new RepresentantUK(
                "Mrs. London", // TODO: Det blir fylt inn via sidemeny. Hent data når det er tilgjenglig.
                List.of())
            )
            .utsendelse(lagUtsendelse(lovvalgsperioder, persondokument))
            .build();
    }

    private Utsendelse lagUtsendelse(Collection<Lovvalgsperiode> lovvalgsperioder, Persondata persondata) {
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
            .startdato(lovvalgsperiode.getFom())
            .sluttdato(lovvalgsperiode.getTom())
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

    private ArbeidsgiverNorge lagArbeidsgiverNorge(Behandling behandling) {
        var avklarteVirksomheter = avklarteVirksomheterService.hentNorskeArbeidsgivere(behandling);
        if (avklarteVirksomheter.size() != 1) {
            throw new FunksjonellException("Fant " + avklarteVirksomheter.size() + " avklarte virksomheter for behandling: " + behandling + ". Må være 1 for trygdeavtale");
        }
        AvklartVirksomhet norskArbeidsgiver = avklarteVirksomheter.get(0);
        return new ArbeidsgiverNorge(norskArbeidsgiver.navn, norskArbeidsgiver.adresse.toList());
    }

    private List<Person> mapBarn(long behandlingID) {
        var avklarteMedfølgendeBarn = avklarteMedfølgendeFamilieService.hentAvklarteMedfølgendeBarn(behandlingID);
        var barnOmfattetAvNorskTrygd = avklarteMedfølgendeBarn.getFamilieOmfattetAvNorskTrygd();
        if (barnOmfattetAvNorskTrygd.isEmpty()) {
            return List.of();
        }
        var medfølgendeBarn = avklarteMedfølgendeFamilieService.hentMedfølgendeBarn(behandlingID);
        return barnOmfattetAvNorskTrygd.stream().map(omfattetFamilie -> tilPerson(medfølgendeBarn, omfattetFamilie.getUuid())).toList();
    }

    private Person mapEktefelle(long behandlingID) {
        var avklartMedfølgendeEktefelle = avklarteMedfølgendeFamilieService.hentAvklartMedfølgendeEktefelle(behandlingID);
        var ektefelleOmfattetAvNorskTrygd = avklartMedfølgendeEktefelle.getFamilieOmfattetAvNorskTrygd();
        if (ektefelleOmfattetAvNorskTrygd.isEmpty()) {
            return null;
        }
        var omfattetFamilie = ektefelleOmfattetAvNorskTrygd.iterator().next();
        var medfølgendeEktefelle = avklarteMedfølgendeFamilieService.hentMedfølgendEktefelle(behandlingID);
        return tilPerson(medfølgendeEktefelle, omfattetFamilie.getUuid());
    }

    private Person tilPerson(Map<String, MedfolgendeFamilie> medfølgendeFamilieMap, String uuid) {
        var medfølgendeFamilie = Optional.of(medfølgendeFamilieMap.get(uuid))
            .orElseThrow(() -> new FunksjonellException("Avklart medfølgende familie " + uuid + " finnes ikke i behandlingsgrunnlaget"));
        var fnr = medfølgendeFamilie.getFnr();

        var sammensattNavn = fnr != null ? dokgenMapperDatahenter.hentSammensattNavn(fnr) : medfølgendeFamilie.getNavn();
        var fødselsdato = persondataFasade.hentPerson(fnr).getFødselsdato().atStartOfDay(ZoneId.systemDefault()).toInstant();
        return new Person(sammensattNavn, fødselsdato, fnr, null);
    }
}
