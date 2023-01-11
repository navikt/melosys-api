package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.transaction.Transactional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.FastMottakerMedOrgnr;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.SoeknadTrygdeavtale;
import no.nav.melosys.domain.mottatteopplysninger.data.IdentType;
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.InnvilgelseOgAttestTrygdeavtale;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Innvilgelse;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Person;
import no.nav.melosys.integrasjon.dokgen.dto.trygdeavtale.attest.*;
import no.nav.melosys.integrasjon.dokgen.dto.trygdeavtale.innvilgelse.*;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.UtledMottaksdato;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk.UK_ART8_2;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_usa.USA_ART5_1;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_usa.USA_ART5_9;
import static no.nav.melosys.domain.mottatteopplysninger.data.IdentType.DNR;
import static no.nav.melosys.domain.mottatteopplysninger.data.IdentType.FNR;
import static org.springframework.util.ObjectUtils.isEmpty;

@Component
public class TrygdeavtaleMapper {
    private final AvklarteMedfolgendeFamilieService avklarteMedfølgendeFamilieService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final UtledMottaksdato utledMottaksdato;

    public TrygdeavtaleMapper(AvklarteMedfolgendeFamilieService avklarteMedfølgendeFamilieService,
                              AvklarteVirksomheterService avklarteVirksomheterService,
                              LovvalgsperiodeService lovvalgsperiodeService,
                              UtledMottaksdato utledMottaksdato) {
        this.avklarteMedfølgendeFamilieService = avklarteMedfølgendeFamilieService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.utledMottaksdato = utledMottaksdato;
    }

    @Transactional
    public InnvilgelseOgAttestTrygdeavtale map(InnvilgelseBrevbestilling brevbestilling, Land_iso2 soknadsland) {
        var innvilgelse = mapInnvilgelse(brevbestilling, soknadsland);
        return new InnvilgelseOgAttestTrygdeavtale.Builder(brevbestilling)
            .innvilgelse(innvilgelse)
            .attest(mapAttest(brevbestilling, soknadsland))
            .skalHaInfoOmRettigheter(skalHaInfoOmRettigheter(innvilgelse, brevbestilling))
            .nyVurderingBakgrunn(brevbestilling.getNyVurderingBakgrunn())
            .build();
    }

    private InnvilgelseTrygdeavtale mapInnvilgelse(InnvilgelseBrevbestilling brevbestilling, Land_iso2 soknadsland) {
        if (skalIkkeHaInnvilgelse(brevbestilling)) return null;

        var behandling = brevbestilling.getBehandling();
        var lovvalgsperiode = lovvalgsperiodeService.hentLovvalgsperiode(behandling.getId());

        return new InnvilgelseTrygdeavtale.Builder()
            .innvilgelse(Innvilgelse.av(brevbestilling))
            .artikkel(lovvalgsperiode.getBestemmelse())
            .tilleggsbestemmelse(lovvalgsperiode.getTilleggsbestemmelse())
            .soknad(lagSøknad(behandling, lovvalgsperiode, soknadsland))
            .familie(lagFamile(behandling.getId()))
            .virksomhetArbeidsgiverSkalHaKopi(brevbestilling.isVirksomhetArbeidsgiverSkalHaKopi())
            .build();
    }

    private AttestTrygdeavtale mapAttest(DokgenBrevbestilling brevbestilling, Land_iso2 soknadsland) {
        if (skalIkkeHaAttest(brevbestilling)) return null;

        var behandlingID = brevbestilling.getBehandlingId();
        var behandling = brevbestilling.getBehandling();
        var persondokument = brevbestilling.getPersondokument();
        var lovvalgsperioder = lovvalgsperiodeService.hentLovvalgsperioder(behandlingID);

        var adresseSjekker = new TrygdeavtaleAdresseSjekker(persondokument);

        return new AttestTrygdeavtale.Builder(brevbestilling)
            .medfolgendeFamiliemedlemmer(mapMedfolgendeFamiliemedlemmer(behandlingID))
            .arbeidsgiverNorge(lagArbeidsgiverNorge(behandling))
            .arbeidstaker(new Arbeidstaker(
                persondokument.getSammensattNavn(),
                persondokument.getFødselsdato(),
                persondokument.hentFolkeregisterident(),
                adresseSjekker.finnGyldigNorskAdresse(soknadsland)))
            .representant(lagRepresentant(behandling.getMottatteOpplysninger()))
            .utsendelse(lagUtsendelse(lovvalgsperioder, persondokument, soknadsland))
            .build();
    }

