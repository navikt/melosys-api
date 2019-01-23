package no.nav.melosys.domain.avklartefakta;

import no.nav.melosys.domain.Kodeverk;

public enum AvklartefaktaType implements Kodeverk {

    // AvklarteFakta fra https://confluence.adeo.no/display/TEESSI/Kodeverk+i+Melosys#KodeverkiMelosys-AvklarteFakta
    ARBEIDSLAND("ARBEIDSLAND", "Arbeidsland"),
    ARBEID_SOKKEL_SKIP("ARBEID_SOKKEL_SKIP", "Arbeid på sokkel eller skip"),
    AVKLARTE_ARBEIDSGIVER("AVKLARTE_ARBEIDSGIVER", "Avklarte arbeidsgiver"),
    TIDLIGERE_LOVVALGSPERIODE("TIDLIGERE_LOVVALGSPERIODE", "Tidligere lovvalgsperiode"),
    YRKESGRUPPE("YRKESGRUPPE", "Yrkesgruppe"),
    // Støtter flyt i stegvelger frontend // FIXME Synces med frontend
    MOTTAR_KONTANTYTELSE("MOTTAR_KONTANTYTELSE", "Mottar kontantytelse"),
    OFFENTLIG_TJENESTEMANN("OFFENTLIG_TJENESTEMANN", "Offentlig tjenestemann"),
    BOSTEDSLAND("BOSTEDSLAND", "Bostedsland"),
    SOKKEL_SKIP("SOKKEL_SKIP", "Arbeid på sokkel eller skip");

    private String kode;
    private String beskrivelse;

    AvklartefaktaType(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }
}
