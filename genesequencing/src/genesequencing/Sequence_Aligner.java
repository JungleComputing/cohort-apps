package genesequencing;

import neobio.alignment.*;
import java.io.*;

public class Sequence_Aligner implements AlignmentAlgorithms, Serializable {
    public static String computeAlignment(String alignmentAlgorithm, Reader s1,
            Reader s2, ScoringScheme scoringScheme) {
        PairwiseAlignmentAlgorithm algorithm = null;
        if (alignmentAlgorithm.equals(SMITH_WATERMAN)) {
            algorithm = new SmithWaterman();
        } else if (alignmentAlgorithm.equals(NEEDLEMAN_WUNSCH)) {
            algorithm = new NeedlemanWunsch();
        } else if (alignmentAlgorithm.equals(CROCHEMORE_LOCAL)) {
            algorithm = new CrochemoreLandauZivUkelsonLocalAlignment();
        } else if (alignmentAlgorithm.equals(CROCHEMORE_GLOBAL)) {
            algorithm = new CrochemoreLandauZivUkelsonGlobalAlignment();
        } else {
            throw new Error("Non-existant alignment algorithm selected: "
                    + alignmentAlgorithm);
        }

        // set scoring scheme
        algorithm.setScoringScheme(scoringScheme);

        try {
            algorithm.loadSequences(s1, s2);
            s1.close();
            s2.close();

            // return the actual alignment
            PairwiseAlignment alignment = algorithm.getPairwiseAlignment();
            String result = alignment.toString(); 
            algorithm.unloadSequences();
            
            return result;
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public static int computeAlignmentScore(String alignmentAlgorithm,
            Reader s1, Reader s2, ScoringScheme scoringScheme) throws Exception {
        PairwiseAlignmentAlgorithm algorithm = null;
        if (alignmentAlgorithm.equals(SMITH_WATERMAN)) {
            algorithm = new SmithWaterman();
        } else if (alignmentAlgorithm.equals(NEEDLEMAN_WUNSCH)) {
            algorithm = new NeedlemanWunsch();
        } else if (alignmentAlgorithm.equals(CROCHEMORE_LOCAL)) {
            algorithm = new CrochemoreLandauZivUkelsonLocalAlignment();
        } else if (alignmentAlgorithm.equals(CROCHEMORE_GLOBAL)) {
            algorithm = new CrochemoreLandauZivUkelsonGlobalAlignment();
        } else {
            throw new Exception("Non-existant alignment algorithm selected: "
                    + alignmentAlgorithm);
        }

        // set scoring scheme
        algorithm.setScoringScheme(scoringScheme);

        algorithm.loadSequences(s1, s2);

        //align the sequences and produce the score
        int score = algorithm.getScore();

        algorithm.unloadSequences();

        // close files
        s1.close();
        s2.close();

        return score;
    }
}
