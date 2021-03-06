/*
 * Copyright 2018 LinkedIn Corp. Licensed under the BSD 2-Clause License (the "License"). See License in the project root for license information.
 */

package com.linkedin.kafka.cruisecontrol.detector;

import com.linkedin.cruisecontrol.config.CruiseControlConfig;
import com.linkedin.cruisecontrol.detector.metricanomaly.MetricAnomaly;
import com.linkedin.cruisecontrol.detector.metricanomaly.MetricAnomalyFinder;
import com.linkedin.cruisecontrol.monitor.sampling.aggregator.ValuesAndExtrapolations;
import com.linkedin.kafka.cruisecontrol.KafkaCruiseControlUnitTestUtils;
import com.linkedin.kafka.cruisecontrol.config.constants.AnomalyDetectorConfig;
import com.linkedin.kafka.cruisecontrol.monitor.sampling.holder.BrokerEntity;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.linkedin.cruisecontrol.detector.metricanomaly.PercentileMetricAnomalyFinderConfig.METRIC_ANOMALY_PERCENTILE_LOWER_THRESHOLD_CONFIG;
import static com.linkedin.cruisecontrol.detector.metricanomaly.PercentileMetricAnomalyFinderConfig.METRIC_ANOMALY_PERCENTILE_UPPER_THRESHOLD_CONFIG;
import static com.linkedin.cruisecontrol.detector.metricanomaly.PercentileMetricAnomalyFinderConfig.METRIC_ANOMALY_LOWER_MARGIN_CONFIG;
import static com.linkedin.cruisecontrol.detector.metricanomaly.PercentileMetricAnomalyFinderConfig.METRIC_ANOMALY_UPPER_MARGIN_CONFIG;
import static com.linkedin.kafka.cruisecontrol.detector.AnomalyDetectorTestUtils.createHistory;
import static com.linkedin.kafka.cruisecontrol.detector.AnomalyDetectorTestUtils.createCurrentMetrics;
import static com.linkedin.kafka.cruisecontrol.detector.AnomalyDetectorTestUtils.BROKER_ENTITIES;
import static com.linkedin.kafka.cruisecontrol.detector.AnomalyDetectorTestUtils.ANOMALY_DETECTION_TIME_MS;
import static com.linkedin.kafka.cruisecontrol.detector.AnomalyDetectorTestUtils.createMetricAnomalyFinder;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;


public class KafkaMetricAnomalyFinderTest {
  private static short METRIC_ID = 55;

  @Rule
  public ExpectedException _expected = ExpectedException.none();

  @Test
  public void testMetricAnomaliesWithNullArguments() {
    MetricAnomalyFinder<BrokerEntity> anomalyFinder = createKafkaMetricAnomalyFinder();

    _expected.expect(IllegalArgumentException.class);
    assertTrue("IllegalArgumentException is expected for null history or null current metrics.",
               anomalyFinder.metricAnomalies(null, null).isEmpty());
  }

  @Test
  public void testMetricAnomalies() {
    MetricAnomalyFinder<BrokerEntity> anomalyFinder = createKafkaMetricAnomalyFinder();
    Map<BrokerEntity, ValuesAndExtrapolations> history =
        createHistory(Collections.singletonMap(METRIC_ID, 20.0), Collections.singletonMap(METRIC_ID, 1.0), 20, BROKER_ENTITIES.get(0));
    Map<BrokerEntity, ValuesAndExtrapolations> currentMetrics =
        createCurrentMetrics(Collections.singletonMap(METRIC_ID, 40.0), 21, BROKER_ENTITIES.get(0));
    Collection<MetricAnomaly<BrokerEntity>> anomalies = anomalyFinder.metricAnomalies(history, currentMetrics);
    assertTrue("There should be exactly a single metric anomaly", anomalies.size() == 1);
    MetricAnomaly<BrokerEntity> anomaly = anomalies.iterator().next();
    assertTrue(anomaly.entities().containsKey(BROKER_ENTITIES.get(0)));
    assertEquals(ANOMALY_DETECTION_TIME_MS, (long) anomaly.entities().get(BROKER_ENTITIES.get(0)));
  }

  @Test
  public void testInsufficientData() {
    MetricAnomalyFinder<BrokerEntity> anomalyFinder = createKafkaMetricAnomalyFinder();
    Map<BrokerEntity, ValuesAndExtrapolations> history =
        createHistory(Collections.singletonMap(METRIC_ID, 20.0), Collections.singletonMap(METRIC_ID, 1.0), 19, BROKER_ENTITIES.get(0));
    Map<BrokerEntity, ValuesAndExtrapolations> currentMetrics =
        createCurrentMetrics(Collections.singletonMap(METRIC_ID, 20.0), 20, BROKER_ENTITIES.get(0));
    Collection<MetricAnomaly<BrokerEntity>> anomalies = anomalyFinder.metricAnomalies(history, currentMetrics);
    assertTrue(anomalies.isEmpty());
  }

  private MetricAnomalyFinder<BrokerEntity> createKafkaMetricAnomalyFinder() {
    Properties props = KafkaCruiseControlUnitTestUtils.getKafkaCruiseControlProperties();
    props.setProperty(AnomalyDetectorConfig.METRIC_ANOMALY_FINDER_CLASSES_CONFIG, KafkaMetricAnomalyFinder.class.getName());
    props.setProperty(METRIC_ANOMALY_PERCENTILE_UPPER_THRESHOLD_CONFIG, "95.0");
    props.setProperty(METRIC_ANOMALY_PERCENTILE_LOWER_THRESHOLD_CONFIG, "5.0");
    props.setProperty(METRIC_ANOMALY_UPPER_MARGIN_CONFIG, "0.5");
    props.setProperty(METRIC_ANOMALY_LOWER_MARGIN_CONFIG, "0.2");
    props.setProperty(CruiseControlConfig.METRIC_ANOMALY_FINDER_METRICS_CONFIG,
                      "BROKER_PRODUCE_LOCAL_TIME_MS_50TH,BROKER_PRODUCE_LOCAL_TIME_MS_999TH,BROKER_CONSUMER_FETCH_LOCAL_TIME_MS_50TH,"
                      + "BROKER_CONSUMER_FETCH_LOCAL_TIME_MS_999TH,BROKER_FOLLOWER_FETCH_LOCAL_TIME_MS_50TH,"
                      + "BROKER_FOLLOWER_FETCH_LOCAL_TIME_MS_999TH,BROKER_LOG_FLUSH_TIME_MS_50TH,BROKER_LOG_FLUSH_TIME_MS_999TH");
    return createMetricAnomalyFinder(props);
  }
}
