package no.nav.melosys.tjenester.gui.patch;

/**
 * Interface for å støtte HTTP PATCH
 * Se https://github.com/dscheerens/patching-jax-rs
 */
public interface ObjectPatch {
    <T> T apply(T target) throws ObjectPatchException;
}
