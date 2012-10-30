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

package net.myrrix.online.partition;

import java.util.List;

import org.apache.mahout.common.Pair;

/**
 * Does nothing; not applicable in local mode.
 *
 * @author Sean Owen
 */
public final class PartitionLoaderImpl implements PartitionLoader {

  /**
   * @throws UnsupportedOperationException
   */
  @Override
  public List<List<Pair<String, Integer>>> loadPartitions(int defaultPort, String bucket, String instanceID) {
    throw new UnsupportedOperationException();
  }

}
