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

package net.myrrix.online.eval;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import org.apache.mahout.cf.taste.common.NoSuchItemException;
import org.apache.mahout.cf.taste.common.NoSuchUserException;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.WeightedRunningAverage;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.myrrix.common.MyrrixRecommender;

/**
 * An alternate evaluation  which computes the estimated strength score (see
 * {@link org.apache.mahout.cf.taste.recommender.Recommender#estimatePreference(long, long)}) for each test
 * datum. It simply reports the average -- a weighted average, weighted by the test datum's value.
 *
 * @author Sean Owen
 */
public final class EstimatedStrengthEvaluator extends AbstractEvaluator {

  private static final Logger log = LoggerFactory.getLogger(EstimatedStrengthEvaluator.class);

  @Override
  protected boolean isSplitTestByPrefValue() {
    return false;
  }

  @Override
  public EvaluationResult evaluate(MyrrixRecommender recommender,
                                   Multimap<Long,RecommendedItem> testData) throws TasteException {
    WeightedRunningAverage score = new WeightedRunningAverage();
    int count = 0;
    int unknownItems = 0;
    int unknownUsers = 0;
    for (Map.Entry<Long,RecommendedItem> entry : testData.entries()) {
      long userID = entry.getKey();
      RecommendedItem itemPref = entry.getValue();
      try {
        float estimate = recommender.estimatePreference(userID, itemPref.getItemID());
        Preconditions.checkState(!Float.isNaN(estimate));
        score.addDatum(estimate, itemPref.getValue());
      } catch (NoSuchItemException nsie) {
        unknownItems++;
        // continue
      } catch (NoSuchUserException nsue) {
        unknownUsers++;
        // continue
      }
      if (++count % 10000 == 0) {
        log.info("Score: {}", score);
      }
    }
    log.info("Score: {}", score);
    log.info("{} unknown items, {} unknown users", unknownItems, unknownUsers); // TODO
    return new EvaluationResultImpl(score.getAverage());
  }

}