package com.liga.calendar;

import java.util.List;

/**
 * Static, in-memory list of the league's collaborators, shown on the
 * "Colaboradores" section (see {@link ColaboradoresController}).
 * <p>
 * There's no database yet — add, edit, or remove a {@link Collaborator}
 * here directly. Seeded with one placeholder entry as an example of the
 * fields to fill in — swap it out for the real team.
 */
public final class CollaboratorCatalog {

    public static final List<Collaborator> ALL = List.of(
            new Collaborator("ejemplo", "Nombre Apellido", "Organizador",
                    "Breve descripcion de que hace en la liga.", null,
                    "https://instagram.com/liga.pauper.mvd")
    );

    private CollaboratorCatalog() {
    }
}
