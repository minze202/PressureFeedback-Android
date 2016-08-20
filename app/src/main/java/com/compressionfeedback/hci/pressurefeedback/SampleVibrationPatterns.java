package com.compressionfeedback.hci.pressurefeedback;

import java.util.ArrayList;

public class SampleVibrationPatterns {

    public  ArrayList<NumberPair[]> sampleVibrationPatterns=new ArrayList<>();
    private static int vibrationStrength=25;

    public  void setVibrationStrength(int strength){
        double strengthNormalized=(((double)strength)/100)*25;
        vibrationStrength=(int)strengthNormalized;
        NumberPair[] pattern1={
                new NumberPair(vibrationStrength,10)
        };
        NumberPair[] pattern2={
                new NumberPair(vibrationStrength,5), new NumberPair(vibrationStrength,5)
        };
        NumberPair[] pattern3={
                new NumberPair(vibrationStrength,1), new NumberPair(vibrationStrength,1), new NumberPair(vibrationStrength,1)
        };
        NumberPair[] pattern5={
                new NumberPair(vibrationStrength,15), new NumberPair(vibrationStrength,20)
        };
        sampleVibrationPatterns.clear();
        sampleVibrationPatterns.add(pattern1);
        sampleVibrationPatterns.add(pattern2);
        sampleVibrationPatterns.add(pattern3);
        sampleVibrationPatterns.add(pattern5);
    }

    public  NumberPair[] pattern1={
            new NumberPair(vibrationStrength,10)
    };
    public  NumberPair[] pattern2={
            new NumberPair(vibrationStrength,5), new NumberPair(vibrationStrength,5)
    };
    public  NumberPair[] pattern3={
            new NumberPair(vibrationStrength,1), new NumberPair(vibrationStrength,1), new NumberPair(vibrationStrength,1)
    };
    public  NumberPair[] pattern5={
            new NumberPair(vibrationStrength,15), new NumberPair(vibrationStrength,20)
    };

    {
        sampleVibrationPatterns.add(pattern1);
        sampleVibrationPatterns.add(pattern2);
        sampleVibrationPatterns.add(pattern3);
        sampleVibrationPatterns.add(pattern5);
    }

}
