package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.Gateadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.MaritimtArbeid;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.MaritimtArbeidssted;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BrevDataTestUtils {

    public static StrukturertAdresse lagStrukturertAdresse() {
        StrukturertAdresse addr = new StrukturertAdresse();
        addr.gatenavn = "Strukturert Gate";
        addr.husnummer = "12B";
        addr.poststed = "Poststed";
        addr.postnummer = "4321";
        addr.landkode = Landkoder.BG.getKode();
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
        foretakUtland.adresse.landkode = "NO";
        foretakUtland.selvstendigNæringsvirksomhet = selvstendig;
        return foretakUtland;
    }

    public static Saksopplysning lagSoeknadssaksopplysning(SoeknadDokument søknad) {
        return lagSaksopplysning(SaksopplysningType.SØKNAD, søknad);
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
        MaritimtArbeid maritimtArbeid = lagMaritimtArbeid();
        AvklartMaritimtArbeid avklartMaritimtArbeid = lagAvklartMaritimtArbeid();
        return new MaritimtArbeidssted(maritimtArbeid, avklartMaritimtArbeid);
    }

    public static AvklartMaritimtArbeid lagAvklartMaritimtArbeid() {
        AvklartMaritimtArbeid avklartMaritimtArbeid = mock(AvklartMaritimtArbeid.class);
        when(avklartMaritimtArbeid.getMaritimtype()).thenReturn(Maritimtyper.SKIP);
        when(avklartMaritimtArbeid.getLand()).thenReturn(Landkoder.GB.getKode());
        return avklartMaritimtArbeid;
    }

    public static MaritimtArbeid lagMaritimtArbeid() {
        MaritimtArbeid maritimtArbeid = new MaritimtArbeid();
        maritimtArbeid.foretakOrgnr = "123456789";
        maritimtArbeid.foretakNavn = "Equinor GB";
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
}