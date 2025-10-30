def count_pokemon_by_tier():
    """Count Pokemon in each tier from AllPokemons.json"""
    
    with open('AllPokemons.json', 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    current_tier = None
    counts = {}
    
    for line in lines:
        line = line.strip()
        if not line:
            continue
            
        # Check if this is a tier name
        if line in ['LEGENDARIES', 'MYTHICALS', 'ULTRA_BEASTS', 'PARADOX', 'ULTRA_RARE', 'RARE', 'UNCOMMON', 'COMMON']:
            current_tier = line
            counts[current_tier] = 0
        elif current_tier:
            counts[current_tier] += 1
    
    print("Pokemon count by tier:")
    total = 0
    for tier, count in counts.items():
        print(f"  {tier}: {count}")
        if tier != 'COMMON':  # Don't count COMMON
            total += count
    
    print(f"\nTotal (excluding COMMON): {total}")
    print(f"Expected for summoning: 532 (70+23+11+20+110+119+179)")
    
    return counts

if __name__ == "__main__":
    count_pokemon_by_tier()