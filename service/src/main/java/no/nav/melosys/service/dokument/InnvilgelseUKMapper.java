package no.nav.melosys.service.dokument;

import javax.transaction.Transactional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia.*;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

@Component
public class InnvilgelseUKMapper {

    private final AvklarteMedfolgendeFamilieService avklarteMedfølgendeFamilieService;
    private final DokgenMapperDatahenter dokgenMapperDatahenter;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final PersondataFasade persondataFasade;

    public InnvilgelseUKMapper(AvklarteMedfolgendeFamilieService avklarteMedfølgendeFamilieService,
                               DokgenMapperDatahenter dokgenMapperDatahenter,
                               LovvalgsperiodeService lovvalgsperiodeService, PersondataFasade persondataFasade) {
        this.avklarteMedfølgendeFamilieService = avklarteMedfølgendeFamilieService;
        this.dokgenMapperDatahenter = dokgenMapperDatahenter;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.persondataFasade = persondataFasade;
    }

    @Transactional
    public InnvilgelseUK map(InnvilgelseBrevbestilling brevbestilling) {
        Behandling behandling = brevbestilling.getBehandling();
        Behandlingsgrunnlag behandlingsgrunnlag = behandling.getBehandlingsgrunnlag();
        BehandlingsgrunnlagData behandlingsgrunnlagdata = behandlingsgrunnlag.getBehandlingsgrunnlagdata();
        var lovvalgsperioder = lovvalgsperiodeService.hentLovvalgsperioder(behandling.getId());

        boolean virksomhetArbeidsgiverSkalHaKopi = false;
        return new InnvilgelseUK.Builder(brevbestilling)
            .artikkel(getLovvalgbestemmelse(lovvalgsperioder))
            .soknad(lagSøknad(behandlingsgrunnlagdata.periode, behandlingsgrunnlag.getMottaksdato()))
            .familie(lagFamile(behandling.getId()))
            .virksomhetArbeidsgiverSkalHaKopi(virksomhetArbeidsgiverSkalHaKopi)
            .build();
    }

    private Familie lagFamile(long behandlingID) {
        return new Familie.Builder()
            .barn(finnBarn(behandlingID))
            .ektefelle(finnEktefelle(behandlingID))
            .build();
    }

    private Ektefelle finnEktefelle(long behandlingID) {
        var avklartMedfølgendeEktefelle = avklarteMedfølgendeFamilieService.hentAvklartMedfølgendeEktefelle(behandlingID);
        Map<String, MedfolgendeFamilie> medfolgendeEktefelleMap = avklarteMedfølgendeFamilieService.hentMedfølgendEktefelle(behandlingID);
        Set<OmfattetFamilie> ektefelleOmfattetAvNorskTrygd = avklartMedfølgendeEktefelle.getFamilieOmfattetAvNorskTrygd();
        if (!ektefelleOmfattetAvNorskTrygd.isEmpty()) {
            var ektefelleOmfattet = ektefelleOmfattetAvNorskTrygd.iterator().next();
            return tilEktefelle(medfolgendeEktefelleMap, ektefelleOmfattet.getUuid(), null);
        }
        Set<IkkeOmfattetFamilie> ektefelleIkkeOmfattetAvNorskTrygd = avklartMedfølgendeEktefelle.getFamilieIkkeOmfattetAvNorskTrygd();
        if (ektefelleIkkeOmfattetAvNorskTrygd.isEmpty()) {
            return null;
        }

        IkkeOmfattetFamilie ikkeOmfattetEktefelle = ektefelleIkkeOmfattetAvNorskTrygd.iterator().next();
        return tilEktefelle(medfolgendeEktefelleMap, ikkeOmfattetEktefelle.getUuid(), ikkeOmfattetEktefelle.getBegrunnelse());
    }

    private Ektefelle tilEktefelle(Map<String, MedfolgendeFamilie> medfølgendeFamilieMap, String uuid, String begrunnelse) {
        var medfølgendeFamilie = Optional.of(medfølgendeFamilieMap.get(uuid))
            .orElseThrow(() -> new FunksjonellException("Avklart medfølgende familie " + uuid + " finnes ikke i behandlingsgrunnlaget"));

        return new Ektefelle.Builder()
            .fnr(medfølgendeFamilie.getFnr())
            .navn(getSammensattNavn(medfølgendeFamilie))
            .begrunnelse(begrunnelse)
            .foedselsdato(getFødselDato(medfølgendeFamilie.getFnr()))
            .build();
    }

    private String getSammensattNavn(MedfolgendeFamilie medfølgendeFamilie) {
        if (medfølgendeFamilie.getFnr() == null) {
            return medfølgendeFamilie.getNavn();
        }
        return dokgenMapperDatahenter.hentSammensattNavn(medfølgendeFamilie.getFnr());
    }

    private List<Barn> finnBarn(long behandlingID) {
        var avklarteMedfølgendeBarn = avklarteMedfølgendeFamilieService.hentAvklarteMedfølgendeBarn(behandlingID);
        var barnOmfattetAvNorskTrygd = avklarteMedfølgendeBarn.barnOmfattetAvNorskTrygd;
        Set<no.nav.melosys.domain.person.familie.IkkeOmfattetBarn> barnIkkeOmfattetAvNorskTrygd = avklarteMedfølgendeBarn.barnIkkeOmfattetAvNorskTrygd;
        return Stream.concat(barnOmfattetAvNorskTrygd.stream()
                .map(this::tilBarn),
            barnIkkeOmfattetAvNorskTrygd.stream()
                .map(this::tilBarn)
        ).toList();
    }

    private Barn tilBarn(OmfattetFamilie omfattetFamilie) {
        return new Barn.Builder()
            .navn(omfattetFamilie.getSammensattNavn())
            .fnr(omfattetFamilie.getIdent())
            .foedselsdato(getFødselDato(omfattetFamilie.getIdent()))
            .build();
    }

    private Barn tilBarn(no.nav.melosys.domain.person.familie.IkkeOmfattetBarn ikkeOmfattetBarn) {
        return new Barn.Builder()
            .navn(ikkeOmfattetBarn.sammensattNavn)
            .fnr(ikkeOmfattetBarn.ident)
            .begrunnelse(ikkeOmfattetBarn.begrunnelse)
            .foedselsdato(getFødselDato(ikkeOmfattetBarn.ident))
            .build();
    }

    private LocalDate getFødselDato(String fnr) {
        var persondata = persondataFasade.hentPerson(fnr);
        return persondata.getFødselsdato();
    }

    private Lovvalgbestemmelser_trygdeavtale_uk getLovvalgbestemmelse(Collection<Lovvalgsperiode> lovvalgsperioder) {
        if (lovvalgsperioder.size() != 1) {
            throw new FunksjonellException("Det kan bare være en lovvalgsperiode for trygdeavtale. Fant "
                + lovvalgsperioder.size()
            );
        }
        var lovvalgsperiode = lovvalgsperioder.iterator().next();
        return (Lovvalgbestemmelser_trygdeavtale_uk) lovvalgsperiode.getBestemmelse();
    }

    private Soknad lagSøknad(Periode periode, LocalDate mottaksdato) {
        return new Soknad(
            // Er det riktig å bruke mottaksdato?
            // Brukes slik Vi viser til søknaden din om medlemskap i folketrygden som vi fikk [soknadsdato].
            mottaksdato,
            periode.getFom(),
            periode.getTom(),
            "virksomhets navn" // TODO
        );
    }
}
