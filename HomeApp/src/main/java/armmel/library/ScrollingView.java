/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package armmel.library;

/** An interface that can be implemented by Views to provide scroll related APIs. */
public interface ScrollingView {
  /**
   * Compute the horizontal range that the horizontal scrollbar represents.
   *
   * <p>The range is expressed in arbitrary units that must be the same as the units used by {@link
   * #computeHorizontalScrollExtent()} and {@link #computeHorizontalScrollOffset()}.
   *
   * <p>The default range is the drawing width of this view.
   *
   * @return the total horizontal range represented by the horizontal scrollbar
   * @see #computeHorizontalScrollExtent()
   * @see #computeHorizontalScrollOffset()
   * @see android.widget.ScrollBarDrawable
   */
  int computeHorizontalScrollRange();

  /**
   * Compute the horizontal offset of the horizontal scrollbar's thumb within the horizontal range.
   * This value is used to compute the position of the thumb within the scrollbar's track.
   *
   * <p>The range is expressed in arbitrary units that must be the same as the units used by {@link
   * #computeHorizontalScrollRange()} and {@link #computeHorizontalScrollExtent()}.
   *
   * <p>The default offset is the scroll offset of this view.
   *
   * @return the horizontal offset of the scrollbar's thumb
   * @see #computeHorizontalScrollRange()
   * @see #computeHorizontalScrollExtent()
   * @see android.widget.ScrollBarDrawable
   */
  int computeHorizontalScrollOffset();

  /**
   * Compute the horizontal extent of the horizontal scrollbar's thumb within the horizontal range.
   * This value is used to compute the length of the thumb within the scrollbar's track.
   *
   * <p>The range is expressed in arbitrary units that must be the same as the units used by {@link
   * #computeHorizontalScrollRange()} and {@link #computeHorizontalScrollOffset()}.
   *
   * <p>The default extent is the drawing width of this view.
   *
   * @return the horizontal extent of the scrollbar's thumb
   * @see #computeHorizontalScrollRange()
   * @see #computeHorizontalScrollOffset()
   * @see android.widget.ScrollBarDrawable
   */
  int computeHorizontalScrollExtent();

  /**
   * Compute the vertical range that the vertical scrollbar represents.
   *
   * <p>The range is expressed in arbitrary units that must be the same as the units used by {@link
   * #computeVerticalScrollExtent()} and {@link #computeVerticalScrollOffset()}.
   *
   * @return the total vertical range represented by the vertical scrollbar
   *     <p>The default range is the drawing height of this view.
   * @see #computeVerticalScrollExtent()
   * @see #computeVerticalScrollOffset()
   * @see android.widget.ScrollBarDrawable
   */
  int computeVerticalScrollRange();

  /**
   * Compute the vertical offset of the vertical scrollbar's thumb within the horizontal range. This
   * value is used to compute the position of the thumb within the scrollbar's track.
   *
   * <p>The range is expressed in arbitrary units that must be the same as the units used by {@link
   * #computeVerticalScrollRange()} and {@link #computeVerticalScrollExtent()}.
   *
   * <p>The default offset is the scroll offset of this view.
   *
   * @return the vertical offset of the scrollbar's thumb
   * @see #computeVerticalScrollRange()
   * @see #computeVerticalScrollExtent()
   * @see android.widget.ScrollBarDrawable
   */
  int computeVerticalScrollOffset();

  /**
   * Compute the vertical extent of the vertical scrollbar's thumb within the vertical range. This
   * value is used to compute the length of the thumb within the scrollbar's track.
   *
   * <p>The range is expressed in arbitrary units that must be the same as the units used by {@link
   * #computeVerticalScrollRange()} and {@link #computeVerticalScrollOffset()}.
   *
   * <p>The default extent is the drawing height of this view.
   *
   * @return the vertical extent of the scrollbar's thumb
   * @see #computeVerticalScrollRange()
   * @see #computeVerticalScrollOffset()
   * @see android.widget.ScrollBarDrawable
   */
  int computeVerticalScrollExtent();
}
