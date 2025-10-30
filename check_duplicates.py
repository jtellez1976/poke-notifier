import json
import itertools
import random

def load_pokemon_combinations():
    with open('src/main/resources/data/poke-notifier/pokemon_combinations.json', 'r') as f:
        return json.load(f)

def pattern_to_string(pattern):
    """Convert pattern dict to comparable string"""
    positions = ['north', 'east', 'south', 'west', 'northeast', 'southeast', 'southwest', 'northwest']
    return '|'.join([pattern.get(pos, 'empty') for pos in positions])

def find_duplicates(data):
    """Find all duplicate patterns"""
    pattern_to_pokemon = {}
    duplicates = []
    
    for tier, pokemon_dict in data.items():
        for pokemon, pattern in pokemon_dict.items():
            pattern_str = pattern_to_string(pattern)
            
            if pattern_str in pattern_to_pokemon:
                duplicates.append({
                    'pattern': pattern_str,
                    'pokemon1': pattern_to_pokemon[pattern_str],
                    'pokemon2': f"{tier}:{pokemon}"
                })
            else:
                pattern_to_pokemon[pattern_str] = f"{tier}:{pokemon}"
    
    return duplicates, pattern_to_pokemon

def generate_all_possible_patterns():
    """Generate all possible unique patterns"""
    pokeballs = [
        'poke_ball', 'great_ball', 'ultra_ball', 'master_ball', 'timer_ball',
        'dusk_ball', 'quick_ball', 'repeat_ball', 'luxury_ball', 'net_ball',
        'nest_ball', 'dive_ball', 'heal_ball', 'premier_ball', 'safari_ball',
        'sport_ball', 'park_ball', 'cherish_ball', 'gs_ball', 'beast_ball',
        'dream_ball', 'moon_ball', 'love_ball', 'friend_ball', 'lure_ball',
        'heavy_ball', 'level_ball', 'fast_ball'
    ]
    
    positions = ['north', 'east', 'south', 'west', 'northeast', 'southeast', 'southwest', 'northwest']
    
    # Generate all possible combinations for full patterns (8 positions)
    full_patterns = list(itertools.product(pokeballs, repeat=8))
    
    # Generate partial patterns (3-7 positions) for lower tiers
    partial_patterns = []
    for num_positions in range(3, 8):
        for pos_combo in itertools.combinations(positions, num_positions):
            for ball_combo in itertools.product(pokeballs, repeat=num_positions):
                pattern = {}
                for i, pos in enumerate(pos_combo):
                    pattern[pos] = ball_combo[i]
                partial_patterns.append(pattern)
    
    return full_patterns, partial_patterns, positions

def fix_duplicates(data):
    """Fix all duplicate patterns by assigning unique ones"""
    duplicates, used_patterns = find_duplicates(data)
    
    if not duplicates:
        print("[SUCCESS] No duplicates found!")
        return data
    
    print(f"[FOUND] Found {len(duplicates)} duplicate patterns:")
    for dup in duplicates:
        print(f"   - {dup['pokemon1']} == {dup['pokemon2']}")
        print(f"     Pattern: {dup['pattern']}")
    
    # Generate all possible patterns
    full_patterns, partial_patterns, positions = generate_all_possible_patterns()
    pokeballs = [
        'poke_ball', 'great_ball', 'ultra_ball', 'master_ball', 'timer_ball',
        'dusk_ball', 'quick_ball', 'repeat_ball', 'luxury_ball', 'net_ball',
        'nest_ball', 'dive_ball', 'heal_ball', 'premier_ball', 'safari_ball',
        'sport_ball', 'park_ball', 'cherish_ball', 'gs_ball', 'beast_ball',
        'dream_ball', 'moon_ball', 'love_ball', 'friend_ball', 'lure_ball',
        'heavy_ball', 'level_ball', 'fast_ball'
    ]
    
    # Convert used patterns to set for quick lookup
    used_pattern_strings = set(used_patterns.keys())
    
    # Fix duplicates
    fixed_count = 0
    for tier, pokemon_dict in data.items():
        for pokemon, pattern in pokemon_dict.items():
            pattern_str = pattern_to_string(pattern)
            
            # Count how many times this pattern is used
            usage_count = sum(1 for p in used_patterns.values() if pattern_str == pattern_to_string(data[p.split(':')[0]][p.split(':')[1]]))
            
            if usage_count > 1:  # This is a duplicate
                # Generate new unique pattern
                new_pattern = None
                attempts = 0
                max_attempts = 10000
                
                while attempts < max_attempts:
                    if tier in ['LEGENDARIES', 'MYTHICALS', 'ULTRA_BEASTS']:
                        # Full 8-position patterns for high tiers
                        balls = random.choices(pokeballs, k=8)
                        new_pattern = {pos: balls[i] for i, pos in enumerate(positions)}
                    else:
                        # Partial patterns for lower tiers
                        num_positions = random.randint(3, 7)
                        selected_positions = random.sample(positions, num_positions)
                        selected_balls = random.choices(pokeballs, k=num_positions)
                        new_pattern = {pos: ball for pos, ball in zip(selected_positions, selected_balls)}
                    
                    new_pattern_str = pattern_to_string(new_pattern)
                    
                    if new_pattern_str not in used_pattern_strings:
                        # Found unique pattern
                        data[tier][pokemon] = new_pattern
                        used_pattern_strings.add(new_pattern_str)
                        used_patterns[new_pattern_str] = f"{tier}:{pokemon}"
                        fixed_count += 1
                        print(f"[FIXED] Fixed {tier}:{pokemon} with new unique pattern")
                        break
                    
                    attempts += 1
                
                if attempts >= max_attempts:
                    print(f"[ERROR] Could not find unique pattern for {tier}:{pokemon} after {max_attempts} attempts")
    
    print(f"[FIXED] Fixed {fixed_count} duplicate patterns")
    return data

def main():
    print("[CHECK] Checking for duplicate patterns in pokemon_combinations.json...")
    
    # Load current data
    data = load_pokemon_combinations()
    
    # Find and fix duplicates
    fixed_data = fix_duplicates(data)
    
    # Verify no duplicates remain
    final_duplicates, _ = find_duplicates(fixed_data)
    
    if final_duplicates:
        print(f"[ERROR] Still have {len(final_duplicates)} duplicates after fixing!")
        for dup in final_duplicates:
            print(f"   - {dup['pokemon1']} == {dup['pokemon2']}")
    else:
        print("[SUCCESS] All patterns are now unique!")
        
        # Save fixed data
        with open('src/main/resources/data/poke-notifier/pokemon_combinations.json', 'w') as f:
            json.dump(fixed_data, f, indent=4)
        
        print("[SAVE] Saved fixed pokemon_combinations.json")
        
        # Count total Pokemon
        total = sum(len(pokemon_dict) for pokemon_dict in fixed_data.values())
        print(f"[STATS] Total Pokemon with unique patterns: {total}")

if __name__ == "__main__":
    main()