    private Familie lagFamile(long behandlingID) {
        var ektefelle = finnEktefelle(behandlingID);
        var barn = finnBarn(behandlingID);

        if (isEmpty(ektefelle) && isEmpty(barn)) {
            return null;
        }

        return new Familie.Builder()
            .ektefelle(ektefelle)
            .barn(barn)
            .build();
    }

    private Ektefelle finnEktefelle(long behandlingID) {
        var avklartMedfølgendeEktefelle = avklarteMedfølgendeFamilieService.hentAvklartMedfølgendeEktefelle(behandlingID);
        var medfolgendeEktefelleMap = avklarteMedfølgendeFamilieService.hentMedfølgendEktefelle(behandlingID);
        var ektefelleOmfattetAvNorskTrygd = avklartMedfølgendeEktefelle.getFamilieOmfattetAvNorskTrygd();
        if (!ektefelleOmfattetAvNorskTrygd.isEmpty()) {
            var ektefelleOmfattet = ektefelleOmfattetAvNorskTrygd.iterator().next();
            return tilEktefelle(medfolgendeEktefelleMap, ektefelleOmfattet.getUuid(), null);
        }
        var ektefelleIkkeOmfattetAvNorskTrygd = avklartMedfølgendeEktefelle.getFamilieIkkeOmfattetAvNorskTrygd();
        if (ektefelleIkkeOmfattetAvNorskTrygd.isEmpty()) {
            return null;
        }

        IkkeOmfattetFamilie ikkeOmfattetEktefelle = ektefelleIkkeOmfattetAvNorskTrygd.iterator().next();
        return tilEktefelle(medfolgendeEktefelleMap, ikkeOmfattetEktefelle.getUuid(), ikkeOmfattetEktefelle.getBegrunnelse());
    }

    private Ektefelle tilEktefelle(Map<String, MedfolgendeFamilie> medfølgendeFamilieMap, String uuid, String begrunnelse) {
        var medfølgendeFamilie = Optional.of(medfølgendeFamilieMap.get(uuid))
            .orElseThrow(() -> finnesIkkeIMottatteOpplysningerException(uuid));

        IdentType identType = medfølgendeFamilie.utledIdentType();
        return new Ektefelle.Builder()
            .fnr(identType == FNR ? medfølgendeFamilie.getFnr() : null)
            .dnr(identType == DNR ? medfølgendeFamilie.getFnr() : null)
            .navn(medfølgendeFamilie.getNavn())
            .begrunnelse(begrunnelse)
            .fødselsdato(medfølgendeFamilie.datoFraFnr())
            .build();
    }

    private List<Barn> finnBarn(long behandlingID) {
        var avklarteMedfølgendeBarn = avklarteMedfølgendeFamilieService.hentAvklarteMedfølgendeBarn(behandlingID);
        var barnOmfattetAvNorskTrygd = avklarteMedfølgendeBarn.getFamilieOmfattetAvNorskTrygd();
        var barnIkkeOmfattetAvNorskTrygd = avklarteMedfølgendeBarn.getFamilieIkkeOmfattetAvNorskTrygd();

        var medfølgendeBarn = avklarteMedfølgendeFamilieService.hentMedfølgendeBarn(behandlingID);

        return Stream.concat(
            barnOmfattetAvNorskTrygd.stream()
                .map(omfattetFamilie -> tilBarn(medfølgendeBarn, omfattetFamilie.getUuid(), null)),
            barnIkkeOmfattetAvNorskTrygd.stream()
                .map(ikkeOmfattetBarn -> tilBarn(medfølgendeBarn, ikkeOmfattetBarn.getUuid(), ikkeOmfattetBarn.getBegrunnelse()))
        ).toList();
    }

