package com.compressionfeedback.hci.pressurefeedback;



import java.util.ArrayList;

public class SampleCompressionPatterns {

    private   ArrayList<ArrayList<NumberPair[]>> sampleCompressionPatterns=new ArrayList<>();
    private  int weakPressure=50;
    private  int strongPressure=100;

    public  void changePressure(int pressure) {
        strongPressure = pressure;
        weakPressure = strongPressure / 2;
        NumberPair[] pattern1Weak = {
                new NumberPair(1, weakPressure), new NumberPair(3, 10)
        };
        NumberPair[] pattern1Strong = {
                new NumberPair(1, strongPressure), new NumberPair(3, 10)
        };
        NumberPair[] pattern2Weak = {
                new NumberPair(1, weakPressure), new NumberPair(2, 3), new NumberPair(3, 10)
        };
        NumberPair[] pattern2Strong = {
                new NumberPair(1, strongPressure), new NumberPair(2, 3), new NumberPair(3, 10)
        };
        NumberPair[] pattern3Weak = {
                new NumberPair(1, weakPressure), new NumberPair(3, weakPressure / 2), new NumberPair(1, weakPressure), new NumberPair(3, 10)
        };
        NumberPair[] pattern3Strong = {
                new NumberPair(1, strongPressure), new NumberPair(3, strongPressure / 2), new NumberPair(1, strongPressure), new NumberPair(3, 10)
        };
        NumberPair[] pattern5Weak = {
                new NumberPair(1, weakPressure / 5), new NumberPair(1, (weakPressure / 5) * 2), new NumberPair(1, (weakPressure / 5) * 3), new NumberPair(1, (weakPressure / 5) * 4), new NumberPair(1, weakPressure), new NumberPair(3, 10)
        };
        NumberPair[] pattern5Strong = {
                new NumberPair(1, (strongPressure / 5)), new NumberPair(1, (strongPressure / 5) * 2), new NumberPair(1, (strongPressure / 5) * 3), new NumberPair(1, (strongPressure / 5) * 4), new NumberPair(1, strongPressure), new NumberPair(3, 10)
        };
        ArrayList<NumberPair[]> weakPatterns = new ArrayList<>();
        weakPatterns.add(pattern1Weak);
        weakPatterns.add(pattern2Weak);
        weakPatterns.add(pattern3Weak);
        weakPatterns.add(pattern5Weak);
        ArrayList<NumberPair[]> strongPatterns = new ArrayList<>();
        strongPatterns.add(pattern1Strong);
        strongPatterns.add(pattern2Strong);
        strongPatterns.add(pattern3Strong);
        strongPatterns.add(pattern5Strong);
        sampleCompressionPatterns.clear();
        sampleCompressionPatterns.add(weakPatterns);
        sampleCompressionPatterns.add(strongPatterns);
    }

    public  NumberPair[] pattern1Weak={
            new NumberPair(1,weakPressure), new NumberPair(3,10)
           };
    public  NumberPair[] pattern1Strong={
            new NumberPair(1,strongPressure), new NumberPair(3,10)
    };
    public  NumberPair[] pattern2Weak={
            new NumberPair(1,weakPressure), new NumberPair(2,5), new NumberPair(3,10)
    };
    public  NumberPair[] pattern2Strong={
            new NumberPair(1,strongPressure), new NumberPair(2,5), new NumberPair(3,10)
    };
    public  NumberPair[] pattern3Weak={
            new NumberPair(1,weakPressure), new NumberPair(3,weakPressure/2), new NumberPair(1,weakPressure), new NumberPair(3,10)
    };
    public  NumberPair[] pattern3Strong={
            new NumberPair(1,strongPressure), new NumberPair(3,strongPressure/2), new NumberPair(1,strongPressure), new NumberPair(3,10)
    };
    public  NumberPair[] pattern5Weak={
            new NumberPair(1,weakPressure/5), new NumberPair(1,(weakPressure/5)*2), new NumberPair(1,(weakPressure/5)*3), new NumberPair(1,(weakPressure/5)*4), new NumberPair(1,weakPressure), new NumberPair(3,10)
    };
    public  NumberPair[] pattern5Strong={
            new NumberPair(1,(strongPressure/5)), new NumberPair(1,(strongPressure/5)*2), new NumberPair(1,(strongPressure/5)*3), new NumberPair(1,(strongPressure/5)*4), new NumberPair(1,strongPressure), new NumberPair(3,10)
    };
     {
        ArrayList<NumberPair[]> weakPatterns=new ArrayList<>();
        weakPatterns.add(pattern1Weak);
        weakPatterns.add(pattern2Weak);
        weakPatterns.add(pattern3Weak);
        weakPatterns.add(pattern5Weak);
        ArrayList<NumberPair[]> strongPatterns=new ArrayList<>();
        strongPatterns.add(pattern1Strong);
        strongPatterns.add(pattern2Strong);
        strongPatterns.add(pattern3Strong);
        strongPatterns.add(pattern5Strong);
        sampleCompressionPatterns.add(weakPatterns);
        sampleCompressionPatterns.add(strongPatterns);
    }

    public ArrayList<ArrayList<NumberPair[]>> getSampleCompressionPatterns() {
        return sampleCompressionPatterns;
    }

    public int getStrongPressure() {
        return strongPressure;
    }
}
