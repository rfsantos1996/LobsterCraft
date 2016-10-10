import math


def square(i):
    return i * i


def num_of_chunks(search_range):
    return square((search_range * 2) + 1)


def min_chunk_range(block_dist):
    return math.ceil(block_dist / 16.0)


def level_scaling(initial, per_level):
    last_value = 0
    for level in range(1, max_level + 1):
        current_value = initial + math.ceil(per_level * (level - 1))
        print('Level ', level, ': ', current_value, ' (', (current_value - last_value), ')')
        last_value = current_value


def get_scaling_value(initial_range, final_value):
    scaling_value = round((final_value - initial_range) / (max_level - 1), 2)
    print('scaling_value: ', scaling_value)
    return scaling_value


def get_protection_range_xz(floor_side_length, message=''):
    print('protection_range ', message, ' (x, z) = ', round(math.sqrt(2) * (floor_side_length / 2), 5))


def get_protection_range_xyz(floor_side_length, height_from_ground=5, message=''):
    # Center will be a block above the floor
    # House height will be from 5, from the center, it'll be 4
    # maximum distance will be on the top corner (c.o. = 4, c.a. = floor_side_length/2 * sqrt(2), hip = protection_range)
    print('protection_range ', message, ' (x, y, z) = ', round(math.sqrt(math.pow(height_from_ground - 1, 2) + (math.pow(floor_side_length, 2) / 2)), 5))


max_level = 10
level_scaling(60, get_scaling_value(60, 139))  # used for city protection
get_protection_range_xyz(24, 5 + 4 * 2, 'house')
get_protection_range_xz(24, 'house')
get_protection_range_xyz(16, 16, 'player')
get_protection_range_xz(84, 'city')  # the entire city on the first level (3.5 houses side by side)
get_protection_range_xz(240, 'administrator')  # administrator protection
print(2 << 5)
