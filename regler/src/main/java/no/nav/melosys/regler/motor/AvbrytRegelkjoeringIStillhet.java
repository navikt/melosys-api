package no.nav.melosys.regler.motor;

public class AvbrytRegelkjoeringIStillhet extends Regelpakke {
    
    @Regel
    public static void avbryt() {
        throw new AvbrytRegelkjoeringIStillhetException();
    }

}

class AvbrytRegelkjoeringIStillhetException extends RuntimeException {
}
