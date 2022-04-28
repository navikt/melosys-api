package no.nav.melosys.service.dokument.brev.mapper;

import java.util.*;
import java.util.stream.Stream;
import javax.transaction.Transactional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadTrygdeavtale;
import no.nav.melosys.domain.behandlingsgrunnlag.data.IdentType;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.FastMottakerMedOrgnr;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.InnvilgelseOgAttestStorbritannia;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Innvilgelse;
import no.nav.melosys.integrasjon.dokgen.dto.felles.Person;
import no.nav.melosys.integrasjon.dokgen.dto.storbritannia.attest.*;
import no.nav.melosys.integrasjon.dokgen.dto.storbritannia.innvilgelse.*;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterSystemService;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.behandlingsgrunnlag.data.IdentType.DNR;
import static no.nav.melosys.domain.behandlingsgrunnlag.data.IdentType.FNR;
import static org.springframework.util.ObjectUtils.isEmpty;

@Component
public class StorbritanniaMapper {
    private final AvklarteMedfolgendeFamilieService avklarteMedfølgendeFamilieService;
    private final AvklarteVirksomheterSystemService avklarteVirksomheterSystemService;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    public StorbritanniaMapper(AvklarteMedfolgendeFamilieService avklarteMedfølgendeFamilieService,
                               AvklarteVirksomheterSystemService avklarteVirksomheterSystemService,
                               LovvalgsperiodeService lovvalgsperiodeService) {
        this.avklarteMedfølgendeFamilieService = avklarteMedfølgendeFamilieService;
        this.avklarteVirksomheterSystemService = avklarteVirksomheterSystemService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    @Transactional
    public InnvilgelseOgAttestStorbritannia map(InnvilgelseBrevbestilling brevbestilling) {
        var innvilgelse = mapInnvilgelse(brevbestilling);
        return new InnvilgelseOgAttestStorbritannia.Builder(brevbestilling)
            .innvilgelse(innvilgelse)
            .attest(mapAttest(brevbestilling))
            .skalHaInfoOmRettigheter(skalHaInfoOmRettigheter(innvilgelse, brevbestilling))
            .nyVurderingBakgrunn(brevbestilling.getNyVurderingBakgrunn())
            .build();
    }

    private InnvilgelseStorbritannia mapInnvilgelse(InnvilgelseBrevbestilling brevbestilling) {
        if (skalIkkeHaInnvilgelse(brevbestilling)) return null;

        var behandling = brevbestilling.getBehandling();
        var behandlingsgrunnlag = behandling.getBehandlingsgrunnlag();
        var lovvalgsperiode = lovvalgsperiodeService.hentValidertLovvalgsperiode(behandling.getId());

        return new InnvilgelseStorbritannia.Builder()
            .innvilgelse(Innvilgelse.av(brevbestilling))
            .artikkel((Lovvalgbestemmelser_trygdeavtale_uk) lovvalgsperiode.getBestemmelse())
            .soknad(lagSøknad(behandlingsgrunnlag, lovvalgsperiode))
            .familie(lagFamile(behandling.getId()))
            .virksomhetArbeidsgiverSkalHaKopi(brevbestilling.isVirksomhetArbeidsgiverSkalHaKopi())
            .build();
    }

    private AttestStorbritannia mapAttest(DokgenBrevbestilling brevbestilling) {
        if (skalIkkeHaAttest(brevbestilling)) return null;

        var behandlingID = brevbestilling.getBehandlingId();
        var behandling = brevbestilling.getBehandling();
        var persondokument = brevbestilling.getPersondokument();
        var lovvalgsperioder = lovvalgsperiodeService.hentLovvalgsperioder(behandlingID);

        var adresseSjekker = new StorbritanniaAdresseSjekker(persondokument);

        return new AttestStorbritannia.Builder(brevbestilling)
            .medfolgendeFamiliemedlemmer(mapMedfolgendeFamiliemedlemmer(behandlingID))
            .arbeidsgiverNorge(lagArbeidsgiverNorge(behandling))
            .arbeidstaker(new Arbeidstaker(
                persondokument.getSammensattNavn(),
                persondokument.getFødselsdato(),
                persondokument.hentFolkeregisterident(),
                adresseSjekker.finnGyldigNorskAdresse()))
            .representant(lagRepresentant(behandling.getBehandlingsgrunnlag()))
            .utsendelse(lagUtsendelse(lovvalgsperioder, persondokument))
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
            .orElseThrow(() -> finnesIkkeIBehandlingsGrunnlagetException(uuid));

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
            .orElseThrow(() -> finnesIkkeIBehandlingsGrunnlagetException(uuid));
        var identType = medfølgendeBarn.utledIdentType();
        return new Barn.Builder()
            .navn(medfølgendeBarn.getNavn())
            .fnr(identType == FNR ? medfølgendeBarn.getFnr() : null)
            .dnr(identType == DNR ? medfølgendeBarn.getFnr() : null)
            .foedselsdato(medfølgendeBarn.datoFraFnr())
            .begrunnelse(begrunnelse)
            .build();
    }

    private Soknad lagSøknad(Behandlingsgrunnlag behandlingsgrunnlag, Lovvalgsperiode lovvalgsperiode) {
        var avklartVirksomhet = hentAvklartVirksomhet(behandlingsgrunnlag.getBehandling());

        return new Soknad(
            behandlingsgrunnlag.getMottaksdato(),
            lovvalgsperiode.getFom(),
            lovvalgsperiode.getTom(),
            avklartVirksomhet.navn
        );
    }

    private Utsendelse lagUtsendelse(Collection<Lovvalgsperiode> lovvalgsperioder, Persondata persondata) {
        if (lovvalgsperioder.size() != 1) {
            throw new FunksjonellException("Det kan bare være en lovvalgsperiode for trygdeavtale. Fant "
                + lovvalgsperioder.size()
            );
        }
        var lovvalgsperiode = lovvalgsperioder.iterator().next();

        var bestemmelse = lovvalgsperiode.getBestemmelse();

        var adresseSjekker = new StorbritanniaAdresseSjekker(persondata);

        return new Utsendelse.Builder()
            .artikkel((Lovvalgbestemmelser_trygdeavtale_uk) bestemmelse)
            .oppholdsadresseUK(adresseSjekker.finnGyldigStorbritanniaAdresse(lovvalgsperiode))
            .startdato(lovvalgsperiode.getFom())
            .sluttdato(lovvalgsperiode.getTom())
            .build();
    }


    private AvklartVirksomhet hentAvklartVirksomhet(Behandling behandling) {
        var avklarteVirksomheter = avklarteVirksomheterSystemService.hentNorskeArbeidsgivere(behandling);
        if (avklarteVirksomheter.size() != 1) {
            throw new FunksjonellException("Fant " + avklarteVirksomheter.size() + " avklarte virksomheter for behandling: " + behandling + ". Må være 1 for trygdeavtale");
        }
        return avklarteVirksomheter.get(0);
    }

    private ArbeidsgiverNorge lagArbeidsgiverNorge(Behandling behandling) {
        var norskArbeidsgiver = hentAvklartVirksomhet(behandling);
        return new ArbeidsgiverNorge(norskArbeidsgiver.navn, norskArbeidsgiver.adresse.toList());
    }

    private RepresentantStorbritannia lagRepresentant(Behandlingsgrunnlag behandlingsgrunnlag) {
        var soeknadTrygdeavtale = (SoeknadTrygdeavtale) behandlingsgrunnlag.getBehandlingsgrunnlagdata();
        var representantIUtlandet = soeknadTrygdeavtale.getRepresentantIUtlandet();
        if (representantIUtlandet == null) {
            throw new FunksjonellException(Kontroll_begrunnelser.ATTEST_MANGLER_ARBEIDSSTED.getBeskrivelse());
        }

        return new RepresentantStorbritannia(
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
            .orElseThrow(() -> finnesIkkeIBehandlingsGrunnlagetException(uuid));
        var identType = medfølgendeFamilie.utledIdentType();

        return new Person(
            medfølgendeFamilie.getNavn(),
            medfølgendeFamilie.datoFraFnr(),
            identType == FNR ? medfølgendeFamilie.getFnr() : null,
            identType == DNR ? medfølgendeFamilie.getFnr() : null);
    }

    private FunksjonellException finnesIkkeIBehandlingsGrunnlagetException(String uuid) {
        return new FunksjonellException("Avklart medfølgende familie " + uuid + " finnes ikke i behandlingsgrunnlaget");
    }

    private boolean erSkatteetaten(OrganisasjonDokument org) {
        // Skatteetaten skal ikke ha attest
        return org != null && FastMottakerMedOrgnr.SKATT.getOrgnr().equals(org.getOrgnummer());
    }

    private boolean skalIkkeHaInnvilgelse(InnvilgelseBrevbestilling brevbestilling) {
        // Utenlandkse trygdemyndigheter skal ikke ha innvilgelse
        return brevbestilling.getUtenlandskMyndighet() != null;
    }

    private boolean skalIkkeHaAttest(DokgenBrevbestilling brevbestilling) {
        // Skatteetaten skal ikke ha attest
        boolean erArtikkel8_2 = lovvalgsperiodeService.hentValidertLovvalgsperiode(brevbestilling.getBehandlingId()).getBestemmelse() == Lovvalgbestemmelser_trygdeavtale_uk.UK_ART8_2;
        return erSkatteetaten(brevbestilling.getOrg()) || erArtikkel8_2;
    }

    private boolean skalHaInfoOmRettigheter(InnvilgelseStorbritannia innvilgelse, DokgenBrevbestilling brevbestilling) {
        // Skal bare ha med vedlegget om innvilgelse er med og mottaker ikke er skatteetaten
        return !(isEmpty(innvilgelse) || erSkatteetaten(brevbestilling.getOrg()));
    }
}
