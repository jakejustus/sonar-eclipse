/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.core.internal.remote;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Actually this is an implementation of heuristic algorithm - magic happens here.
 *
 * @author Evgeny Mandrikov
 */
public class SimpleSourceCodeDiffEngine {
  private static final Logger LOG = LoggerFactory.getLogger(SimpleSourceCodeDiffEngine.class);

  public SourceCodeDiff diff(String local, String remote) {
    return diff(split(local), split(remote));
  }

  /**
   * {@inheritDoc}
   */
  public SourceCodeDiff diff(String[] local, String remote[]) {
    SourceCodeDiff result = new SourceCodeDiff();

    int[] hashCodes = getHashCodes(local);
    // Works for O(S*L) time, where S - number of lines on server and L - number of lines in working copy.
    for (int i = 0; i < remote.length; i++) {
      int originalLine = i + 1;
      int newLine = internalMatch(remote[i], hashCodes, originalLine);
      if (newLine != -1) {
        result.map(originalLine, newLine);
      }
    }

    return result;
  }

  /**
   * Currently this method just compares hash codes (see {@link #getHashCode(String)}).
   *
   * @return -1 if not found
   */
  private int internalMatch(String originalSourceLine, int[] hashCodes, int originalLine) {
    int newLine = -1;
    int originalHashCode = getHashCode(originalSourceLine);
    // line might not exists in working copy
    if ((originalLine - 1) < hashCodes.length
      && hashCodes[originalLine - 1] == originalHashCode) {
      newLine = originalLine;
    }
    for (int i = 0; i < hashCodes.length; i++) {
      if (hashCodes[i] == originalHashCode) {
        if ((newLine != -1) && (newLine != originalLine)) {
          // may be more than one match, but we take into account only first
          LOG.debug("Found more than one match for line '{}'", originalSourceLine);
          break;
        }
        newLine = i + 1;
      }
    }
    return newLine;
  }

  public static String[] split(String text) {
    return StringUtils.splitPreserveAllTokens(text, '\n');
  }

  /**
   * Returns hash code for specified string after removing whitespaces.
   *
   * @param str string
   * @return hash code for specified string after removing whitespaces
   */
  static int getHashCode(String str) {
    if (str == null) {
      return 0;
    }
    return StringUtils.deleteWhitespace(str).hashCode();
  }

  /**
   * Returns hash codes for specified strings after removing whitespaces.
   *
   * @param str strings
   * @return hash codes for specified strings after removing whitespaces
   */
  private static int[] getHashCodes(String[] str) {
    int[] hashCodes = new int[str.length];
    for (int i = 0; i < str.length; i++) {
      hashCodes[i] = getHashCode(str[i]);
    }
    return hashCodes;
  }

}
