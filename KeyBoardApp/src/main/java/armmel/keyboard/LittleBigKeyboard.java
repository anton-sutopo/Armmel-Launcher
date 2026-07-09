/*
 * Copyright (C) 2011 Thomas Lundqvist
 * Copyright (C) 2008-2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package armmel.keyboard;

import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Input method for a soft keyboard. Based on the android sample code: softkeyboard. */
public class LittleBigKeyboard extends InputMethodService
    implements ModKeyboardView.OnKeyboardActionListener {

  static final boolean DEBUG = false;
  //    static final boolean DEBUG = true;
  private static final int MAX_DICTIONARY_SUGGESTIONS = 8;
  private static final List<String> FALLBACK_EMOJIS =
      Collections.unmodifiableList(
          Arrays.asList(
              "😀",
              "😃",
              "😄",
              "😁",
              "😆",
              "😅",
              "😂",
              "🤣",
              "🙂",
              "😊",
              "😉",
              "😍",
              "😘",
              "🥰",
              "😎",
              "🤗",
              "🤔",
              "🤭",
              "😭",
              "😡",
              "🥹",
              "😴",
              "🤩",
              "😇",
              "👍",
              "👎",
              "👌",
              "✌️",
              "🤞",
              "🤟",
              "👏",
              "🙌",
              "🙏",
              "💪",
              "👋",
              "🤝",
              "❤️",
              "🩷",
              "🧡",
              "💛",
              "💚",
              "💙",
              "💜",
              "🖤",
              "🤍",
              "🤎",
              "💔",
              "💕",
              "💞",
              "💯",
              "🔥",
              "✨",
              "⭐",
              "🌟",
              "🎉",
              "🎊",
              "✅",
              "❌",
              "⚠️",
              "🚀",
              "📌",
              "💡",
              "📷",
              "📱",
              "💻",
              "⌚",
              "🎵",
              "🎮",
              "🏆",
              "⚽",
              "🏀",
              "🍎",
              "🍕",
              "🍔",
              "🍟",
              "🌮",
              "🍜",
              "☕",
              "🧋",
              "🍰",
              "🍩",
              "🐱",
              "🐶",
              "🐼",
              "🐸",
              "🦊",
              "🐥",
              "🌸",
              "🌼",
              "🌈",
              "☀️",
              "🌙",
              "⭐",
              "⛄",
              "🌊",
              "🎁",
              "🛒",
              "🏠",
              "🚗",
              "✈️"));

  private ModKeyboardView mInputView;
  private CandidateView mCandidateView;
  private DictionaryEngine mDictionaryEngine;
  private SuggestionRankingRepository mSuggestionRankingRepository;

  // Composing is only used for dead key accent combinations (maximum length 1)
  private char mDeadKeyComposing;
  private final StringBuilder mComposingWord = new StringBuilder();
  private boolean mPredictionOn;
  private boolean mCapsLock;
  private long mLastShiftTime;
  private boolean mJawaMode;
  private boolean mShowingFallbackEmoji;
  private List<String> mDictionarySuggestions = Collections.emptyList();

  private Keyboard mAlphaKeyboard;
  private Keyboard mAlphaShiftedKeyboard;
  private Keyboard mNumeralKeyboard;
  private Keyboard mNumeralShiftedKeyboard;

  private Keyboard mJawaKeyboard;
  private Keyboard mJawaShiftedKeyboard;
  private Keyboard mJawaNumeralKeyboard;
  private Keyboard mJawaNumeralShiftedKeyboard;

  private Keyboard mCurKeyboard;

  private static final String TAG = "LittleBigKeyboard";

  /** Main initialization of the input method component. Be sure to call to super class. */
  @Override
  public void onCreate() {
    super.onCreate();
    // Use the following line to debug IME service.
    if (DEBUG) {
      android.os.Debug.waitForDebugger();
      Log.d(TAG, "onCreate()");
    }
  }

  /**
   * This is the point where you can do all of your UI initialization. It is called after creation
   * and any configuration change.
   */
  private int mLastDisplayWidth;

  @Override
  public void onInitializeInterface() {
    int displayWidth = getMaxWidth();
    if (mAlphaKeyboard != null) {
      // Configuration changes can happen after the keyboard gets recreated,
      // so we avoid re-building the keyboards if the available
      // space hasn't changed.
      if (displayWidth == mLastDisplayWidth) return;
    }
    mLastDisplayWidth = displayWidth;

    mAlphaKeyboard = new Keyboard(this, R.xml.alpha);
    mAlphaShiftedKeyboard = new Keyboard(this, R.xml.alpha_shifted);
    mNumeralKeyboard = new Keyboard(this, R.xml.numeral);
    mNumeralShiftedKeyboard = new Keyboard(this, R.xml.numeral_shifted);
    mJawaKeyboard = new Keyboard(this, R.xml.jawa);
    mJawaShiftedKeyboard = new Keyboard(this, R.xml.jawa_shifted);
    mJawaNumeralKeyboard = new Keyboard(this, R.xml.jawa_numeral);
    mJawaNumeralShiftedKeyboard = new Keyboard(this, R.xml.jawa_numeral_shifted);
  }

  private boolean isDark() {
    int currentNightMode =
        this.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    switch (currentNightMode) {
      case Configuration.UI_MODE_NIGHT_YES:
        return true;
    }
    return false;
  }

  /**
   * Called by the framework when your view for creating input needs to be generated. This will be
   * called the first time your input method is displayed, and every time it needs to be re-created
   * such as due to a configuration change.
   */
  @Override
  public View onCreateInputView() {
    if (DEBUG) {
      Log.d(TAG, "onCreateInputView()");
    }
    View rootView = getLayoutInflater().inflate(R.layout.input, null);
    FrameLayout candidatesContainer = (FrameLayout) rootView.findViewById(R.id.candidates_container);
    if (mCandidateView == null) {
      mCandidateView = new CandidateView(this);
      mCandidateView.setService(this);
    } else {
      ViewGroup currentParent = (ViewGroup) mCandidateView.getParent();
      if (currentParent != null) {
        currentParent.removeView(mCandidateView);
      }
    }
    candidatesContainer.addView(mCandidateView);
    updateCandidateVisibility(false);
    mInputView = (ModKeyboardView) rootView.findViewById(R.id.keyboard);
    mInputView.setTheme(isDark());
    mInputView.setOnKeyboardActionListener(this);
    
    // Set back disposition to ensure keyboard appears above navigation bar
    setBackDisposition(BACK_DISPOSITION_DEFAULT);
    
    return rootView;
  }

  /**
   * Called by the framework when your view for showing candidates needs to be generated, like
   * {@link #onCreateInputView}.
   */
  @Override
  public View onCreateCandidatesView() {
    return null;
  }

  /**
   * This is the main point where we do our initialization of the input method to begin operating on
   * an application. At this point we have been bound to the client, and are now receiving all of
   * the detailed information about the target of our edits.
   */
  @Override
  public void onStartInput(EditorInfo attribute, boolean restarting) {
    if (DEBUG) {
      Log.d(TAG, "onStartInput()");
    }
    super.onStartInput(attribute, restarting);

    // Reset our state.  We want to do this even if restarting, because
    // the underlying state of the text editor could have changed in any way.
    mDeadKeyComposing = 0;
    clearComposingWord();

    mPredictionOn = false;

    // We are now going to initialize our state based on the type of
    // text being edited.
    switch (attribute.inputType & EditorInfo.TYPE_MASK_CLASS) {
      case EditorInfo.TYPE_CLASS_NUMBER:
      case EditorInfo.TYPE_CLASS_DATETIME:
        // Numbers and dates default to the symbols keyboard, with
        // no extra features.
        mCurKeyboard = mNumeralKeyboard;
        break;

      case EditorInfo.TYPE_CLASS_PHONE:
        // Phones will also default to the symbols keyboard, though
        // often you will want to have a dedicated phone keyboard.
        mCurKeyboard = mNumeralKeyboard;
        break;

      case EditorInfo.TYPE_CLASS_TEXT:
        // This is general text editing.  We will default to the
        // normal alphabetic keyboard, and assume that we should
        // be doing predictive text (showing candidates as the
        // user types).
        mCurKeyboard = mJawaMode ? mJawaKeyboard : mAlphaKeyboard;
        mPredictionOn = isPredictionEnabled(attribute);
        break;

      default:
        // For all unknown input types, default to the alphabetic
        // keyboard with no special features.
        mCurKeyboard = mJawaMode ? mJawaKeyboard : mAlphaKeyboard;
    }
  }

  /** This is called when the user is done editing a field. We can use this to reset our state. */
  @Override
  public void onFinishInput() {
    super.onFinishInput();

    // Clear current composing text
    mDeadKeyComposing = 0;
    clearComposingWord();

    // We only hide the candidates window when finishing input on
    // a particular editor, to avoid popping the underlying application
    // up and down if the user is entering text into the bottom of
    // its window.
    updateCandidateVisibility(false);

    mCurKeyboard = mJawaMode ? mJawaKeyboard : mAlphaKeyboard;
  }

  @Override
  public void onStartInputView(EditorInfo attribute, boolean restarting) {
    super.onStartInputView(attribute, restarting);
    // Apply the selected keyboard to the input view.
    mInputView.setKeyboard(mCurKeyboard);
    clearDictionarySuggestions();
    updateShiftKeyState();
  }

  /**
   * Deal with the editor reporting movement of its cursor. Note: start can be larger than end
   * (backwards)
   */
  @Override
  public void onUpdateSelection(
      int oldSelStart,
      int oldSelEnd,
      int newSelStart,
      int newSelEnd,
      int candidatesStart,
      int candidatesEnd) {
    super.onUpdateSelection(
        oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);

    if (DEBUG) {
      Log.d(TAG, "onUpdateSelection(" + newSelStart + "," + newSelEnd + ")");
      InputConnection ic = getCurrentInputConnection();
      if (ic != null) {
        String text = "";
        ExtractedTextRequest etr = new ExtractedTextRequest();
        etr.hintMaxChars = 200;
        //	        	etr.hintMaxLines = 100;
        ExtractedText et = ic.getExtractedText(etr, 0);
        text = et.text.toString();
        Log.d(TAG, "Extracted text=" + text + "\nstartOffset=" + et.startOffset);
      }
    }

    // If the current selection in the text view changes, we should
    // clear whatever candidate text we have.
    if (mDeadKeyComposing != 0 && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
      mDeadKeyComposing = 0;
      InputConnection ic = getCurrentInputConnection();
      if (ic != null) {
        ic.finishComposingText();
      }
    }
    if (mComposingWord.length() > 0 && !cursorStillAtComposingWord(newSelStart, newSelEnd)) {
      clearComposingWord();
    }
    updateShiftKeyState();
  }

  @Override
  public void onDisplayCompletions(android.view.inputmethod.CompletionInfo[] completions) {}

  /** Helper function to commit any text being composed in to the editor. */
  private void commitTyped(InputConnection inputConnection) {
    if (mDeadKeyComposing != 0) {
      inputConnection.commitText(Character.toString(mDeadKeyComposing), 1);
      mDeadKeyComposing = 0;
    }
  }

  /** Helper to update the shift state of our keyboard based on the initial editor state. */
  private void updateShiftKeyState() {
    EditorInfo attr = getCurrentInputEditorInfo();
    if (attr != null && mInputView != null) {
      int caps = 0;
      if (!mJawaMode
          && (mAlphaKeyboard == mInputView.getKeyboard()
              || mAlphaShiftedKeyboard == mInputView.getKeyboard())) {
        EditorInfo ei = getCurrentInputEditorInfo();
        InputConnection ic = getCurrentInputConnection();
        if (ei != null && ei.inputType != EditorInfo.TYPE_NULL && ic != null) {
          caps = ic.getCursorCapsMode(attr.inputType);
        }
      }
      setShifted(mCapsLock || caps != 0);
    }
  }

  private void setShifted(boolean shifted) {
    Keyboard currentKeyboard = mInputView.getKeyboard();
    if (mJawaMode) {
      setShiftedJawa(shifted, currentKeyboard);
    } else {
      setShiftedAlpha(shifted, currentKeyboard);
    }
  }

  private void setShiftedJawa(boolean shifted, Keyboard currentKeyboard) {
    if (shifted) {
      if (currentKeyboard == mJawaKeyboard) mInputView.setKeyboard(mJawaShiftedKeyboard);
      else if (currentKeyboard == mJawaNumeralKeyboard)
        mInputView.setKeyboard(mJawaNumeralShiftedKeyboard);
    } else {
      if (currentKeyboard == mJawaShiftedKeyboard) mInputView.setKeyboard(mJawaKeyboard);
      else if (currentKeyboard == mJawaShiftedKeyboard) mInputView.setKeyboard(mJawaKeyboard);
    }
    mInputView.setShifted(mCapsLock, shifted, mJawaMode);
    boolean nummode =
        currentKeyboard == mJawaNumeralKeyboard || currentKeyboard == mJawaNumeralShiftedKeyboard;
    mInputView.setNumMode(nummode);
  }

  private void setShiftedAlpha(boolean shifted, Keyboard currentKeyboard) {
    if (shifted) {
      if (currentKeyboard == mAlphaKeyboard) mInputView.setKeyboard(mAlphaShiftedKeyboard);
      else if (currentKeyboard == mNumeralKeyboard) mInputView.setKeyboard(mNumeralShiftedKeyboard);
    } else {
      if (currentKeyboard == mAlphaShiftedKeyboard) mInputView.setKeyboard(mAlphaKeyboard);
      else if (currentKeyboard == mNumeralShiftedKeyboard) mInputView.setKeyboard(mNumeralKeyboard);
    }
    mInputView.setShifted(mCapsLock, shifted, mJawaMode);
    boolean nummode =
        currentKeyboard == mNumeralKeyboard || currentKeyboard == mNumeralShiftedKeyboard;
    mInputView.setNumMode(nummode);
  }

  // Implementation of KeyboardViewListener

  public void onKey(int primaryCode) {
    if (mInputView == null) return;

    if (primaryCode == Keyboard.KEYCODE_DELETE) {
      handleBackspace();
    } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
      handleShift();
    } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
      handleClose();
    } else if (primaryCode == Keyboard.KEYCODE_OPTIONS) {
      // Show a menu or somethin'
    } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE) {
      handleModeChange();
    } else if (primaryCode == Keyboard.KEYCODE_JAVA) {
      handleJawaModeChange();
    } else if (primaryCode == Keyboard.KEYCODE_LEFT) {
      clearComposingWord();
      sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT);
    } else if (primaryCode == Keyboard.KEYCODE_RIGHT) {
      clearComposingWord();
      sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT);
    } else if (primaryCode == Keyboard.KEYCODE_END) {
      clearComposingWord();
      for (int i = 0; i < 5; i++) {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT);
      }
    } else if (primaryCode == Keyboard.KEYCODE_HOME) {
      clearComposingWord();
      getCurrentInputConnection().setSelection(0, 0);
    } else if (primaryCode == Keyboard.KEYCODE_DEAD_ACUTE) {
      handleDeadKey('\u00b4');
    } else if (primaryCode == Keyboard.KEYCODE_DEAD_GRAVE) {
      handleDeadKey('`');
    } else if (primaryCode == Keyboard.KEYCODE_DEAD_DIARESIS) {
      handleDeadKey('\u00a8');
    } else if (primaryCode == Keyboard.KEYCODE_DEAD_CIRCUMFLEX) {
      handleDeadKey('^');
    } else if (primaryCode == Keyboard.KEYCODE_DEAD_TILDE) {
      handleDeadKey('~');
    } else {
      // normal character or separator
      if (mDeadKeyComposing != 0) {
        // Compose with previous accent
        int r = (char) KeyEvent.getDeadChar(mDeadKeyComposing, primaryCode);
        if (r != 0) mDeadKeyComposing = (char) r;
        final String committed = Character.toString(mDeadKeyComposing);
        commitTyped(getCurrentInputConnection());
        handleCommittedText(committed);
      } else {
        if (primaryCode != 10) {
          final InputConnection ic = getCurrentInputConnection();
          final String text = new String(new int[] {primaryCode}, 0, 1);
          ic.commitText(text, text.length());
          handleCommittedText(text);
        } else {
          clearComposingWord();
          sendKeyChar((char) primaryCode);
        }
      }
      updateShiftKeyState();
    }
  }

  public void onText(CharSequence text) {
    InputConnection ic = getCurrentInputConnection();
    if (ic == null) return;
    ic.beginBatchEdit();
    ic.commitText(text, 0);
    ic.endBatchEdit();
    handleCommittedText(text);
    updateShiftKeyState();
  }

  public void setSuggestions(
      List<String> suggestions, boolean completions, boolean typedWordValid) {
    if (mCandidateView != null) {
      mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
      updateCandidateVisibility(suggestions != null && !suggestions.isEmpty());
    }
  }

  private void handleBackspace() {
    if (mDeadKeyComposing != 0) {
      mDeadKeyComposing = 0;
      getCurrentInputConnection().setComposingText("", 0);
    } else {
      sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
      syncComposingWordWithEditor();
      if (mComposingWord.length() > 0) {
        updateDictionarySuggestions();
      }
    }
    updateShiftKeyState();
  }

  private void handleShift() {
    checkToggleCapsLock();
    boolean shifted = mCapsLock || !mInputView.isShifted();
    setShifted(shifted);
  }

  private void handleJawaModeChange() {
    clearComposingWord();
    Keyboard current = mAlphaKeyboard;
    if (!mJawaMode) {
      mJawaMode = true;
      current = mJawaKeyboard;
    } else {
      mJawaMode = false;
    }
    mInputView.setKeyboard(current);
    current.setJawa(mJawaMode);
  }

  private void handleModeChange() {
    clearComposingWord();
    Keyboard current = mInputView.getKeyboard();
    if (mJawaMode) {
      handleModeChangeJawa(current);
    } else {
      handleModeChangeAlpha(current);
    }
  }

  private void handleModeChangeJawa(Keyboard current) {
    if (current == mJawaNumeralKeyboard || current == mJawaNumeralShiftedKeyboard) {
      current = mJawaKeyboard;
    } else {
      current = mJawaNumeralKeyboard;
    }
    mInputView.setKeyboard(current);
    mCapsLock = false;
    setShifted(false);
    updateShiftKeyState();
  }

  private void handleModeChangeAlpha(Keyboard current) {
    if (current == mNumeralKeyboard || current == mNumeralShiftedKeyboard) {
      current = mAlphaKeyboard;
    } else {
      current = mNumeralKeyboard;
    }
    mInputView.setKeyboard(current);
    mCapsLock = false;
    setShifted(false);
    updateShiftKeyState();
  }

  private void handleClose() {
    commitTyped(getCurrentInputConnection());
    clearComposingWord();
    requestHideSelf(0);
  }

  private void checkToggleCapsLock() {
    long now = System.currentTimeMillis();
    if (mLastShiftTime + 600 > now) {
      mCapsLock = true;
      mLastShiftTime = now;
    } else {
      mLastShiftTime = now;
      mCapsLock = false;
    }
  }

  private void handleDeadKey(char accent) {
    mDeadKeyComposing = accent;
    getCurrentInputConnection().setComposingText(Character.toString(mDeadKeyComposing), 1);
    updateShiftKeyState();
  }

  //    public void pickDefaultCandidate() {
  //        pickSuggestionManually(0);
  //    }

  public void pickSuggestionManually(int index) {
    if (mShowingFallbackEmoji) {
      InputConnection inputConnection = getCurrentInputConnection();
      List<String> fallbackEmojis = getFallbackEmojis();
      if (inputConnection != null && index >= 0 && index < fallbackEmojis.size()) {
        String emoji = fallbackEmojis.get(index);
        inputConnection.commitText(emoji, 1);
        getSuggestionRankingRepository().recordEmojiPick(emoji);
      }
      clearComposingWord();
      updateShiftKeyState();
      return;
    }

    if (index >= 0 && index < mDictionarySuggestions.size() && mComposingWord.length() > 0) {
      String suggestion = mDictionarySuggestions.get(index);
      String committedSuggestion = formatSuggestionForCommit(suggestion);
      InputConnection inputConnection = getCurrentInputConnection();
      if (inputConnection != null) {
        inputConnection.beginBatchEdit();
        inputConnection.deleteSurroundingText(mComposingWord.length(), 0);
        inputConnection.commitText(committedSuggestion, 1);
        inputConnection.endBatchEdit();
        getSuggestionRankingRepository().recordWordPick(suggestion);
        mComposingWord.setLength(0);
        mComposingWord.append(suggestion);
        updateDictionarySuggestions();
      }
      updateShiftKeyState();
    }
  }

  public void onPress(int primaryCode) {}

  public void onRelease(int primaryCode) {}

  private boolean isPredictionEnabled(EditorInfo attribute) {
    if ((attribute.inputType & EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) {
      return false;
    }

    switch (attribute.inputType & EditorInfo.TYPE_MASK_VARIATION) {
      case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
      case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_SUBJECT:
      case EditorInfo.TYPE_TEXT_VARIATION_FILTER:
      case EditorInfo.TYPE_TEXT_VARIATION_PASSWORD:
      case EditorInfo.TYPE_TEXT_VARIATION_URI:
      case EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
      case EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS:
      case EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD:
        return false;
      default:
        return true;
    }
  }

  private void handleCommittedText(CharSequence text) {
    if (!mPredictionOn || mJawaMode) {
      clearComposingWord();
      return;
    }

    if (text == null || text.length() == 0) {
      clearDictionarySuggestions();
      return;
    }

    String normalizedText = text.toString().toLowerCase(Locale.ROOT);
    if (isWordFragment(normalizedText)) {
      syncComposingWordWithEditor();
      if (mComposingWord.length() == 0) {
        mComposingWord.append(normalizedText);
      }
      updateDictionarySuggestions();
      return;
    }

    clearComposingWord();
  }

  private boolean isWordFragment(String text) {
    for (int i = 0; i < text.length(); i++) {
      char current = text.charAt(i);
      if (!Character.isLetter(current) && current != '\'' && current != '-') {
        return false;
      }
    }
    return true;
  }

  private void syncComposingWordWithEditor() {
    String wordFragment = getWordFragmentBeforeCursor();
    mComposingWord.setLength(0);
    mComposingWord.append(wordFragment);
    if (mComposingWord.length() == 0) {
      clearDictionarySuggestions();
    }
  }

  private void updateDictionarySuggestions() {
    if (mComposingWord.length() < 2) {
      showFallbackEmoji();
      return;
    }

    ensureDictionaryEngine();
    mDictionarySuggestions =
        mDictionaryEngine.getSuggestions(mComposingWord.toString(), MAX_DICTIONARY_SUGGESTIONS);
    if (mDictionarySuggestions.isEmpty()) {
      showFallbackEmoji();
      return;
    }
    mShowingFallbackEmoji = false;
    setSuggestions(mDictionarySuggestions, false, true);
  }

  private void clearDictionarySuggestions() {
    if (shouldShowFallbackEmoji()) {
      showFallbackEmoji();
      return;
    }
    hideSuggestions();
  }

  private void showFallbackEmoji() {
    mShowingFallbackEmoji = true;
    mDictionarySuggestions = Collections.emptyList();
    setSuggestions(getFallbackEmojis(), false, true);
  }

  private void hideSuggestions() {
    mShowingFallbackEmoji = false;
    mDictionarySuggestions = Collections.emptyList();
    setSuggestions(null, false, false);
  }

  private void clearComposingWord() {
    mComposingWord.setLength(0);
    clearDictionarySuggestions();
  }

  private void ensureDictionaryEngine() {
    if (mDictionaryEngine == null) {
      mDictionaryEngine = new DictionaryEngine(this, getSuggestionRankingRepository());
    }
  }

  private SuggestionRankingRepository getSuggestionRankingRepository() {
    if (mSuggestionRankingRepository == null) {
      mSuggestionRankingRepository = new SuggestionRankingRepository(this);
    }
    return mSuggestionRankingRepository;
  }

  private List<String> getFallbackEmojis() {
    List<String> rankedEmojis =
        getSuggestionRankingRepository().getRankedEmojis(FALLBACK_EMOJIS.size());
    if (rankedEmojis.isEmpty()) {
      return FALLBACK_EMOJIS;
    }

    LinkedHashSet<String> orderedEmojis = new LinkedHashSet<String>(FALLBACK_EMOJIS.size());
    orderedEmojis.addAll(rankedEmojis);
    orderedEmojis.addAll(FALLBACK_EMOJIS);
    return new ArrayList<String>(orderedEmojis);
  }

  private boolean shouldShowFallbackEmoji() {
    if (mJawaMode) {
      return false;
    }

    EditorInfo attribute = getCurrentInputEditorInfo();
    if (attribute == null) {
      return false;
    }

    if ((attribute.inputType & EditorInfo.TYPE_MASK_CLASS) != EditorInfo.TYPE_CLASS_TEXT) {
      return false;
    }

    switch (attribute.inputType & EditorInfo.TYPE_MASK_VARIATION) {
      case EditorInfo.TYPE_TEXT_VARIATION_PASSWORD:
      case EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
      case EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD:
        return false;
      default:
        return true;
    }
  }

  private void updateCandidateVisibility(boolean visible) {
    if (mCandidateView == null) {
      return;
    }

    View parent = (View) mCandidateView.getParent();
    if (parent != null) {
      parent.setVisibility(View.VISIBLE);
    }
    mCandidateView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
  }

  private String formatSuggestionForCommit(String suggestion) {
    if (shouldCapitalizeSuggestion()) {
      return capitalizeFirstCodePoint(suggestion);
    }
    return suggestion;
  }

  private boolean shouldCapitalizeSuggestion() {
    InputConnection inputConnection = getCurrentInputConnection();
    if (inputConnection == null || mComposingWord.length() == 0) {
      return false;
    }

    CharSequence beforeCursor =
        inputConnection.getTextBeforeCursor(mComposingWord.length() + 64, 0);
    if (beforeCursor == null) {
      return false;
    }

    String beforeText = beforeCursor.toString();
    String normalizedBeforeText = beforeText.toLowerCase(Locale.ROOT);
    if (!normalizedBeforeText.endsWith(mComposingWord.toString())) {
      return false;
    }

    String contextBeforeWord = beforeText.substring(0, beforeText.length() - mComposingWord.length());
    for (int index = contextBeforeWord.length() - 1; index >= 0; index--) {
      char current = contextBeforeWord.charAt(index);
      if (Character.isWhitespace(current)) {
        continue;
      }
      return current == '.' || current == '!' || current == '?' || current == '\n';
    }

    return true;
  }

  private String capitalizeFirstCodePoint(String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }

    int firstCodePoint = value.codePointAt(0);
    int upperCodePoint = Character.toUpperCase(firstCodePoint);
    int firstCodePointLength = Character.charCount(firstCodePoint);
    StringBuilder capitalized = new StringBuilder(value.length());
    capitalized.appendCodePoint(upperCodePoint);
    capitalized.append(value.substring(firstCodePointLength));
    return capitalized.toString();
  }

  private boolean cursorStillAtComposingWord(int newSelStart, int newSelEnd) {
    if (newSelStart != newSelEnd) {
      return false;
    }

    InputConnection inputConnection = getCurrentInputConnection();
    if (inputConnection == null) {
      return false;
    }

    CharSequence beforeCursor = inputConnection.getTextBeforeCursor(mComposingWord.length(), 0);
    if (beforeCursor == null) {
      return false;
    }
    return beforeCursor.toString().toLowerCase(Locale.ROOT).endsWith(mComposingWord.toString());
  }

  private String getWordFragmentBeforeCursor() {
    InputConnection inputConnection = getCurrentInputConnection();
    if (inputConnection == null) {
      return "";
    }

    CharSequence beforeCursor = inputConnection.getTextBeforeCursor(64, 0);
    if (beforeCursor == null || beforeCursor.length() == 0) {
      return "";
    }

    int wordStart = beforeCursor.length();
    while (wordStart > 0) {
      char current = beforeCursor.charAt(wordStart - 1);
      if (!Character.isLetter(current) && current != '\'' && current != '-') {
        break;
      }
      wordStart--;
    }

    return beforeCursor.subSequence(wordStart, beforeCursor.length()).toString().toLowerCase(Locale.ROOT);
  }
}
