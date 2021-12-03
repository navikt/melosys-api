package no.nav.melosys.service.dokument.brev;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.MaritimtArbeid;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.adresse.Gateadresse;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.MaritimtArbeidssted;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.ARBEIDSLAND;
import static no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser.OVER_18_AR;

public class BrevDataTestUtils {

    public static StrukturertAdresse lagStrukturertAdresse() {
        StrukturertAdresse addr = new StrukturertAdresse();
        addr.setGatenavn("Strukturert Gate");
        addr.setHusnummerEtasjeLeilighet("12B");
        addr.setPoststed("Poststed");
        addr.setPostnummer("4321");
        addr.setLandkode(Landkoder.BG.getKode());
        return addr;
    }

    public static Bostedsadresse lagBostedsadresse() {
        Bostedsadresse badr = new Bostedsadresse();
        badr.setLand(new Land(Land.BELGIA));
        badr.setPoststed("Sted");
        badr.setPostnr("1234");
        Gateadresse gadr = lagGateAdresse();
        badr.setGateadresse(gadr);
        return badr;
    }

    private static Gateadresse lagGateAdresse() {
        Gateadresse gadr = new Gateadresse();
        gadr.setGatenavn("Gate");
        gadr.setGatenummer(1);
        gadr.setHusbokstav("A");
        gadr.setHusnummer(123);
        return gadr;
    }

    public static AvklartVirksomhet lagNorskVirksomhet() {
        return new AvklartVirksomhet("Bedrift AS", "123456789", lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID);
    }

    public static ForetakUtland lagForetakUtland(Boolean selvstendig) {
        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.navn = "Company International Ltd.";
        foretakUtland.orgnr = "12345678910";
        foretakUtland.uuid = "49m8gf-9dk4j0";
        foretakUtland.adresse = lagStrukturertAdresse();
        foretakUtland.adresse.setLandkode("NO");
        foretakUtland.selvstendigNæringsvirksomhet = selvstendig;
        return foretakUtland;
    }

    public static Saksopplysning lagPersonsaksopplysning(PersonDokument person) {
        return lagSaksopplysning(SaksopplysningType.PERSOPL, person);
    }

    private static Saksopplysning lagSaksopplysning(SaksopplysningType type, SaksopplysningDokument dokument) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(type);
        saksopplysning.setDokument(dokument);
        return saksopplysning;
    }

    public static Arbeidssted lagMaritimtArbeidssted() {
        return lagMaritimtArbeidssted(Maritimtyper.SKIP);
    }

    public static Arbeidssted lagMaritimtArbeidssted(Maritimtyper maritimtype) {
        MaritimtArbeid maritimtArbeid = lagMaritimtArbeid();
        AvklartMaritimtArbeid avklartMaritimtArbeid = lagAvklartMaritimtArbeid();
        return new MaritimtArbeidssted(maritimtArbeid, avklartMaritimtArbeid);
    }

    public static AvklartMaritimtArbeid lagAvklartMaritimtArbeid() {
        AvklartMaritimtArbeid avklartMaritimtArbeid = new AvklartMaritimtArbeid("MaritimtArbeid",
            List.of(new Avklartefakta(null, null, ARBEIDSLAND, null, "GB")));
        return avklartMaritimtArbeid;
    }

    public static MaritimtArbeid lagMaritimtArbeid() {
        MaritimtArbeid maritimtArbeid = new MaritimtArbeid();
        maritimtArbeid.enhetNavn = "Dunfjæder";
        maritimtArbeid.flaggLandkode = Landkoder.GB.getKode();
        return maritimtArbeid;
    }

    public static AnmodningsperiodeSvar lagAnmodningsperiodeSvarAvslag() {
        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        anmodningsperiodeSvar.setBegrunnelseFritekst("No tiendo");
        anmodningsperiodeSvar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.AVSLAG);
        return anmodningsperiodeSvar;
    }

    public static AnmodningsperiodeSvar lagAnmodningsperiodeSvarInnvilgelse() {
        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        anmodningsperiodeSvar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.DELVIS_INNVILGELSE);
        anmodningsperiodeSvar.setBegrunnelseFritekst("OK");
        return anmodningsperiodeSvar;
    }

    public static Vilkaarsresultat lagVilkaarsresultat(Vilkaar vilkaar, boolean oppfylt, Kodeverk... vilkårbegrunnelser) {
        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setOppfylt(oppfylt);
        vilkaarsresultat.setVilkaar(vilkaar);
        vilkaarsresultat.setBegrunnelser(new HashSet<>());
        for (Kodeverk begrunnelseKode : vilkårbegrunnelser) {
            VilkaarBegrunnelse begrunnelse = new VilkaarBegrunnelse();
            begrunnelse.setKode(begrunnelseKode.getKode());
            vilkaarsresultat.getBegrunnelser().add(begrunnelse);
        }
        return vilkaarsresultat;
    }

    public static AvklarteMedfolgendeFamilie lagAvklarteMedfølgendeBarn() {
        OmfattetFamilie omfattetBarn = new OmfattetFamilie("fnrOmfattet");
        omfattetBarn.setSammensattNavn("Omfattet Barn");
        omfattetBarn.setIdent("123321123");
        IkkeOmfattetFamilie ikkeOmfattetBarn = new IkkeOmfattetFamilie("fnrIkkeOmfattet", OVER_18_AR.getKode(), null);
        ikkeOmfattetBarn.sammensattNavn = "Ikke Omfattet Barn";
        ikkeOmfattetBarn.ident = "1111111111";

        return new AvklarteMedfolgendeFamilie(Set.of(omfattetBarn), Set.of(ikkeOmfattetBarn));
    }
}
