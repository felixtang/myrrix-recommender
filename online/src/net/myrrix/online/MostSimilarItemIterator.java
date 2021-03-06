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

package net.myrrix.online;

import java.util.Iterator;

import com.google.common.base.Preconditions;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Rescorer;
import org.apache.mahout.common.LongPair;

import net.myrrix.common.LangUtils;
import net.myrrix.common.MutableRecommendedItem;
import net.myrrix.common.collection.FastByIDMap;
import net.myrrix.common.math.SimpleVectorMath;

/**
 * An {@link Iterator} that generates and iterates over all possible candidate items in computation
 * of {@link org.apache.mahout.cf.taste.recommender.ItemBasedRecommender#mostSimilarItems(long, int)}.
 *
 * @author Sean Owen
 * @see RecommendedBecauseIterator
 * @see RecommendIterator
 */
final class MostSimilarItemIterator implements Iterator<RecommendedItem> {

  private final MutableRecommendedItem delegate;
  private final float[][] itemFeatures;
  private final Iterator<FastByIDMap.MapEntry<float[]>> Yiterator;
  private final long[] toItemIDs;
  private final Rescorer<LongPair> rescorer;

  MostSimilarItemIterator(Iterator<FastByIDMap.MapEntry<float[]>> Yiterator,
                          long[] toItemIDs,
                          float[][] itemFeatures,
                          Rescorer<LongPair> rescorer) {
    delegate = new MutableRecommendedItem();
    this.toItemIDs = toItemIDs;
    this.itemFeatures = itemFeatures;
    this.Yiterator = Yiterator;
    this.rescorer = rescorer;
  }

  @Override
  public boolean hasNext() {
    return Yiterator.hasNext();
  }

  @Override
  public RecommendedItem next() {
    FastByIDMap.MapEntry<float[]> entry = Yiterator.next();
    long itemID = entry.getKey();
    for (long l : toItemIDs) {
      if (l == itemID) {
        return null;
      }
    }

    Rescorer<LongPair> rescorer1 = this.rescorer;
    float[] candidateFeatures = entry.getValue();
    double candidateFeaturesNorm = SimpleVectorMath.norm(candidateFeatures);
    double total = 0.0;

    int length = itemFeatures.length;
    for (int i = 0; i < length; i++) {
      long toItemID = toItemIDs[i];
      if (rescorer1 != null && rescorer1.isFiltered(new LongPair(itemID, toItemID))) {
        return null;
      }
      float[] features = itemFeatures[i];
      double featuresNorm = SimpleVectorMath.norm(features);
      double similarity = SimpleVectorMath.dot(candidateFeatures, features) / (candidateFeaturesNorm * featuresNorm);
      if (!LangUtils.isFinite(similarity)) {
        return null;
      }
      if (rescorer1 != null) {
        similarity = rescorer1.rescore(new LongPair(itemID, toItemID), similarity);
        if (!LangUtils.isFinite(similarity)) {
          return null;
        }
      }
      total += similarity;
    }

    float result = (float) (total / length);
    Preconditions.checkState(LangUtils.isFinite(result), "Bad similarity value");
    delegate.set(itemID, result);
    return delegate;
  }

  /**
   * @throws UnsupportedOperationException
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

}