    private Barn tilBarn(Map<String, MedfolgendeFamilie> medfølgendeBarnMap, String uuid, String begrunnelse) {
        var medfølgendeBarn = Optional.of(medfølgendeBarnMap.get(uuid))
            .orElseThrow(() -> finnesIkkeIMottatteOpplysningerException(uuid));
        var identType = medfølgendeBarn.utledIdentType();
        return new Barn.Builder()
            .navn(medfølgendeBarn.getNavn())
            .fnr(identType == FNR ? medfølgendeBarn.getFnr() : null)
            .dnr(identType == DNR ? medfølgendeBarn.getFnr() : null)
            .foedselsdato(medfølgendeBarn.datoFraFnr())
            .begrunnelse(begrunnelse)
            .build();
    }

    private Soknad lagSøknad(Behandling behandling, Lovvalgsperiode lovvalgsperiode, Land_iso2 soknadsland) {
        var avklartVirksomhet = hentAvklartVirksomhet(behandling);

        return new Soknad(
            utledMottaksdato.getMottaksdato(behandling),
            lovvalgsperiode.getFom(),
            lovvalgsperiode.getTom(),
            avklartVirksomhet.navn,
            soknadsland.getBeskrivelse()
        );
    }

    private Utsendelse lagUtsendelse(Collection<Lovvalgsperiode> lovvalgsperioder, Persondata persondata, Land_iso2 soknadsland) {
        if (lovvalgsperioder.size() != 1) {
            throw new FunksjonellException("Det kan bare være en lovvalgsperiode for trygdeavtale. Fant "
                + lovvalgsperioder.size()
            );
        }
        var lovvalgsperiode = lovvalgsperioder.iterator().next();

        var bestemmelse = lovvalgsperiode.getBestemmelse();

        var adresseSjekker = new TrygdeavtaleAdresseSjekker(persondata);

        return new Utsendelse.Builder()
            .artikkel(bestemmelse)
            .oppholdsadresse(adresseSjekker.finnGyldigTrygdeavtaleAdresse(lovvalgsperiode, soknadsland))
            .startdato(lovvalgsperiode.getFom())
            .sluttdato(lovvalgsperiode.getTom())
            .build();
    }


    private AvklartVirksomhet hentAvklartVirksomhet(Behandling behandling) {
        var avklarteVirksomheter = avklarteVirksomheterService.hentNorskeArbeidsgivere(behandling);
        if (avklarteVirksomheter.size() != 1) {
            throw new FunksjonellException("Fant " + avklarteVirksomheter.size() + " avklarte virksomheter for behandling: " + behandling + ". Må være 1 for trygdeavtale");
        }
        return avklarteVirksomheter.get(0);
    }

    private ArbeidsgiverNorge lagArbeidsgiverNorge(Behandling behandling) {
        var norskArbeidsgiver = hentAvklartVirksomhet(behandling);
        return new ArbeidsgiverNorge(norskArbeidsgiver.navn, norskArbeidsgiver.adresse.toList());
    }

    private RepresentantTrygdeavtale lagRepresentant(MottatteOpplysninger mottatteOpplysninger) {
        var soeknadTrygdeavtale = (SoeknadTrygdeavtale) mottatteOpplysninger.getMottatteOpplysningerData();
        var representantIUtlandet = soeknadTrygdeavtale.getRepresentantIUtlandet();
        if (representantIUtlandet == null) {
            throw new FunksjonellException(Kontroll_begrunnelser.ATTEST_MANGLER_ARBEIDSSTED.getBeskrivelse());
        }

        return new RepresentantTrygdeavtale(
            representantIUtlandet.representantNavn,
            representantIUtlandet.adresselinjer
        );
    }

