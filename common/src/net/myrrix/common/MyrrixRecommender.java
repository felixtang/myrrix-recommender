/*
 * Copyright Myrrix Ltd
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

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.myrrix.common;

import java.io.File;
import java.io.Reader;
import java.util.List;

import org.apache.mahout.cf.taste.common.NoSuchItemException;
import org.apache.mahout.cf.taste.common.NoSuchUserException;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.IDRescorer;
import org.apache.mahout.cf.taste.recommender.ItemBasedRecommender;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Rescorer;
import org.apache.mahout.common.LongPair;

import net.myrrix.common.collection.FastIDSet;

/**
 * <p>Defines additional methods supported beyond what is in {@link ItemBasedRecommender},
 * including a method to recommend to "anonymous" users {@link #recommendToAnonymous(long[], int)}
 * and a means to bulk-load data {@link #ingest(Reader)}.</p>
 *
 * <p>Note that the API methods {@link #setPreference(long, long)}
 * and {@link #removePreference(long, long)}, retained from Apache Mahout, have a somewhat different meaning
 * than in Mahout. They add to an association strength, rather than replace it. See the javadoc.</p>
 *
 * <p>Several methods exist here because they are defined in Mahout, but have no useful meaning in this
 * implementation. These are marked as {@code @Deprecated} and should not be used.</p>
 *
 * @author Sean Owen
 * @author Mahout
 */
public interface MyrrixRecommender extends ItemBasedRecommender {

  // Inherited from Recommender

  /**
   * @param userID user for which recommendations are to be computed
   * @param howMany desired number of recommendations
   * @return {@link List} of recommended {@link RecommendedItem}s, ordered from most strongly recommend to least
   * @throws NoSuchUserException if the user is not known
   */
  @Override
  List<RecommendedItem> recommend(long userID, int howMany) throws TasteException;

  /**
   * @param userID user for which recommendations are to be computed
   * @param howMany desired number of recommendations
   * @param rescorer rescoring function to apply before final list of recommendations is determined
   * @return {@link List} of recommended {@link RecommendedItem}s, ordered from most strongly recommend to least
   * @throws NoSuchUserException if the user is not known
   */
  @Override
  List<RecommendedItem> recommend(long userID, int howMany, IDRescorer rescorer) throws TasteException;

  /**
   * @param userID user ID whose preference is to be estimated
   * @param itemID item ID to estimate preference for
   * @return an estimated preference, which may not be the total strength from the input. This is always an
   *  estimate built from the model alone. Returns 0 if user or item is not known.
   */
  @Override
  float estimatePreference(long userID, long itemID) throws TasteException;

  /**
   * <p>Adds to a user-item preference, or association. This is called in response to some action that indicates
   * the user has a stronger association to an item, like a click or purchase. It is intended to be called many
   * times for a user and item, as more actions are observed that associate the two. That is, this calls
   * <em>adds to</em> rather than <em>sets</em> the association.</p>
   *
   * <p>To "undo" associations, call this method with negative values, or
   * see {@link #removePreference(long, long)}.</p>
   *
   * <p>Value is <strong>not</strong> a rating, but a strength indicator. It may be negative.
   * Its magnitude should correspond to the degree to which an observed event suggests an association between
   * a user and item. A value twice as big should correspond to an event that suggests twice as strong an
   * association.</p>
   *
   * <p>For example, a click on a video might result in a call with value 1.0. Watching half of the video
   * might result in another call adding value 3.0. Finishing the video, another 3.0. Liking or sharing the video,
   * an additional 10.0. Clicking away from a video within 10 seconds might result in a -3.0.</p>
   *
   * @param userID user involved in the new preference
   * @param itemID item involved
   * @param value strength value
   * @throws TasteException if the preference cannot be updated, due to a server error
   * @throws NotReadyException if the implementation has no usable model yet
   */
  @Override
  void setPreference(long userID, long itemID, float value) throws TasteException;

  /**
   * <p>This method will remove an item from the user's set of known items,
   * making it eligible for recommendation again. If the user has no more items, this method will remove
   * the user too, such that new calls to {@link #recommend(long, int)} for example
   * will fail with {@link org.apache.mahout.cf.taste.common.NoSuchUserException}.</p>
   *
   * <p>It does not affect any user-item association strengths.</p>
   *
   * <p>Contrast with calling {@link #setPreference(long, long, float)} with a negative value,
   * which merely records a negative association between the user and item.</p>
   */
  @Override
  void removePreference(long userID, long itemID) throws TasteException;

