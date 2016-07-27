package com.compressionfeedback.hci.pressurefeedback;



import java.util.ArrayList;

public class SampleCompressionPatterns {

    public static ArrayList<ArrayList<NumberPair[]>> sampleCompressionPatterns=new ArrayList<>();

    public static NumberPair[] pattern1Weak={
            new NumberPair(1,50), new NumberPair(3,10)
           };
    public static NumberPair[] pattern1Strong={
            new NumberPair(1,101), new NumberPair(3,10)
    };
    public static NumberPair[] pattern2Weak={
            new NumberPair(1,45), new NumberPair(2,5), new NumberPair(3,10)
    };
    public static NumberPair[] pattern2Strong={
            new NumberPair(1,101), new NumberPair(2,5), new NumberPair(3,10)
    };
    public static NumberPair[] pattern3Weak={
            new NumberPair(1,50), new NumberPair(3,25), new NumberPair(1,50), new NumberPair(3,10)
    };
    public static NumberPair[] pattern3Strong={
            new NumberPair(1,101), new NumberPair(3,50), new NumberPair(1,101), new NumberPair(3,10)
    };
    public static NumberPair[] pattern4Weak={
            new NumberPair(1,50), new NumberPair(3,25), new NumberPair(2,3), new NumberPair(3,10)
    };
    public static NumberPair[] pattern4Strong={
            new NumberPair(1,101), new NumberPair(3,50), new NumberPair(2,3), new NumberPair(3,10)
    };
    public static NumberPair[] pattern5Weak={
            new NumberPair(1,10), new NumberPair(1,20), new NumberPair(1,30), new NumberPair(1,40), new NumberPair(1,50), new NumberPair(3,10)
    };
    public static NumberPair[] pattern5Strong={
            new NumberPair(1,20), new NumberPair(1,40), new NumberPair(1,60), new NumberPair(1,80), new NumberPair(1,101), new NumberPair(3,10)
    };
    static {
        ArrayList<NumberPair[]> weakPatterns=new ArrayList<>();
        weakPatterns.add(pattern1Weak);
        weakPatterns.add(pattern2Weak);
        weakPatterns.add(pattern3Weak);
        weakPatterns.add(pattern4Weak);
        weakPatterns.add(pattern5Weak);
        ArrayList<NumberPair[]> strongPatterns=new ArrayList<>();
        strongPatterns.add(pattern1Strong);
        strongPatterns.add(pattern2Strong);
        strongPatterns.add(pattern3Strong);
        strongPatterns.add(pattern4Strong);
        strongPatterns.add(pattern5Strong);
        sampleCompressionPatterns.add(weakPatterns);
        sampleCompressionPatterns.add(strongPatterns);
    }

}
