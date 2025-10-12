package com.zehro_mc.pokenotifier.client.compat;

import com.cobblemon.mod.common.CobblemonItems;
import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.advancement.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.AdvancementToast;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.Optional;

/**
 * Clase de compatibilidad que contiene todo el código que interactúa directamente con AdvancementPlaques.
 * Esta clase solo debe ser llamada si se ha confirmado que el mod está cargado.
 */
public class AdvancementPlaquesCompat {

    public static void showPlaque(Text title, boolean isActivation) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        // 1. Seleccionamos el tipo de marco. GOAL para un sonido de logro, TASK para un sonido más sutil.
        AdvancementFrame frameType = isActivation ? AdvancementFrame.GOAL : AdvancementFrame.TASK;

        // 2. Creamos un 'DisplayInfo' falso. La clave es pasar nuestro texto como el TÍTULO y NO especificar un fondo.
        Optional<AdvancementDisplay> displayInfo = Optional.of(new AdvancementDisplay(
                new ItemStack(CobblemonItems.POKE_BALL), // Usamos el icono correcto de Cobblemon
                title, // Este es el texto grande que se mostrará en la placa.
                Text.empty(), // La descripción se ignora, así que la dejamos vacía.
                Optional.empty(), // Al no especificar un fondo, AdvancementPlaques no muestra el título superior.
                frameType,
                true, // Mostrar el toast (la placa)
                false, // No anunciar en el chat
                false // No está oculto
        ));

        // 3. Creamos un 'Advancement' y 'AdvancementEntry' falsos para envolver el DisplayInfo.
        Advancement dummyAdvancement = new Advancement(Optional.empty(), displayInfo, AdvancementRewards.NONE, Collections.emptyMap(), AdvancementRequirements.EMPTY, false);
        AdvancementEntry dummyEntry = new AdvancementEntry(Identifier.of(PokeNotifier.MOD_ID, "dummy_advancement"), dummyAdvancement);

        // 4. Creamos el Toast y lo añadimos a la cola. AdvancementPlaques lo interceptará y mostrará la placa.
        client.getToastManager().add(new AdvancementToast(dummyEntry));
    }
}