  /**
   * @deprecated do not call; only present to satisfy Mahout API
   * @throws UnsupportedOperationException in general
   */
  @Deprecated
  @Override
  DataModel getDataModel();

  // Inherited from ItemBasedRecommender

  /**
   * @param itemID ID of item for which to find most similar other items
   * @param howMany desired number of most similar items to find
   * @return items most similar to the given item, ordered from most similar to least
   * @throws NoSuchItemException if the item is not known
   */
  @Override
  List<RecommendedItem> mostSimilarItems(long itemID, int howMany) throws TasteException;

  /**
   * @param itemID ID of item for which to find most similar other items
   * @param howMany desired number of most similar items to find
   * @param rescorer {@link Rescorer} which can adjust item-item similarity estimates used to determine
   *  most similar items
   * @return items most similar to the given item, ordered from most similar to least
   * @throws NoSuchItemException if the item is not known
   */
  @Override
  List<RecommendedItem> mostSimilarItems(long itemID, int howMany, Rescorer<LongPair> rescorer) throws TasteException;

  /**
   * @param itemIDs IDs of item for which to find most similar other items
   * @param howMany desired number of most similar items to find estimates used to determine most similar items
   * @return items most similar to the given items, ordered from most similar to least
   * @throws NoSuchItemException if <em>none</em> of {@code itemIDs} exist in the model. Otherwise, unknown
   *  items are ignored.
   */
  @Override
  List<RecommendedItem> mostSimilarItems(long[] itemIDs, int howMany) throws TasteException;

  /**
   * @param itemIDs IDs of item for which to find most similar other items
   * @param howMany desired number of most similar items to find
   * @param rescorer {@link Rescorer} which can adjust item-item similarity estimates used to determine
   *  most similar items
   * @return items most similar to the given items, ordered from most similar to least
   * @throws NoSuchItemException if <em>none</em> of {@code itemIDs} exist in the model. Otherwise, unknown
   *  items are ignored.
   */
  @Override
  List<RecommendedItem> mostSimilarItems(long[] itemIDs,
                                         int howMany,
                                         Rescorer<LongPair> rescorer) throws TasteException;

  /**
   * @deprecated do not call; only present to satisfy Mahout API
   * @throws UnsupportedOperationException in general
   */
  @Deprecated
  @Override
  List<RecommendedItem> mostSimilarItems(long[] itemIDs,
                                         int howMany,
                                         boolean excludeItemIfNotSimilarToAll) throws TasteException;

  /**
   * @deprecated do not call; only present to satisfy Mahout API
   * @throws UnsupportedOperationException in general
   */
  @Deprecated
  @Override
  List<RecommendedItem> mostSimilarItems(long[] itemIDs,
                                         int howMany,
                                         Rescorer<LongPair> rescorer,
                                         boolean excludeItemIfNotSimilarToAll) throws TasteException;

  /**
   * <p>Lists the items that were most influential in recommending a given item to a given user. Exactly how this
   * is determined is left to the implementation, but, generally this will return items that the user prefers
   * and that are similar to the given item.</p>
   *
   * <p>This returns a {@link List} of {@link RecommendedItem} which is a little misleading since it's returning
   * recommend<strong>ing</strong> items, but, I thought it more natural to just reuse this class since it
   * encapsulates an item and value. The value here does not necessarily have a consistent interpretation or
   * expected range; it will be higher the more influential the item was in the recommendation.</p>
   *
   * @param userID ID of user who was recommended the item
   * @param itemID ID of item that was recommended
   * @param howMany maximum number of items to return
   * @return {@link List} of {@link RecommendedItem}, ordered from most influential in recommended the
   *  given item to least
   */
  @Override
  List<RecommendedItem> recommendedBecause(long userID, long itemID, int howMany) throws TasteException;

