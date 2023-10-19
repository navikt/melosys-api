package no.nav.melosys.domain.brev;

import java.util.Objects;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;

public final class Mottaker {
    private Mottakerroller rolle;
    private String aktørId;
    private String personIdent;
    private String orgnr;
    private String institusjonID;
    private Land_iso2 trygdemyndighetLand;

    public Mottaker() {
    }

    public Mottaker(Mottakerroller rolle, String aktørId, String personIdent, String orgnr, String institusjonID, Land_iso2 trygdemyndighetLand) {
        this.rolle = rolle;
        this.aktørId = aktørId;
        this.personIdent = personIdent;
        this.orgnr = orgnr;
        this.institusjonID = institusjonID;
        this.trygdemyndighetLand = trygdemyndighetLand;
    }

    public static Mottaker medRolle(Mottakerroller rolle) {
        var mottaker = new Mottaker();
        mottaker.setRolle(rolle);
        return mottaker;
    }

    public static Mottaker av(Aktoer aktoer) {
        return new Mottaker(mottakerrolleAv(aktoer.getRolle()), aktoer.getAktørId(), aktoer.getPersonIdent(), aktoer.getOrgnr(), aktoer.getInstitusjonId(), aktoer.getTrygdemyndighetLand());
    }

    private static Mottakerroller mottakerrolleAv(Aktoersroller aktoersrolle) {
        return switch (aktoersrolle) {
            case BRUKER -> Mottakerroller.BRUKER;
            case VIRKSOMHET -> Mottakerroller.VIRKSOMHET;
            case ARBEIDSGIVER -> Mottakerroller.ARBEIDSGIVER;
            case REPRESENTANT, FULLMEKTIG -> Mottakerroller.FULLMEKTIG;
            case TRYGDEMYNDIGHET -> Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET;
            default -> throw new FunksjonellException("Støtter ikke mapping av aktoersrolle" + aktoersrolle);
        };
    }

    public static Mottaker av(NorskMyndighet norskMyndighet) {
        return switch (norskMyndighet) {
            case HELFO -> mottakerNorskMyndighet(NorskMyndighet.HELFO.getOrgnr());
            case SKATTEETATEN -> mottakerNorskMyndighet(NorskMyndighet.SKATTEETATEN.getOrgnr());
            case SKATTEINNKREVER_UTLAND -> mottakerNorskMyndighet(NorskMyndighet.SKATTEINNKREVER_UTLAND.getOrgnr());
        };
    }

    private static Mottaker mottakerNorskMyndighet(String orgnr) {
        var mottaker = Mottaker.medRolle(Mottakerroller.NORSK_MYNDIGHET);
        mottaker.setOrgnr(orgnr);
        return mottaker;
    }

    public boolean erOrganisasjon() {
        return switch (rolle) {
            case BRUKER -> false;
            case FULLMEKTIG -> orgnr != null;
            default -> true;
        };
    }

    public boolean erUtenlandskMyndighet() {
        return rolle == Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET && (institusjonID != null || trygdemyndighetLand != null);
    }

    public Land_iso2 hentMyndighetLandkode() {
        if (erUtenlandskMyndighet()) {
            return institusjonID != null ? UtenlandskMyndighet.konverterInstitusjonIdTilLandkode(institusjonID) : trygdemyndighetLand;
        }
        throw new TekniskException("Mottaker er ikke en utenlandsk myndighet");
    }

    public Mottakerroller getRolle() {
        return rolle;
    }

    public String getAktørId() {
        return aktørId;
    }

    public String getPersonIdent() {
        return personIdent;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public String getInstitusjonID() {
        return institusjonID;
    }

    public Land_iso2 getTrygdemyndighetLand() {
        return trygdemyndighetLand;
    }


    public void setRolle(Mottakerroller rolle) {
        this.rolle = rolle;
    }

    public void setAktørId(String aktørId) {
        this.aktørId = aktørId;
    }

    public void setPersonIdent(String personIdent) {
        this.personIdent = personIdent;
    }

    public void setOrgnr(String orgnr) {
        this.orgnr = orgnr;
    }

    public void setInstitusjonID(String institusjonID) {
        this.institusjonID = institusjonID;
    }

    public void setTrygdemyndighetLand(Land_iso2 trygdemyndighetLand) {
        this.trygdemyndighetLand = trygdemyndighetLand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Mottaker mottaker)) {
            return false;
        }
        return rolle.equals(mottaker.rolle) &&
            Objects.equals(aktørId, mottaker.aktørId) &&
            Objects.equals(personIdent, mottaker.personIdent) &&
            Objects.equals(orgnr, mottaker.orgnr) &&
            Objects.equals(institusjonID, mottaker.institusjonID);
    }
}
