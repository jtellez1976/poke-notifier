import json

def normalize_pokemon_data():
    """Convert AllPokemons.json from text format to proper JSON structure"""
    
    # Read the text file
    with open('AllPokemons.json', 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    # Parse into proper structure
    pokemon_data = {}
    current_tier = None
    
    for line in lines:
        line = line.strip()
        if not line:
            continue
            
        # Check if this is a tier name
        if line in ['LEGENDARIES', 'MYTHICALS', 'ULTRA_BEASTS', 'PARADOX', 'ULTRA_RARE', 'RARE', 'UNCOMMON', 'COMMON']:
            current_tier = line
            pokemon_data[current_tier] = []
        elif current_tier:
            pokemon_data[current_tier].append(line)
    
    # Print counts
    print("Pokemon counts by tier:")
    total_non_common = 0
    for tier, pokemon_list in pokemon_data.items():
        print(f"  {tier}: {len(pokemon_list)}")
        if tier != 'COMMON':
            total_non_common += len(pokemon_list)
    
    print(f"\nTotal (excluding COMMON): {total_non_common}")
    
    # Save as proper JSON
    with open('AllPokemons_normalized.json', 'w', encoding='utf-8') as f:
        json.dump(pokemon_data, f, indent=2, ensure_ascii=False)
    
    print(f"\nSaved normalized data to AllPokemons_normalized.json")
    
    # Also create the summoning version (excluding COMMON)
    summoning_data = {k: v for k, v in pokemon_data.items() if k != 'COMMON'}
    
    with open('SummonablePokemons.json', 'w', encoding='utf-8') as f:
        json.dump(summoning_data, f, indent=2, ensure_ascii=False)
    
    print(f"Saved summonable Pokemon to SummonablePokemons.json")
    
    return summoning_data

if __name__ == "__main__":
    normalize_pokemon_data()