  /**
   * @param userID user for which recommendations are to be computed
   * @param howMany desired number of recommendations
   * @param considerKnownItems if true, items that the user is already associated to are candidates
   *  for recommendation. Normally this is {@code false}.
   * @param rescorer rescoring function used to modify association strengths before ranking results
   * @return {@link List} of recommended {@link RecommendedItem}s, ordered from most strongly recommend to least
   * @throws NotReadyException if the recommender has no model available yet
   * @throws TasteException if another error occurs
   */
  List<RecommendedItem> recommend(long userID,
                                  int howMany,
                                  boolean considerKnownItems,
                                  IDRescorer rescorer) throws TasteException;

  /**
   * Recommends to a group of users all at once. It takes into account their tastes equally and produces
   * one set of recommendations that is deemed most suitable to them as a group. It is otherwise identical
   * to {@link #recommend(long, int, boolean, IDRescorer)}.
   *
   * @see #recommend(long, int, boolean, IDRescorer)
   * @throws NoSuchUserException if <em>none</em> of {@code userIDs} exist in the model. Otherwise, unknown
   *  users are ignored.
   */
  List<RecommendedItem> recommendToMany(long[] userIDs,
                                        int howMany,
                                        boolean considerKnownItems,
                                        IDRescorer rescorer) throws TasteException;

  /**
   * Computes recommendations for a user that is not known to the model yet; instead, the user's
   * associated items are supplied to the method and it proceeds as if a user with these associated
   * items were in the model.
   *
   * @see #recommend(long, int)
   * @throws NotReadyException if the implementation has no usable model yet
   * @throws org.apache.mahout.cf.taste.common.NoSuchItemException if <em>none</em> of {@code itemIDs}
   *  exist in the model. Otherwise, unknown items are ignored.
   */
  List<RecommendedItem> recommendToAnonymous(long[] itemIDs, int howMany) throws TasteException;

  /**
   * Like {@link #recommendToAnonymous(long[], int)} but rescorer results like
   * {@link #recommend(long, int, boolean, IDRescorer)}.
   *
   * @see #recommendToAnonymous(long[], int)
   * @throws org.apache.mahout.cf.taste.common.NoSuchItemException if <em>none</em> of {@code itemIDs}
   *  exist in the model. Otherwise, unknown items are ignored.
   */
  List<RecommendedItem> recommendToAnonymous(long[] itemIDs, int howMany, IDRescorer rescorer) throws TasteException;

  /**
   * A bulk version of {@link #estimatePreference(long, long)}, suitable for computing many estimates
   * at once. The return values correspond, in order, to the item IDs provided, in order.
   *
   * @see #estimatePreference(long, long)
   */
  float[] estimatePreferences(long userID, long... itemIDs) throws TasteException;

  /**
   * Like {@link #ingest(File)}, but reads from a {@link Reader}.
   *
   * @param reader source of CSV data to ingest
   * @throws NotReadyException if the implementation has no usable model yet
   */
  void ingest(Reader reader) throws TasteException;

  /**
   * "Uploads" a series of new associations to the recommender; this is like a bulk version of
   * {@link #setPreference(long, long, float)}. The input file should be in CSV format, where each
   * line is of the form {@code userID,itemID,value}. Note that the file may be compressed. If it is
   * make sure that its name reflects its compression -- gzip ending in ".gz", zip ending in ".zip",
   * deflate ending in ".deflate".
   *
   * @param file CSV file to ingest, possibly compressed.
   * @throws NotReadyException if the implementation has no usable model yet
   */
  void ingest(File file) throws TasteException;

  // Some overloads that make sense in the project's model:

  /**
   * Defaults to value 1.0.
   *
   * @see #setPreference(long, long, float)
   * @throws TasteException if the preference cannot be updated, due to a server error
   * @throws NotReadyException if the implementation has no usable model yet
   */
  void setPreference(long userID, long itemID) throws TasteException;

  /**
   * @return true if and only if the instance is ready to make recommendations; may be false for example
   *  while the recommender is still building an initial model
   */
  boolean isReady() throws TasteException;

  /**
   * Blocks until {@link #isReady()} returns {@code true}.
   */
  void await() throws TasteException;

  /**
   * @return all user IDs currently in the model
   */
  FastIDSet getAllUserIDs() throws TasteException;

  /**
   * @return all item IDs currently in the model
   */
  FastIDSet getAllItemIDs() throws TasteException;

}
