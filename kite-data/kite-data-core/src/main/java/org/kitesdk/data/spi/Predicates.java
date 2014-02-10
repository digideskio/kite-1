/*
 * Copyright 2013 Cloudera Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kitesdk.data.spi;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.BoundType;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.collect.Ranges;
import java.util.Set;
import javax.annotation.Nullable;

public abstract class Predicates {
  @SuppressWarnings("unchecked")
  public static <T> Exists<T> exists() {
    return (Exists<T>) Exists.INSTANCE;
  }

  public static <T> In<T> in(Set<T> set) {
    return new In<T>(set);
  }

  public static <T> In<T> in(T... set) {
    return new In<T>(set);
  }

  // This should be a method on Range, like In#transform.
  // Unfortunately, Range is final so we will probably need to re-implement it.
  public static <S extends Comparable, T extends Comparable>
  Range<T> transformClosed(Range<S> range, Function<? super S, T> function) {
    if (range.hasLowerBound()) {
      if (range.hasUpperBound()) {
        return Ranges.closed(
            function.apply(range.lowerEndpoint()),
            function.apply(range.upperEndpoint()));
      } else {
        return Ranges.atLeast(function.apply(range.lowerEndpoint()));
      }
    } else if (range.hasUpperBound()) {
      return Ranges.atMost(function.apply(range.upperEndpoint()));
    } else {
      return (Range<T>) Ranges.<T>all();
    }
  }

  public static <S extends Comparable, T extends Comparable<T>>
  Range<T> transformClosedConservative(Range<S> range, Function<S, T> func,
                              DiscreteDomain<T> domain) {
    if (range.hasLowerBound()) {
      S lower = range.lowerEndpoint();
      // TODO: make sure not checking the bottom applies with generics
      // the special case, (a, _] and apply(a) == a is handled by skipping a
      T afterLower = domain.next(func.apply(lower));
      if (afterLower != null) {
        if (range.hasUpperBound()) {
          S upper = range.upperEndpoint();
          T upperImage = func.apply(upper);
          // TODO: reimplement upper.equals(upperImage) check for generics
          // meaning: at the endpoint
          if (upper.equals(upperImage) && range.upperBoundType() == BoundType.CLOSED) {
            // include upper
            return Ranges.closed(afterLower, upperImage);
          } else {
            T beforeUpper = domain.previous(upperImage);
            if (afterLower.compareTo(beforeUpper) <= 0) {
              return Ranges.closed(afterLower, beforeUpper);
            }
          }
        } else {
          return Ranges.atLeast(afterLower);
        }
      }
    } else if (range.hasUpperBound()) {
      S upper = range.upperEndpoint();
      T upperImage = func.apply(upper);
      if (upper.equals(upperImage) && range.upperBoundType() == BoundType.CLOSED) {
        // include upper
        return Ranges.atMost(upperImage);
      } else {
        T beforeUpper = domain.previous(upperImage);
        if (beforeUpper != null) {
          return Ranges.atMost(beforeUpper);
        }
      }
    }
    return null;
  }

  public static <T extends Comparable>
  Range<T> adjustClosed(Range<T> range, DiscreteDomain<T> domain) {
    // adjust to a closed range to avoid catching extra keys
    if (range.hasLowerBound()) {
      T lower = range.lowerEndpoint();
      if (BoundType.OPEN == range.lowerBoundType()) {
        lower = domain.next(lower);
      }
      if (range.hasUpperBound()) {
        T upper = range.upperEndpoint();
        if (BoundType.OPEN == range.upperBoundType()) {
          upper = domain.previous(upper);
        }
        return Ranges.closed(lower, upper);
      } else {
        return Ranges.atLeast(lower);
      }
    } else if (range.hasUpperBound()) {
      T upper = range.upperEndpoint();
      if (BoundType.OPEN == range.upperBoundType()) {
        upper = domain.previous(upper);
      }
      return Ranges.atMost(upper);
    } else {
      throw new IllegalArgumentException("Invalid range: no endpoints");
    }
  }

  public static class Exists<T> implements Predicate<T> {
    public static final Exists INSTANCE = new Exists();

    private Exists() {
    }

    @Override
    public boolean apply(@Nullable T value) {
      return (value != null);
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this).toString();
    }
  }

  public static class In<T> implements Predicate<T> {
    // ImmutableSet entries are non-null
    private final ImmutableSet<T> set;

    public In(Iterable<T> values) {
      this.set = ImmutableSet.copyOf(values);
      Preconditions.checkArgument(set.size() > 0, "No values to match");
    }

    public In(T... values) {
      this.set = ImmutableSet.copyOf(values);
    }

    @Override
    public boolean apply(@Nullable T test) {
      // Set#contains may throw NPE, depending on implementation
      return (test != null) && set.contains(test);
    }

    public In<T> filter(Predicate<? super T> predicate) {
      try {
        return new In<T>(Iterables.filter(set, predicate));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Filter predicate produces empty set", e);
      }
    }

    public <V> In<V> transform(Function<? super T, V> function) {
      return new In<V>(Iterables.transform(set, function));
    }

    Set<T> getSet() {
      return set;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      return Objects.equal(set, ((In) o).set);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(set);
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this).add("set", set).toString();
    }
  }
}
