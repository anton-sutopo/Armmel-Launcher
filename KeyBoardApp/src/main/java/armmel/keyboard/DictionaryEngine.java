package armmel.keyboard;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

final class DictionaryEngine {

  private static final String TAG = "DictionaryEngine";
  private static final String DICTIONARY_ASSET = "dictionary.bin";
  private static final byte[] DICTIONARY_MAGIC = {
    (byte) 'A', (byte) 'K', (byte) 'D', (byte) '1'
  };
  private static final int DICTIONARY_FORMAT_VERSION = 1;

  private final List<String> mWords;
  private final SuggestionRankingRepository mSuggestionRankingRepository;

  DictionaryEngine(Context context, SuggestionRankingRepository suggestionRankingRepository) {
    mWords = loadWords(context.getAssets());
    mSuggestionRankingRepository = suggestionRankingRepository;
  }

  List<String> getSuggestions(String prefix, int limit) {
    if (limit <= 0) {
      return Collections.emptyList();
    }

    String normalizedPrefix = normalize(prefix);
    if (normalizedPrefix.isEmpty()) {
      return Collections.emptyList();
    }

    int startIndex = Collections.binarySearch(mWords, normalizedPrefix);
    if (startIndex < 0) {
      startIndex = -startIndex - 1;
    }

    List<String> matches = new ArrayList<String>(limit);
    for (int i = startIndex; i < mWords.size() && matches.size() < limit * 3; i++) {
      String word = mWords.get(i);
      if (!word.startsWith(normalizedPrefix)) {
        break;
      }
      matches.add(word);
    }

    if (matches.isEmpty()) {
      return getRankedOnlySuggestions(normalizedPrefix, limit);
    }

    Collections.sort(
        matches,
        new Comparator<String>() {
          @Override
          public int compare(String left, String right) {
            boolean leftExact = left.equals(normalizedPrefix);
            boolean rightExact = right.equals(normalizedPrefix);
            if (leftExact != rightExact) {
              return leftExact ? -1 : 1;
            }

            int byLength = Integer.compare(left.length(), right.length());
            if (byLength != 0) {
              return byLength;
            }
            return left.compareTo(right);
          }
        });

    LinkedHashSet<String> orderedSuggestions = new LinkedHashSet<String>(limit * 2);
    orderedSuggestions.addAll(mSuggestionRankingRepository.getRankedWords(normalizedPrefix, limit));
    orderedSuggestions.addAll(matches);

    if (orderedSuggestions.isEmpty()) {
      return Collections.emptyList();
    }

    List<String> suggestions = new ArrayList<String>(limit);
    for (String suggestion : orderedSuggestions) {
      suggestions.add(suggestion);
      if (suggestions.size() == limit) {
        break;
      }
    }
    return suggestions;
  }

  private List<String> loadWords(AssetManager assetManager) {
    try (InputStream stream = assetManager.open(DICTIONARY_ASSET)) {
      return loadBinaryWords(stream);
    } catch (IOException exception) {
      Log.e(TAG, "Unable to load dictionary asset " + DICTIONARY_ASSET, exception);
      return Collections.emptyList();
    }
  }

  private List<String> loadBinaryWords(InputStream stream)
      throws IOException {
    verifyMagic(stream);

    int formatVersion = readVarInt(stream);
    if (formatVersion != DICTIONARY_FORMAT_VERSION) {
      throw new IOException("Unsupported dictionary format version " + formatVersion);
    }

    int wordCount = readVarInt(stream);
    List<String> words = new ArrayList<String>(wordCount);
    String previousWord = "";
    for (int i = 0; i < wordCount; i++) {
      int prefixLength = readVarInt(stream);
      if (prefixLength > previousWord.length()) {
        throw new IOException("Invalid prefix length " + prefixLength + " for word index " + i);
      }

      int suffixLength = readVarInt(stream);
      byte[] suffixBytes = readBytes(stream, suffixLength);
      String word =
          previousWord.substring(0, prefixLength)
              + new String(suffixBytes, StandardCharsets.UTF_8);
      words.add(word);
      previousWord = word;
    }
    return words;
  }

  private void verifyMagic(InputStream stream) throws IOException {
    byte[] actualMagic = readBytes(stream, DICTIONARY_MAGIC.length);
    for (int i = 0; i < DICTIONARY_MAGIC.length; i++) {
      if (actualMagic[i] != DICTIONARY_MAGIC[i]) {
        throw new IOException("Unsupported dictionary header");
      }
    }
  }

  private int readVarInt(InputStream stream) throws IOException {
    int value = 0;
    int shift = 0;
    while (shift < 32) {
      int currentByte = stream.read();
      if (currentByte < 0) {
        throw new IOException("Unexpected end of dictionary asset");
      }
      value |= (currentByte & 0x7F) << shift;
      if ((currentByte & 0x80) == 0) {
        return value;
      }
      shift += 7;
    }
    throw new IOException("Dictionary varint is too large");
  }

  private byte[] readBytes(InputStream stream, int length) throws IOException {
    byte[] buffer = new byte[length];
    int offset = 0;
    while (offset < length) {
      int bytesRead = stream.read(buffer, offset, length - offset);
      if (bytesRead < 0) {
        throw new IOException("Unexpected end of dictionary asset");
      }
      offset += bytesRead;
    }
    return buffer;
  }

  private static String normalize(String rawWord) {
    return rawWord == null ? "" : rawWord.trim().toLowerCase(Locale.ROOT);
  }

  private List<String> getRankedOnlySuggestions(String normalizedPrefix, int limit) {
    List<String> rankedSuggestions =
        mSuggestionRankingRepository.getRankedWords(normalizedPrefix, limit);
    if (rankedSuggestions.isEmpty()) {
      return Collections.emptyList();
    }
    return rankedSuggestions;
  }
}
