package no.nav.melosys.domain.eessi.sed;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.eessi.SvarAnmodningUnntak;

public class SedDataDto extends SedGrunnlagDto {
    //Persondok.
    private List<FamilieMedlem> familieMedlem = new ArrayList<>();
    private Bruker bruker;

    //Andre medlemsvariabler
    private List<Virksomhet> utenlandskeVirksomheter = new ArrayList<>();

    //Videresending av søknad
    private String avklartBostedsland;

    private Long gsakSaksnummer;

    //Lovvalg
    private List<Lovvalgsperiode> tidligereLovvalgsperioder = new ArrayList<>();

    private List<String> mottakerIder;

    private SvarAnmodningUnntak svarAnmodningUnntak;

    private UtpekingAvvisDto utpekingAvvis;

    public List<FamilieMedlem> getFamilieMedlem() {
        return familieMedlem;
    }

    public void setFamilieMedlem(List<FamilieMedlem> familieMedlem) {
        this.familieMedlem = familieMedlem;
    }

    public Bruker getBruker() {
        return bruker;
    }

    public void setBruker(Bruker bruker) {
        this.bruker = bruker;
    }

    public List<Virksomhet> getUtenlandskeVirksomheter() {
        return utenlandskeVirksomheter;
    }

    public void setUtenlandskeVirksomheter(List<Virksomhet> utenlandskeVirksomheter) {
        this.utenlandskeVirksomheter = utenlandskeVirksomheter;
    }

    public String getAvklartBostedsland() {
        return avklartBostedsland;
    }

    public void setAvklartBostedsland(String avklartBostedsland) {
        this.avklartBostedsland = avklartBostedsland;
    }

    public Long getGsakSaksnummer() {
        return gsakSaksnummer;
    }

    public void setGsakSaksnummer(Long gsakSaksnummer) {
        this.gsakSaksnummer = gsakSaksnummer;
    }

    public List<Lovvalgsperiode> getTidligereLovvalgsperioder() {
        return tidligereLovvalgsperioder;
    }

    public void setTidligereLovvalgsperioder(List<Lovvalgsperiode> tidligereLovvalgsperioder) {
        this.tidligereLovvalgsperioder = tidligereLovvalgsperioder;
    }

    public List<String> getMottakerIder() {
        return mottakerIder;
    }

    public void setMottakerIder(List<String> mottakerIder) {
        this.mottakerIder = mottakerIder;
    }

    public SvarAnmodningUnntak getSvarAnmodningUnntak() {
        return svarAnmodningUnntak;
    }

    public void setSvarAnmodningUnntak(SvarAnmodningUnntak svarAnmodningUnntak) {
        this.svarAnmodningUnntak = svarAnmodningUnntak;
    }

    public UtpekingAvvisDto getUtpekingAvvis() {
        return utpekingAvvis;
    }

    public void setUtpekingAvvis(UtpekingAvvisDto utpekingAvvis) {
        this.utpekingAvvis = utpekingAvvis;
    }
}
