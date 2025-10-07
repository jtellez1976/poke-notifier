import pandas as pd, json, os, zipfile

# === Configuración general ===
RADIUS = 64
CHECK_INTERVAL = 100
COOLDOWN = 600
SOUND_RARE = "minecraft:entity.player.levelup"
SOUND_ULTRARARE = "minecraft:entity.ender_dragon.growl"

# === Cargar Excel ===
df = pd.read_excel("AllPokemons.xlsx", engine="openpyxl")
df["RARITY_CLEAN"] = df["RARITY"].astype(str).str.lower().str.strip()
rare = df[df["RARITY_CLEAN"] == "rare"]["POKEMON"].dropna().unique()
ultra = df[df["RARITY_CLEAN"].isin(["ultra-rare","ultra rare","ultra_rare"])]["POKEMON"].dropna().unique()

base_dir = "rare_ultrarare_alerts/data/rare_alert/functions"
os.makedirs(base_dir, exist_ok=True)

# === Función para generar comandos ===
def build_scan_file(pokemons, color, sound, path):
    lines = [f"# {len(pokemons)} Pokémon detectados"]
    for p in pokemons:
        species = str(p).strip().lower()
        lines += [
            f"execute as @a at @s if entity @e[type=cobblemon:pokemon,distance=..{RADIUS},nbt={{Species:\"{species}\"}}] run playsound {sound} master @s",
            f'execute as @a at @s if entity @e[type=cobblemon:pokemon,distance=..{RADIUS},nbt={{Species:"{species}"}}] run tellraw @s ["",{{"text":"¡{p} ha aparecido cerca en ","color":"{color}"}},{{"selector":"@e[type=cobblemon:pokemon,limit=1,sort=nearest,nbt={{Species:\\"{species}\\"}}]","color":"white"}},{{"text":"! ","color":"{color}"}},{{"text":"[TP]","color":"blue","clickEvent":{{"action":"run_command","value":"/tp @p @e[type=cobblemon:pokemon,limit=1,sort=nearest,nbt={{Species:\\"{species}\\"}}]"}}}}]'
        ]
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

# === Generar archivos ===
build_scan_file(rare, "red", SOUND_RARE, f"{base_dir}/scan_rare.mcfunction")
build_scan_file(ultra, "light_purple", SOUND_ULTRARARE, f"{base_dir}/scan_ultrarare.mcfunction")

with open("rare_ultrarare_alerts/data/rare_alert/functions/tick.mcfunction", "w", encoding="utf-8") as f:
    f.write(f"""scoreboard players add #timer rare_alert 1
execute as @a if score #timer rare_alert matches {CHECK_INTERVAL} run function rare_alert:scan_rare
execute as @a if score #timer rare_alert matches {CHECK_INTERVAL} run function rare_alert:scan_ultrarare
execute if score #timer rare_alert matches {CHECK_INTERVAL}.. run scoreboard players set #timer rare_alert 0
""")

os.makedirs("rare_ultrarare_alerts", exist_ok=True)
with open("rare_ultrarare_alerts/pack.mcmeta","w",encoding="utf-8") as f:
    json.dump({"pack":{"pack_format":48,"description":"Alertas de Pokémon Raros y Ultra-Raros (Cobblemon)"}},f,indent=2)

with open("rare_ultrarare_alerts/config.txt","w",encoding="utf-8") as f:
    f.write(f"""# Configuración
radius={RADIUS}
check_interval={CHECK_INTERVAL}
cooldown={COOLDOWN}
rare_sound={SOUND_RARE}
ultrarare_sound={SOUND_ULTRARARE}
""")

with open("rare_ultrarare_alerts/README.txt","w",encoding="utf-8") as f:
    f.write("""INSTALACIÓN:
1. Copia rare_ultrarare_alerts.zip a world/datapacks/
2. Entra al mundo y ejecuta /reload
3. (Solo primera vez) ejecuta:
   /scoreboard objectives add rare_alert dummy
   /scoreboard players set #timer rare_alert 0 rare_alert
""")

# === Empaquetar ZIP ===
with zipfile.ZipFile("rare_ultrarare_alerts.zip","w",zipfile.ZIP_DEFLATED) as zipf:
    for root,_,files in os.walk("rare_ultrarare_alerts"):
        for f in files:
            path=os.path.join(root,f)
            zipf.write(path, arcname=os.path.relpath(path,"rare_ultrarare_alerts"))

print("✅ Datapack generado: rare_ultrarare_alerts.zip")
