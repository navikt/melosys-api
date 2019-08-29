package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.MaritimtArbeid;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
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

    public static AvklartVirksomhet lagNorskVirksomhet() {
        return new AvklartVirksomhet("Bedrift AS", "123456789", lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID);
    }

    public static ForetakUtland lagForetakUtland() {
        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.navn = "Company International Ltd.";
        foretakUtland.orgnr = "12345678910";
        foretakUtland.uuid = "49m8gf-9dk4j0";
        foretakUtland.adresse = lagStrukturertAdresse();
        foretakUtland.adresse.landkode = "NO";
        return foretakUtland;
    }

    public static Saksopplysning lagSoeknadssaksopplysning(SoeknadDokument søknad) {
        return lagSaksopplysning(SaksopplysningType.SØKNAD, søknad);
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
        when(avklartMaritimtArbeid.getNavn()).thenReturn("Dunfjæder");
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
}