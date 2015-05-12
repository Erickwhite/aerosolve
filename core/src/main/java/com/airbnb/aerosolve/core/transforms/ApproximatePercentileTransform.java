package com.airbnb.aerosolve.core.transforms;

import com.airbnb.aerosolve.core.FeatureVector;
import com.typesafe.config.Config;

import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Given a fieldName1, low, middle, upper key
 * Remaps fieldName2's key2 value such that low = 0, mid =0.5, upper = 1.0 thus approximating
 * the percentile using piecewise linear interpolation.
 */
public class ApproximatePercentileTransform extends Transform {
  private String fieldName1;
  private String fieldName2;
  private String lowKey;
  private String midKey;
  private String upperKey;
  private String key2;
  private String outputName;
  private String outputKey;

  @Override
  public void configure(Config config, String key) {
    fieldName1 = config.getString(key + ".field1");
    fieldName2 = config.getString(key + ".field2");
    lowKey = config.getString(key + ".low");
    midKey = config.getString(key + ".mid");
    upperKey = config.getString(key + ".upper");
    key2 =  config.getString(key + ".key2");
    outputName = config.getString(key + ".output");
    outputKey = config.getString(key + ".outputKey");
  }

  @Override
  public void doTransform(FeatureVector featureVector) {
    Map<String, Map<String, Double>> floatFeatures = featureVector.getFloatFeatures();

    if (floatFeatures == null) {
      return;
    }

    Map<String, Double> feature1 = floatFeatures.get(fieldName1);
    if (feature1 == null) {
      return;
    }

    Map<String, Double> feature2 = floatFeatures.get(fieldName2);
    if (feature2 == null) {
      return;
    }

    Double val = feature2.get(key2);
    if (val == null) {
      return;
    }

    Double low = feature1.get(lowKey);
    Double mid = feature1.get(midKey);
    Double upper = feature1.get(upperKey);
    
    if (low == null || mid == null || upper == null) {
      return;
    }

    // Abstain if the percentiles are co-linear
    if (low >= mid || low >= upper || mid >= upper) {
      return;
    }

    Map<String, Double> output = floatFeatures.get(outputName);

    if (output == null) {
      output = new HashMap<>();
      floatFeatures.put(outputName, output);
    }

    Double outVal = 0.0;
    if (val < low) {
      outVal = 0.0;
    } else if (val < mid) {
      // Interpolate to a value between 0.0 and 0.5
      double denom = mid - low;
      outVal = 0.5 * (val - low) / denom;
    } else if (val < upper) {
      // Interpolate to a value between 0.5 and 1.0
      double denom = upper - mid;
      outVal = 0.5 + 0.5 * (val - mid) / denom;
    } else {
      outVal = 1.0;
    }

    output.put(outputKey, outVal);
  }
}
