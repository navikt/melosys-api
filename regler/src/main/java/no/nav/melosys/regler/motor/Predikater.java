package no.nav.melosys.regler.motor;

public final class Predikater {

    private Predikater() {}
    
    public static final Predikat manglerVerdi(Object o) {
        return () -> {return o == null;};
    }

}
