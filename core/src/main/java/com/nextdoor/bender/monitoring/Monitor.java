/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 * Copyright 2017 Nextdoor.com, Inc
 *
 */

package com.nextdoor.bender.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.log4j.Logger;

/**
 * Writes invocation stats (stats that are generated by lambda calling the function) and function
 * instance stats (stats that are created the first time the function runs) using each
 * {@link Reporter} specified.
 */
public class Monitor {
  private static final Logger logger = Logger.getLogger(Monitor.class);
  private static ArrayList<Stat> instanceStats = new ArrayList<Stat>();
  private static ArrayList<Stat> invocationStats = new ArrayList<Stat>();
  private static ArrayList<Reporter> statsReporters = new ArrayList<Reporter>();
  private static ArrayList<MonitoredProcess> processes = new ArrayList<MonitoredProcess>();

  protected static Set<Tag> tags = new HashSet<Tag>();
  private static Monitor monitor = new Monitor();
  private long invokeTime = 0;

  public void addReporters(List<Reporter> reporters) {
    for (Reporter rep : reporters) {
      if (!statsReporters.contains(rep)) {
        statsReporters.add(rep);
      }
    }
  }

  public void reset() {
    statsReporters.clear();
    instanceStats.clear();
    instanceStats.clear();
    tags.clear();
  }

  public Set<Tag> getTags() {
    return Monitor.tags;
  }

  public Map<String, String> getTagsMap() {
    HashMap<String, String> map = new HashMap<String, String>();
    Monitor.tags.forEach(t -> {
      map.put(t.getKey(), t.getValue());
    });

    return map;
  }

  public void addTag(String name, String val) {
    /*
     * Remove existing tags that will be updated
     */
    Monitor.tags.removeIf(p -> {
      return p.getKey().equals(name);
    });

    Monitor.tags.add(new Tag(name, val));
  }

  public void addTagsMap(Map<String, String> tags) {
    /*
     * Remove existing tags that will be updated
     */
    Monitor.tags.removeIf(p -> {
      return tags.containsKey(p.getKey());
    });

    /*
     * Add new / update tags
     */
    tags.forEach((k, v) -> {
      Monitor.tags.add(new Tag(k, v));
    });
  }

  public void addTags(Set<Tag> tags) {
    /*
     * Remove existing tags that will be updated
     */
    Monitor.tags.removeAll(tags);

    /*
     * Add new /update tags
     */
    Monitor.tags.addAll(tags);
  }

  public static Monitor getInstance() {
    return monitor;
  }

  public synchronized void addInvocationStat(Stat stat) {
    invocationStats.add(stat);
  }

  public synchronized void addInstanceStat(Stat stat) {
    instanceStats.add(stat);
  }

  public ArrayList<Stat> getStats() {
    ArrayList<Stat> combinedStats = new ArrayList<Stat>();
    combinedStats.addAll(instanceStats);
    combinedStats.addAll(invocationStats);

    return combinedStats;
  }

  public void invokeTimeNow() {
    this.invokeTime = System.currentTimeMillis();
  }

  public void addProcess(MonitoredProcess proccess) {
    processes.add(proccess);
  }

  public void writeStats() {

    for (Reporter reporter : statsReporters) {
      List<StatFilter> filters = reporter.getStatFilters();
      ArrayList<Stat> stats = getStats();

      for (StatFilter filter : filters) {
        Predicate<Stat> statPredicate = StatFilter.isMatch(filter);
        stats.removeIf(statPredicate);
      }

      /*
       * Catch anything a reporter may throw to prevent function failures.
       */
      try {
        reporter.write(stats, invokeTime, tags);
      } catch (Exception e) {
        logger.warn("reporter threw an error while writing stats", e);
      }
    }

    clearStats();
  }

  public long getInvokeTime() {
    return invokeTime;
  }

  public void clearStats() {
    invocationStats.clear();
    for (MonitoredProcess process : processes) {
      process.clearStats();
    }
  }
}
