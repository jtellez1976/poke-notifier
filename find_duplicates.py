import json
import os

def pattern_to_string(pattern):
    """Convert pattern dict to comparable string"""
    positions = ['north', 'east', 'south', 'west', 'northeast', 'southeast', 'southwest', 'northwest']
    return '|'.join([pattern.get(pos, 'empty') for pos in positions])

def find_duplicates():
    """Find all duplicate patterns"""
    file_path = 'src/main/resources/data/poke-notifier/pokemon_combinations.json'
    
    if not os.path.exists(file_path):
        print(f"[ERROR] File not found: {file_path}")
        return
    
    try:
        with open(file_path, 'r', encoding='utf-8-sig') as f:
            data = json.load(f)
    except Exception as e:
        print(f"[ERROR] Failed to load JSON: {e}")
        return
    
    pattern_to_pokemon = {}
    duplicates = []
    total_pokemon = 0
    
    for tier, pokemon_dict in data.items():
        print(f"[INFO] Processing tier {tier} with {len(pokemon_dict)} Pokemon")
        total_pokemon += len(pokemon_dict)
        
        for pokemon, pattern in pokemon_dict.items():
            pattern_str = pattern_to_string(pattern)
            
            if pattern_str in pattern_to_pokemon:
                duplicates.append({
                    'pattern': pattern_str,
                    'pokemon1': pattern_to_pokemon[pattern_str],
                    'pokemon2': f"{tier}:{pokemon}"
                })
                print(f"[DUPLICATE] {pattern_to_pokemon[pattern_str]} == {tier}:{pokemon}")
                print(f"            Pattern: {pattern_str}")
            else:
                pattern_to_pokemon[pattern_str] = f"{tier}:{pokemon}"
    
    print(f"\n[SUMMARY] Total Pokemon: {total_pokemon}")
    print(f"[SUMMARY] Unique patterns: {len(pattern_to_pokemon)}")
    print(f"[SUMMARY] Duplicates found: {len(duplicates)}")
    
    if duplicates:
        print(f"\n[DUPLICATES] Found {len(duplicates)} duplicate patterns:")
        for i, dup in enumerate(duplicates, 1):
            print(f"  {i}. {dup['pokemon1']} == {dup['pokemon2']}")
    else:
        print("\n[SUCCESS] No duplicates found!")

if __name__ == "__main__":
    find_duplicates()