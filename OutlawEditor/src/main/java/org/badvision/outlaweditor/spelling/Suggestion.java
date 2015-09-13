/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
package org.badvision.outlaweditor.spelling;

import static org.badvision.outlaweditor.data.DataUtilities.rankMatch;

public class Suggestion implements Comparable<Suggestion> {

    public String original;
    public String word;
    public double similarity;
    private double similarityRank = -1;

    public String getWord() {
        return word;
    }

    public double getSimilarity() {
        return similarity;
    }

    @Override
    public int compareTo(Suggestion o) {
        if (similarity == o.similarity) {
            
            double rank1 = getSimilarityRank();
            double rank2 = o.getSimilarityRank();
            if (rank1 == rank2) {
                return (word.compareTo(o.word));
            } else {
                // Normalize result to -1, 0 or 1 so there is no rounding issues!
                return (int) Math.signum(rank2 - rank1);
            }
        }
        return (int) Math.signum(similarity - o.similarity);
    }

    private double getSimilarityRank() {
        if (similarityRank < 0) {
            similarityRank = rankMatch(word, original, 3) + rankMatch(word, original, 2);
        }
        return similarityRank;
    }
}
