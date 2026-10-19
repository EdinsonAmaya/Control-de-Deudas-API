package com.edinsonamaya.control_deudas.folder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FolderRequest(@NotBlank(message = "El nombre de la carpeta es obligatorio")
                            @Size(max = 120, message = "El nombre de la carpeta debe tener máximo 120 caracteres")
                            String name) {
}
