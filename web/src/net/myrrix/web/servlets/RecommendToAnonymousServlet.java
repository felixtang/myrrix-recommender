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

package net.myrrix.web.servlets;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.mahout.cf.taste.common.NoSuchItemException;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.recommender.IDRescorer;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;

import net.myrrix.common.MyrrixRecommender;
import net.myrrix.common.NotReadyException;
import net.myrrix.common.collection.FastIDSet;
import net.myrrix.online.RescorerProvider;

/**
 * <p>Responds to a GET request to {@code /recommendToAnonymous/[itemID1](/[itemID2]/...)?howMany=n},
 * and in turn calls {@link MyrrixRecommender#recommendToAnonymous(long[], int)} with the supplied values.
 * If howMany is not specified, defaults to {@link AbstractMyrrixServlet#DEFAULT_HOW_MANY}.</p>
 *
 * <p>Unknown item IDs are ignored, unless all are unknown, in which case a
 * {@link HttpServletResponse#SC_BAD_REQUEST} status is returned.</p>
 *
 * <p>Outputs item/score pairs in CSV or JSON format, like {@link RecommendServlet} does.</p>
 *
 * @author Sean Owen
 */
public final class RecommendToAnonymousServlet extends AbstractMyrrixServlet {
  
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    String pathInfo = request.getPathInfo();
    Iterator<String> pathComponents = SLASH.split(pathInfo).iterator();
    FastIDSet itemIDSet = new FastIDSet();
    try {
      while (pathComponents.hasNext()) {
        itemIDSet.add(Long.parseLong(pathComponents.next()));
      }
    } catch (NoSuchElementException nsee) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, nsee.toString());
      return;
    } catch (NumberFormatException nfe) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, nfe.toString());
      return;
    }

    if (itemIDSet.isEmpty()) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    long[] itemIDs = itemIDSet.toArray();
    MyrrixRecommender recommender = getRecommender();
    RescorerProvider rescorerProvider = getRescorerProvider();
    IDRescorer rescorer = rescorerProvider == null ? null :
        rescorerProvider.getRecommendToAnonymousRescorer(itemIDs, getRescorerParams(request));
    try {
      List<RecommendedItem> recommended = recommender.recommendToAnonymous(itemIDs, getHowMany(request), rescorer);
      output(request, response, recommended);
    } catch (NotReadyException nre) {
      response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, nre.toString());
    } catch (NoSuchItemException nsie) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND, nsie.toString());
    } catch (TasteException te) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, te.toString());
      getServletContext().log("Unexpected error in " + getClass().getSimpleName(), te);
    }
  }

  @Override
  protected Long getUnnormalizedPartitionToServe(HttpServletRequest request) {
    String pathInfo = request.getPathInfo();
    Iterator<String> pathComponents = SLASH.split(pathInfo).iterator();
    long firstItemID;
    try {
      firstItemID = Long.parseLong(pathComponents.next());
    } catch (NoSuchElementException nsee) {
      return null;
    } catch (NumberFormatException nfe) {
      return null;
    }
    return firstItemID;
  }

}
