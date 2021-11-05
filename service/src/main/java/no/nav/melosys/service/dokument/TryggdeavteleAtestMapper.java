package no.nav.melosys.service.dokument;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.transaction.Transactional;

import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.brev.storbritannia.AttestStorbritanniaBrevbestilling;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeBarn;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.atteststorbritannia.*;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Person;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.stereotype.Component;

@Component
public class TryggdeavteleAtestMapper {

    private final AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;
    private final DokgenMapperDatahenter dokgenMapperDatahenter;
    private final PersondataFasade persondataFasade;

    public TryggdeavteleAtestMapper(AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService,
                                    DokgenMapperDatahenter dokgenMapperDatahenter,
                                    PersondataFasade registerOppslagService) {
        this.avklarteMedfolgendeFamilieService = avklarteMedfolgendeFamilieService;
        this.dokgenMapperDatahenter = dokgenMapperDatahenter;
        this.persondataFasade = registerOppslagService;
    }

    @Transactional
    public AttestStorbritannia map(AttestStorbritanniaBrevbestilling brevbestilling) {
        long behandlingId = brevbestilling.getBehandlingId();
        var behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(behandlingId);

        return new AttestStorbritannia.Builder(brevbestilling)
            .medfolgendeFamiliemedlemmer(new MedfolgendeFamiliemedlemmer(
                mapEktefelle(behandlingId),
                mapBarn(behandlingId)
            ))
            .arbeidsgiverNorge(new ArbeidsgiverNorge(
                "Virksomhetsnavn", List.of("Nordmannsveg 200", "Norge")))
            .arbeidstaker(
                new Arbeidstaker(
                    "Nordmann, Ola",
                    createDate("1994-04-30"),
                    "01010119901",
                    List.of("Nordmannsveg 200", "Norge")))
            .representantUK(new RepresentantUK(
                "Mrs. London",
                List.of("UK Street 1337"))
            )
            .utsendelse(new Utsendelse(
                Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1,
                List.of("UK Street 1337"),
                createDate("2020-12-24"),
                createDate("2022-11-01")
            ))
            .build();
    }

    private Instant createDate(String date) {
        return LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private List<Person> mapBarn(long behandlingID) {
        AvklarteMedfolgendeBarn avklarteMedfolgendeBarn = avklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(behandlingID);
        Set<OmfattetFamilie> barnOmfattetAvNorskTrygd = avklarteMedfolgendeBarn.barnOmfattetAvNorskTrygd;
        if(barnOmfattetAvNorskTrygd.isEmpty()) {
            return List.of();
        }
        Map<String, MedfolgendeFamilie> medfølgendeBarn = avklarteMedfolgendeFamilieService.hentMedfølgendeBarn(behandlingID);
        return barnOmfattetAvNorskTrygd.stream().map(omfattetFamilie -> tilPerson(medfølgendeBarn, omfattetFamilie.getUuid())).collect(Collectors.toList());
    }

    private Person mapEktefelle(long behandlingID) {
        AvklarteMedfolgendeFamilie avklarteMedfolgendeFamilie = avklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(behandlingID);
        Set<OmfattetFamilie> familieOmfattetAvNorskTrygd = avklarteMedfolgendeFamilie.getFamilieOmfattetAvNorskTrygd();
        if(familieOmfattetAvNorskTrygd.isEmpty()) {
            return null;
        }
        OmfattetFamilie omfattetFamilie = familieOmfattetAvNorskTrygd.iterator().next();
        Map<String, MedfolgendeFamilie> medfolgendeEktefelle = avklarteMedfolgendeFamilieService.hentMedfølgendEktefelle(behandlingID);
        String uuid = omfattetFamilie.getUuid();
        return tilPerson(medfolgendeEktefelle, uuid);
    }

    private Person tilPerson(Map<String, MedfolgendeFamilie> medfolgendeFamilieMap, String uuid) {
        MedfolgendeFamilie medfolgendeFamilie = Optional.of(medfolgendeFamilieMap.get(uuid))
            .orElseThrow(() -> new FunksjonellException("Avklart medfølgende familie " + uuid + " finnes ikke i behandlingsgrunnlaget"));
        String sammensattNavn = medfolgendeFamilie.fnr != null ? dokgenMapperDatahenter.hentSammensattNavn(medfolgendeFamilie.fnr) : medfolgendeFamilie.navn;
        Instant instant = getFødselDato(medfolgendeFamilie.fnr);
        return new Person(sammensattNavn, instant, medfolgendeFamilie.fnr, null);
    }

    private Instant getFødselDato(String fnr) {
        Persondata persondata = persondataFasade.hentPerson(fnr);
        return persondata.getFødselsdato().atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}
