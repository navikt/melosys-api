package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Yrkesaktivitetstyper;

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
        foretakUtland.adresse = lagStrukturertAdresse();
        foretakUtland.adresse.landkode = "NO";
        return foretakUtland;
    }
}