    private MedfolgendeFamiliemedlemmer mapMedfolgendeFamiliemedlemmer(long behandlingID) {
        var ektefelle = mapEktefelle(behandlingID);
        var barn = mapBarn(behandlingID);

        if (ektefelle == null && barn.isEmpty()) return null;

        return new MedfolgendeFamiliemedlemmer(ektefelle, barn);
    }

    private List<Person> mapBarn(long behandlingID) {
        var barnOmfattetAvNorskTrygd =
            avklarteMedfølgendeFamilieService.hentAvklarteMedfølgendeBarn(behandlingID).getFamilieOmfattetAvNorskTrygd();
        var medfølgendeBarn = avklarteMedfølgendeFamilieService.hentMedfølgendeBarn(behandlingID);

        return barnOmfattetAvNorskTrygd.stream().map(omfattetFamilie -> mapFamilieTilPerson(medfølgendeBarn, omfattetFamilie.getUuid())).toList();
    }

    private Person mapEktefelle(long behandlingID) {
        var ektefelleOmfattetAvNorskTrygd =
            avklarteMedfølgendeFamilieService.hentAvklartMedfølgendeEktefelle(behandlingID).getFamilieOmfattetAvNorskTrygd();
        if (ektefelleOmfattetAvNorskTrygd.isEmpty()) {
            return null;
        }

        var omfattetFamilie = ektefelleOmfattetAvNorskTrygd.iterator().next();
        return mapFamilieTilPerson(avklarteMedfølgendeFamilieService.hentMedfølgendEktefelle(behandlingID), omfattetFamilie.getUuid());
    }

    private Person mapFamilieTilPerson(Map<String, MedfolgendeFamilie> medfølgendeFamilieMap, String uuid) {
        var medfølgendeFamilie = Optional.of(medfølgendeFamilieMap.get(uuid))
            .orElseThrow(() -> finnesIkkeIMottatteOpplysningerException(uuid));
        var identType = medfølgendeFamilie.utledIdentType();

        return new Person(
            medfølgendeFamilie.getNavn(),
            medfølgendeFamilie.datoFraFnr(),
            identType == FNR ? medfølgendeFamilie.getFnr() : null,
            identType == DNR ? medfølgendeFamilie.getFnr() : null);
    }

    private FunksjonellException finnesIkkeIMottatteOpplysningerException(String uuid) {
        return new FunksjonellException("Avklart medfølgende familie " + uuid + " finnes ikke i mottatteOpplysningeret");
    }

    private boolean erSkatteetaten(OrganisasjonDokument org) {
        // Skatteetaten skal ikke ha attest
        return org != null && FastMottakerMedOrgnr.SKATTEETATEN.getOrgnr().equals(org.getOrgnummer());
    }

    private boolean skalIkkeHaInnvilgelse(InnvilgelseBrevbestilling brevbestilling) {
        // Utenlandkse trygdemyndigheter skal ikke ha innvilgelse
        return brevbestilling.getUtenlandskMyndighet() != null;
    }

    private boolean skalIkkeHaAttest(DokgenBrevbestilling brevbestilling) {
        LovvalgBestemmelse bestemmelse = lovvalgsperiodeService.hentLovvalgsperiode(brevbestilling.getBehandlingId()).getBestemmelse();
        boolean ukBestemmelseSkalIkkeHaAttest = bestemmelse == UK_ART8_2;
        boolean usaBestemmelseSkalIkkeHaAttest = bestemmelse == USA_ART5_1 || bestemmelse == USA_ART5_9;
        return erSkatteetaten(brevbestilling.getOrg()) || ukBestemmelseSkalIkkeHaAttest || usaBestemmelseSkalIkkeHaAttest;
    }

    private boolean skalHaInfoOmRettigheter(InnvilgelseTrygdeavtale innvilgelse, DokgenBrevbestilling brevbestilling) {
        return !(isEmpty(innvilgelse) || erSkatteetaten(brevbestilling.getOrg()));
    }
}
