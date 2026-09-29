package cl.licitawatch.licitaciones.dto;

import jakarta.validation.constraints.Size;

public record SeleccionarPostulanteRequest(@Size(max = 2000) String comentario) {
}
