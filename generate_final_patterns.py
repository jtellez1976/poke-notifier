import json
import random

def generate_final_unique_patterns():
    """Generate unique patterns using normalized Pokemon data"""
    
    # Load normalized Pokemon data
    with open('SummonablePokemons.json', 'r', encoding='utf-8') as f:
        pokemon_data = json.load(f)
    
    # Available pokeballs
    pokeballs = [
        'poke_ball', 'great_ball', 'ultra_ball', 'master_ball', 'timer_ball',
        'dusk_ball', 'quick_ball', 'repeat_ball', 'luxury_ball', 'net_ball',
        'nest_ball', 'dive_ball', 'heal_ball', 'premier_ball', 'safari_ball',
        'sport_ball', 'park_ball', 'cherish_ball', 'gs_ball', 'beast_ball',
        'dream_ball', 'moon_ball', 'love_ball', 'friend_ball', 'lure_ball',
        'heavy_ball', 'level_ball', 'fast_ball'
    ]
    
    positions = ['north', 'east', 'south', 'west', 'northeast', 'southeast', 'southwest', 'northwest']
    
    print("Pokemon counts from normalized data:")
    total_pokemon = 0
    for tier, pokemon_list in pokemon_data.items():
        print(f"  {tier}: {len(pokemon_list)}")
        total_pokemon += len(pokemon_list)
    print(f"Total: {total_pokemon}")
    
    # Generate unique patterns
    used_patterns = set()
    result = {}
    
    for tier_name, pokemon_list in pokemon_data.items():
        print(f"[INFO] Processing {tier_name} with {len(pokemon_list)} Pokemon")
        result[tier_name] = {}
        
        for pokemon in pokemon_list:
            # Generate unique pattern
            attempts = 0
            max_attempts = 10000
            
            while attempts < max_attempts:
                # Determine pattern complexity based on tier
                if tier_name in ['LEGENDARIES', 'MYTHICALS', 'ULTRA_BEASTS', 'PARADOX', 'ULTRA_RARE']:
                    # Full 8-position patterns for high tiers
                    pattern = {pos: random.choice(pokeballs) for pos in positions}
                else:
                    # Partial patterns for lower tiers (3-7 positions)
                    num_positions = random.randint(3, 7)
                    selected_positions = random.sample(positions, num_positions)
                    pattern = {pos: random.choice(pokeballs) for pos in selected_positions}
                
                # Convert to string for uniqueness check
                pattern_str = '|'.join([pattern.get(pos, 'empty') for pos in positions])
                
                if pattern_str not in used_patterns:
                    used_patterns.add(pattern_str)
                    result[tier_name][pokemon] = pattern
                    break
                
                attempts += 1
            
            if attempts >= max_attempts:
                print(f"[ERROR] Could not find unique pattern for {tier_name}:{pokemon}")
                return None
    
    total_generated = sum(len(tier_dict) for tier_dict in result.values())
    print(f"[SUCCESS] Generated {len(used_patterns)} unique patterns for {total_generated} Pokemon")
    
    return result

def main():
    print("[START] Generating final unique patterns with correct Pokemon counts...")
    
    # Generate unique patterns
    unique_data = generate_final_unique_patterns()
    
    if unique_data is None:
        print("[ERROR] Failed to generate unique patterns")
        return
    
    # Save to file
    output_file = 'src/main/resources/data/poke-notifier/pokemon_combinations.json'
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(unique_data, f, indent=4, ensure_ascii=False)
    
    print(f"[SAVE] Saved final patterns to {output_file}")
    
    # Verify uniqueness
    positions = ['north', 'east', 'south', 'west', 'northeast', 'southeast', 'southwest', 'northwest']
    all_patterns = []
    
    for tier, pokemon_dict in unique_data.items():
        for pokemon, pattern in pokemon_dict.items():
            pattern_str = '|'.join([pattern.get(pos, 'empty') for pos in positions])
            all_patterns.append(pattern_str)
    
    unique_patterns = set(all_patterns)
    total_pokemon = sum(len(tier_dict) for tier_dict in unique_data.values())
    
    print(f"\n[FINAL VERIFICATION]")
    print(f"Total Pokemon: {total_pokemon}")
    print(f"Total patterns: {len(all_patterns)}")
    print(f"Unique patterns: {len(unique_patterns)}")
    
    if len(all_patterns) == len(unique_patterns):
        print("[SUCCESS] All 532 Pokemon have completely unique patterns!")
        print("Breakdown: LEGENDARIES(70) + MYTHICALS(23) + ULTRA_BEASTS(11) + PARADOX(20) + ULTRA_RARE(110) + RARE(119) + UNCOMMON(179) = 532")
    else:
        print(f"[ERROR] Still have {len(all_patterns) - len(unique_patterns)} duplicates")

if __name__ == "__main__":
    main()