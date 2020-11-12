package no.nav.melosys.integrasjon.dokgen.dto;


import java.time.LocalDateTime;
import java.util.List;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstyper;

public class SaksbehandlingstidSoknad extends Flettedata {
    private String fodselsnr;
    private String saksnummer;
    private LocalDateTime dagensDato;
    private LocalDateTime datoMottatt;
    private LocalDateTime datoBehandlingstid;
    private String navnBruker;
    private String navnMottaker;
    private List<String> adresselinjer;
    private String postnr;
    private String poststed;
    private Sakstyper typeSoknad;
    private Aktoersroller avsenderTypeSoknad;
    private boolean mottakerRepresentantForBruker;
    private String avsenderSoknad;
    private String avsenderLand;

    public SaksbehandlingstidSoknad(String fodselsnr, String saksnummer, LocalDateTime dagensDato,
                                    LocalDateTime datoMottatt, LocalDateTime datoBehandlingstid,
                                    String navnBruker, String navnMottaker, List<String> adresselinjer,
                                    String postnr, String poststed, Sakstyper typeSoknad,
                                    Aktoersroller avsenderTypeSoknad, boolean mottakerRepresentantForBruker,
                                    String avsenderSoknad, String avsenderLand) {
        this.fodselsnr = fodselsnr;
        this.saksnummer = saksnummer;
        this.dagensDato = dagensDato;
        this.datoMottatt = datoMottatt;
        this.datoBehandlingstid = datoBehandlingstid;
        this.navnBruker = navnBruker;
        this.navnMottaker = navnMottaker;
        this.adresselinjer = adresselinjer;
        this.postnr = postnr;
        this.poststed = poststed;
        this.typeSoknad = typeSoknad;
        this.avsenderTypeSoknad = avsenderTypeSoknad;
        this.mottakerRepresentantForBruker = mottakerRepresentantForBruker;
        this.avsenderSoknad = avsenderSoknad;
        this.avsenderLand = avsenderLand;
    }

    public static class Builder {
        private String fodselsnr;
        private String saksnummer;
        private LocalDateTime dagensDato;
        private LocalDateTime datoMottatt;
        private LocalDateTime datoBehandlingstid;
        private String navnBruker;
        private String navnMottaker;
        private List<String> adresselinjer;
        private String postnr;
        private String poststed;
        private Sakstyper typeSoknad;
        private Aktoersroller avsenderTypeSoknad;
        private boolean mottakerRepresentantForBruker;
        private String avsenderSoknad;
        private String avsenderLand;

        public Builder medFodselsnr(String fodselsnr) {
            this.fodselsnr = fodselsnr;
            return this;
        }

        public Builder medSaksnummer(String saksnummer) {
            this.saksnummer = saksnummer;
            return this;
        }

        public Builder medDagensDato(LocalDateTime dagensDato) {
            this.dagensDato = dagensDato;
            return this;
        }

        public Builder medDatoMottatt(LocalDateTime datoMottatt) {
            this.datoMottatt = datoMottatt;
            return this;
        }

        public Builder medDatoBehandlingstid(LocalDateTime datoBehandlingstid) {
            this.datoBehandlingstid = datoBehandlingstid;
            return this;
        }

        public Builder medNavnBruker(String navnBruker) {
            this.navnBruker = navnBruker;
            return this;
        }

        public Builder medNavnMottaker(String navnMottaker) {
            this.navnMottaker = navnMottaker;
            return this;
        }

        public Builder medAdresselinjer(List<String> adresselinjer) {
            this.adresselinjer = adresselinjer;
            return this;
        }

        public Builder medPostnr(String postnr) {
            this.postnr = postnr;
            return this;
        }

        public Builder medPoststed(String poststed) {
            this.poststed = poststed;
            return this;
        }

        public Builder medTypeSoknad(Sakstyper typeSoknad) {
            this.typeSoknad = typeSoknad;
            return this;
        }

        public Builder medAvsenderTypeSoknad(Aktoersroller avsenderTypeSoknad) {
            this.avsenderTypeSoknad = avsenderTypeSoknad;
            return this;
        }

        public Builder medMottakerRepresentantForBruker(boolean mottakerRepresentantForBruker) {
            this.mottakerRepresentantForBruker = mottakerRepresentantForBruker;
            return this;
        }

        public Builder medAvsenderSoknad(String avsenderSoknad) {
            this.avsenderSoknad = avsenderSoknad;
            return this;
        }

        public Builder medAvsenderLand(String avsenderLand) {
            this.avsenderLand = avsenderLand;
            return this;
        }

        public SaksbehandlingstidSoknad build() {
            return new SaksbehandlingstidSoknad(fodselsnr, saksnummer, dagensDato, datoMottatt,
                datoBehandlingstid, navnBruker, navnMottaker, adresselinjer, postnr, poststed,
                typeSoknad, avsenderTypeSoknad, mottakerRepresentantForBruker, avsenderSoknad, avsenderLand);
        }
    }

    public String getFodselsnr() {
        return fodselsnr;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public LocalDateTime getDagensDato() {
        return dagensDato;
    }

    public LocalDateTime getDatoMottatt() {
        return datoMottatt;
    }

    public LocalDateTime getDatoBehandlingstid() {
        return datoBehandlingstid;
    }

    public String getNavnBruker() {
        return navnBruker;
    }

    public String getNavnMottaker() {
        return navnMottaker;
    }

    public List<String> getAdresselinjer() {
        return adresselinjer;
    }

    public String getPostnr() {
        return postnr;
    }

    public String getPoststed() {
        return poststed;
    }

    public Sakstyper getTypeSoknad() {
        return typeSoknad;
    }

    public Aktoersroller getAvsenderTypeSoknad() {
        return avsenderTypeSoknad;
    }

    public boolean isMottakerRepresentantForBruker() {
        return mottakerRepresentantForBruker;
    }

    public String getAvsenderSoknad() {
        return avsenderSoknad;
    }

    public String getAvsenderLand() {
        return avsenderLand;
    }
}
