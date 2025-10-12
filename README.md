# Poke Notifier

**¡Nunca más te pierdas un Pokémon raro!**

Poke Notifier es un mod para Cobblemon diseñado para servidores que mejora drásticamente la experiencia de encontrar y cazar Pokémon raros. Alerta a los jugadores cuando un Pokémon especial aparece, proporcionando su nombre, rareza y coordenadas.

## ✨ Características Principales

- **Notificaciones de Spawns Raros:** Alertas en tiempo real cuando aparece un Pokémon Legendario, Mítico, Shiny, Ultra Raro y más.
- **Múltiples Canales de Alerta:** Recibe notificaciones a través de mensajes en el chat, un HUD de notificación en pantalla (Toast) y alertas de sonido. ¡Totalmente configurable por el jugador!
- **Modo "Catch 'em All":** ¡El desafío definitivo para los coleccionistas! Activa el seguimiento para una generación específica (ej. Kanto) y recibe notificaciones especiales de los Pokémon que aún no has capturado.
- **HUD de Progreso:** Mientras el modo "Catch 'em All" está activo, un HUD persistente en pantalla muestra tu progreso de captura para la generación seleccionada (ej. `Gen1: 50/151`).
- **Sincronización de Pokédex:** Al usar el modo "Catch 'em All" por primera vez, el mod escanea tu PC y tu equipo para acreditarte todos los Pokémon que ya has capturado. ¡Tu progreso se respeta!
- **Listas de Caza Personalizadas:** ¿Buscas un Pokémon específico que no es necesariamente raro? Añádelo a tu lista personalizada y recibe una notificación especial cuando aparezca.
- **Efecto de Resplandor:** Los Pokémon notificados reciben un efecto de resplandor temporal, haciéndolos fáciles de localizar.
- **Altamente Configurable:** Tanto los jugadores como los administradores del servidor tienen control total sobre el funcionamiento del mod a través de comandos intuitivos.
- **Compatibilidad Opcional:** Se integra con `AdvancementPlaques` para mostrar notificaciones de activación de modo más estilizadas, con un sistema de fallback si el mod no está instalado.

---

## 🎮 Comandos para Jugadores (Cliente)

Todos los comandos de jugador comienzan con `/pnc`.

### Gestión de Alertas

Controla cómo quieres recibir las notificaciones.

| Comando | Descripción |
| :--- | :--- |
| `/pnc status` | Muestra el estado actual de todas tus configuraciones de cliente. |
| `/pnc silent ON/OFF` | Activa o desactiva el **Modo Silencioso**, un interruptor maestro para todas las notificaciones. |
| `/pnc alert_chat ON/OFF` | Activa o desactiva las notificaciones en el chat. |
| `/pnc alert_toast ON/OFF` | Activa o desactiva las notificaciones en el HUD (pantalla). |
| `/pnc alert_sound ON/OFF` | Activa o desactiva los sonidos de alerta. |

### Lista de Caza Personalizada

Crea tu propia lista de Pokémon para rastrear.

| Comando | Descripción |
| :--- | :--- |
| `/pnc customlist add <pokemon>` | Añade un Pokémon a tu lista de caza personal. |
| `/pnc customlist remove <pokemon>` | Elimina un Pokémon de tu lista. |
| `/pnc customlist list` | Muestra todos los Pokémon en tu lista actual. |
| `/pnc customlist clear` | Limpia por completo tu lista de caza. |

### Modo "Catch 'em All"

¡El desafío para los verdaderos coleccionistas!

| Comando | Descripción |
| :--- | :--- |
| `/pnc catchemall enable <generacion>` | Activa el modo de caza para una generación (ej. `gen1`, `gen2`, etc.). |
| `/pnc catchemall disable <generacion>` | Desactiva el modo de caza para esa generación. |
| `/pnc catchemall list` | Muestra qué generación estás rastreando actualmente. |

### Otros Comandos

| Comando | Descripción |
| :--- | :--- |
| `/pnc version` | Muestra la versión actual del mod Poke Notifier. |

---

## 🛠️ Comandos para Administradores (Servidor)

Todos los comandos de administrador comienzan con `/pokenotifier` y requieren nivel de permiso 2 (OP).

| Comando | Descripción |
| :--- | :--- |
| `/pokenotifier status` | Muestra el estado de la configuración del lado del servidor. |
| `/pokenotifier reloadconfig` | Recarga todos los archivos de configuración del mod sin necesidad de reiniciar el servidor. |
| `/pokenotifier reloadconfig new` | Borra las configuraciones actuales y genera unas nuevas por defecto. |
| `/pokenotifier debug_mode enable/disable` | Activa o desactiva los logs de depuración detallados en la consola del servidor. |
| `/pokenotifier test_mode enable/disable` | Activa o desactiva el modo de prueba. Cuando está activo, el mod también notificará spawns no naturales (ej. de comandos). |
| `/pokenotifier testspawn <pokemon> [shiny]` | Genera un Pokémon específico para pruebas. Solo funciona si el `test_mode` está activado. |

---

## 🔧 Instalación

1.  Asegúrate de tener Fabric Loader instalado.
2.  Descarga la versión correcta de Fabric API y Cobblemon.
3.  Descarga la última versión de **Poke Notifier**.
4.  Coloca los tres archivos .jar en tu carpeta `mods`.
5.  (Opcional) Para notificaciones de activación de modo más estilizadas, instala `AdvancementPlaques` y su dependencia `Iceberg`.

¡Inicia el juego y disfruta de la caza!