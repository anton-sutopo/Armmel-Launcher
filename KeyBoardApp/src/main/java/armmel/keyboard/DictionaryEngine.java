package armmel.keyboard;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

final class DictionaryEngine {

  private static final String TAG = "DictionaryEngine";
  private static final String[] DICTIONARY_ASSETS = {"dictionary_en.txt", "dictionary_id.txt"};

  private final List<String> mWords;

  DictionaryEngine(Context context) {
    mWords = loadWords(context.getAssets());
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
      return Collections.emptyList();
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

    if (matches.size() > limit) {
      return new ArrayList<String>(matches.subList(0, limit));
    }
    return matches;
  }

  private List<String> loadWords(AssetManager assetManager) {
    TreeSet<String> mergedWords = new TreeSet<String>();
    for (String assetName : DICTIONARY_ASSETS) {
      try {
        loadAssetWords(assetManager, assetName, mergedWords);
      } catch (IOException exception) {
        Log.e(TAG, "Unable to load dictionary asset " + assetName, exception);
      }
    }
    return new ArrayList<String>(mergedWords);
  }

  private void loadAssetWords(AssetManager assetManager, String assetName, TreeSet<String> words)
      throws IOException {
    try (InputStream stream = assetManager.open(assetName);
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String normalized = normalize(line);
        if (normalized.isEmpty()) {
          continue;
        }
        words.add(normalized);
      }
    }
  }

  private static String normalize(String rawWord) {
    return rawWord == null ? "" : rawWord.trim().toLowerCase(Locale.ROOT);
  }
}
