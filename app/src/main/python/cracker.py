# This is a simplified implementation of a dictionary attack.
# It is not a wrapper for hashcat.
import hashlib

def dictionary_attack(hash_to_crack, wordlist_path, hash_algorithm):
    try:
        with open(wordlist_path, 'r', encoding='utf-8', errors='ignore') as wordlist:
            for word in wordlist:
                word = word.strip()
                hasher = hashlib.new(hash_algorithm)
                hasher.update(word.encode('utf-8'))
                hashed_word = hasher.hexdigest()
                if hashed_word == hash_to_crack:
                    return word
    except FileNotFoundError:
        return None
    return None
