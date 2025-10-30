import json
import itertools
import random

def generate_unique_pokemon_patterns():
    """Generate completely unique patterns for all 532 Pokemon"""
    
    # Load AllPokemons.json to get the correct Pokemon list
    with open('AllPokemons.json', 'r', encoding='utf-8-sig') as f:
        all_pokemon_data = json.load(f)
    
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
    
    # Generate all possible unique patterns
    print("[INFO] Generating all possible unique patterns...")
    
    # For full 8-position patterns (high tiers)
    full_patterns = []
    for pattern in itertools.product(pokeballs, repeat=8):
        pattern_dict = {pos: ball for pos, ball in zip(positions, pattern)}
        full_patterns.append(pattern_dict)
    
    # For partial patterns (lower tiers) - 3 to 7 positions
    partial_patterns = []
    for num_positions in range(3, 8):
        for pos_combo in itertools.combinations(positions, num_positions):
            for ball_combo in itertools.product(pokeballs, repeat=num_positions):
                pattern_dict = {pos: ball for pos, ball in zip(pos_combo, ball_combo)}
                partial_patterns.append(pattern_dict)
    
    print(f"[INFO] Generated {len(full_patterns)} full patterns and {len(partial_patterns)} partial patterns")
    
    # Shuffle patterns to ensure randomness
    random.shuffle(full_patterns)
    random.shuffle(partial_patterns)
    
    # Create the final structure
    result = {}
    pattern_index = 0
    partial_index = 0
    
    # Process each tier from AllPokemons.json (excluding COMMON)
    tier_pattern_requirements = {
        'LEGENDARIES': 'full',
        'MYTHICALS': 'full', 
        'ULTRA_BEASTS': 'full',
        'PARADOX': 'full',
        'ULTRA_RARE': 'full',
        'RARE': 'partial',
        'UNCOMMON': 'partial'
    }
    
    for tier_name, pokemon_list in all_pokemon_data.items():
        if tier_name == 'COMMON':
            continue  # Skip COMMON tier
            
        print(f"[INFO] Processing {tier_name} with {len(pokemon_list)} Pokemon")
        result[tier_name] = {}
        
        pattern_type = tier_pattern_requirements.get(tier_name, 'partial')
        
        for pokemon in pokemon_list:
            if pattern_type == 'full':
                if pattern_index >= len(full_patterns):
                    print(f"[ERROR] Ran out of full patterns at {tier_name}:{pokemon}")
                    break
                result[tier_name][pokemon] = full_patterns[pattern_index]
                pattern_index += 1
            else:
                if partial_index >= len(partial_patterns):
                    print(f"[ERROR] Ran out of partial patterns at {tier_name}:{pokemon}")
                    break
                result[tier_name][pokemon] = partial_patterns[partial_index]
                partial_index += 1
    
    # Count total Pokemon
    total_pokemon = sum(len(tier_dict) for tier_dict in result.values())
    print(f"[SUCCESS] Generated unique patterns for {total_pokemon} Pokemon")
    
    # Verify uniqueness
    all_patterns = []
    for tier_dict in result.values():
        for pattern in tier_dict.values():
            pattern_str = '|'.join([pattern.get(pos, 'empty') for pos in positions])
            all_patterns.append(pattern_str)
    
    unique_patterns = set(all_patterns)
    print(f"[VERIFY] Total patterns: {len(all_patterns)}, Unique patterns: {len(unique_patterns)}")
    
    if len(all_patterns) == len(unique_patterns):
        print("[SUCCESS] All patterns are unique!")
    else:
        print(f"[ERROR] Found {len(all_patterns) - len(unique_patterns)} duplicate patterns")
        return None
    
    return result

def main():
    print("[START] Generating unique Pokemon patterns...")
    
    # Generate unique patterns
    unique_data = generate_unique_pokemon_patterns()
    
    if unique_data is None:
        print("[ERROR] Failed to generate unique patterns")
        return
    
    # Save to file
    output_file = 'src/main/resources/data/poke-notifier/pokemon_combinations.json'
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(unique_data, f, indent=4, ensure_ascii=False)
    
    print(f"[SAVE] Saved unique patterns to {output_file}")
    
    # Final verification
    print("[VERIFY] Running final duplicate check...")
    with open(output_file, 'r', encoding='utf-8') as f:
        verify_data = json.load(f)
    
    positions = ['north', 'east', 'south', 'west', 'northeast', 'southeast', 'southwest', 'northwest']
    all_patterns = []
    
    for tier, pokemon_dict in verify_data.items():
        for pokemon, pattern in pokemon_dict.items():
            pattern_str = '|'.join([pattern.get(pos, 'empty') for pos in positions])
            all_patterns.append(pattern_str)
    
    unique_patterns = set(all_patterns)
    total_pokemon = sum(len(tier_dict) for tier_dict in verify_data.values())
    
    print(f"[FINAL] Total Pokemon: {total_pokemon}")
    print(f"[FINAL] Total patterns: {len(all_patterns)}")
    print(f"[FINAL] Unique patterns: {len(unique_patterns)}")
    
    if len(all_patterns) == len(unique_patterns):
        print("[SUCCESS] All 532 Pokemon now have completely unique patterns!")
    else:
        print(f"[ERROR] Still have {len(all_patterns) - len(unique_patterns)} duplicates")

if __name__ == "__main__":
    main()