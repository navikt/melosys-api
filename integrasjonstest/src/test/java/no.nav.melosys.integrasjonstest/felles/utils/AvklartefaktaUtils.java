package no.nav.melosys.integrasjonstest.felles.utils;

import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;

public final class AvklartefaktaUtils {
    private AvklartefaktaUtils() {}

    public static AvklartefaktaDto lagAvklartVirksomhet(String orgnr) {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("TRUE");
        avklartefakta.setType(Avklartefaktatyper.VIRKSOMHET);
        avklartefakta.setReferanse(Avklartefaktatyper.VIRKSOMHET.getKode());
        avklartefakta.setSubjekt(orgnr);
        return new AvklartefaktaDto(avklartefakta);
    }

    public static AvklartefaktaDto lagAvklartSoeknadsland(Landkoder søknadsland) {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("TRUE");
        avklartefakta.setReferanse("SOEKNADSLAND");
        avklartefakta.setSubjekt(søknadsland.getKode());
        return new AvklartefaktaDto(avklartefakta);
    }

    public static AvklartefaktaDto lagAvklartYrkesgruppe(Yrkesgrupper yrkesgruppe) {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta(yrkesgruppe.getKode());
        avklartefakta.setType(Avklartefaktatyper.YRKESGRUPPE);
        avklartefakta.setReferanse(Avklartefaktatyper.YRKESGRUPPE.getKode());
        avklartefakta.setSubjekt(null);
        return new AvklartefaktaDto(avklartefakta);
    }

    public static AvklartefaktaDto lagMarginaltArbeid(Landkoder landkoder) {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("TRUE");
        avklartefakta.setType(Avklartefaktatyper.MARGINALT_ARBEID);
        avklartefakta.setReferanse(Avklartefaktatyper.MARGINALT_ARBEID.getKode());
        avklartefakta.setSubjekt(landkoder.getKode());
        return new AvklartefaktaDto(avklartefakta);
    }
}
