package com.spamdetector.util;

import com.spamdetector.domain.TestFile;

import java.io.*;
import java.util.*;

/**
 * TODO: This class will be implemented by you
 * You may create more methods to help you organize your strategy and make your code more readable
 */
public class SpamDetector {
    private Map<String, Integer> trainHamFreq;
    private Map<String, Integer> trainSpamFreq;
    private Map<String, Double> prSwiMap;
    private List<TestFile> testResults;
    private int totalHamFiles;
    private int totalSpamFiles;

    public List<TestFile> trainAndTest(File mainDirectory) {
        trainHamFreq = new HashMap<>();
        trainSpamFreq = new HashMap<>();
        prSwiMap = new HashMap<>();
        testResults = new ArrayList<>();
        totalHamFiles = 0;
        totalSpamFiles = 0;

        //We traverse the ham and spam Files and get the trainHamFreq and trainSpamFreq
        //trainHamFreq contains a map of words and the number of files containing that word in the ham folder
        //trainSpamFreq contains a map of words and the number of files containing that word in the spam folder
        trainModel(new File(mainDirectory, "train/ham"), trainHamFreq, false);
        trainModel(new File(mainDirectory, "train/ham2"), trainHamFreq, false);
        trainModel(new File(mainDirectory, "train/spam"), trainSpamFreq, true);

        //Calc Pr(S|Wi)
        calculatePrSwi();

        //Testing
        testResults.addAll(testModel(new File(mainDirectory, "test/ham"), false));
        testResults.addAll(testModel(new File(mainDirectory, "test/spam"), true));

        return testResults;
    }

    private void trainModel(File directory, Map<String, Integer> freqMap, boolean isSpam) {
        File[] files = directory.listFiles();
        //check if files not empty
        if (files != null) {
            Set<String> wordsInCurrentFile = new HashSet<>();
            for (File file : files) {
                wordsInCurrentFile.clear(); //Clear set
                extractWords(file, wordsInCurrentFile);
                for (String word : wordsInCurrentFile) {
                    freqMap.put(word, freqMap.getOrDefault(word, 0) + 1);
                }
            }
            //Update number files
            if (isSpam) {
                totalSpamFiles += 1;
            } else {
                totalHamFiles += 1;
            }
        }
    }

    private void calculatePrSwi() {
        prSwiMap = new HashMap<>();
        Set<String> allWords = new HashSet<>();
        allWords.addAll(trainHamFreq.keySet());
        allWords.addAll(trainSpamFreq.keySet());
        //iterate over all the words and find prWi|S and prWi|H
        for (String word : allWords) {
            double prWiGivenSpam = calculatePrWiGivenClass(word, trainSpamFreq, totalSpamFiles);
            double prWiGivenHam = calculatePrWiGivenClass(word, trainHamFreq, totalHamFiles);
               //given both probs, we find prSwi
            double prSwi = prWiGivenSpam / (prWiGivenSpam + prWiGivenHam);
            prSwiMap.put(word, prSwi);
        }
    }

    //this is calculation for prWi|S and prWi|H
    private double calculatePrWiGivenClass(String word, Map<String, Integer> freqMap, int totalFiles) {
        int filesContainingWord = freqMap.getOrDefault(word, 0);
        return (double) filesContainingWord / totalFiles;
    }

    private List<TestFile> testModel(File directory, boolean expectedSpam) {
        List<TestFile> testFiles = new ArrayList<>();
        File[] files = directory.listFiles();
        //files not empty
        if (files != null) {
            for (File file : files) {
                Set<String> words = new HashSet<>();
                //get all the words from file and put in set
                extractWords(file, words);
                //use given set of words to calc spamProb of file
                double spamProbability = calculateSpamProbability(words);
                String actualClass = expectedSpam ? "spam" : "ham";
                testFiles.add(new TestFile(file.getName(), spamProbability, actualClass));
            }
        }
        return testFiles;
    }
//extracts all the words
    private void extractWords(File file, Set<String> words) {
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNext()) {
                String word = scanner.next().toLowerCase();
                word = word.replaceAll("[^a-zA-Z]", "");
                if (!word.isEmpty()) {
                    words.add(word);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private double calculateSpamProbability(Set<String> words) {
        double eta = 0.0;

        for (String word : words) {
            double prSwi = prSwiMap.getOrDefault(word, 1.0 / (totalSpamFiles + 2));

            if (Double.isNaN(prSwi) || Double.isInfinite(prSwi)) {
                continue;
            }

            //This part is to deal with log(0) or log(1)
            //log(0) is undefined and causes issues
            //as well as log(1) which is 0. It also causes errors
            //Change the values to something very near to 1 or 0
            if (prSwi == 0) {
                prSwi = 0.000000001;
            } else if (prSwi == 1) {
                prSwi = 0.999999999;
            }

            //Calculate the eta
            eta += Math.log(1 - prSwi) - Math.log(prSwi);
        }

        //Calculate spamProbability
        double probability = 1 / (1 + Math.exp(eta));
        //Return probability
        return probability;
    }

    public double getAccuracy() {
        double numTruePositive = 0;
        double numTrueNegative = 0;

        //Iterate test files
        for (TestFile testFile : testResults) {
            // Check actual class = 'spam' & check spam probability > 0.5
            if (testFile.getActualClass().equals("spam") && testFile.getSpamProbability() > 0.5) {
                numTruePositive++;
            }
            // Check actual class is 'ham' & check spam probability <= 0.5
            else if (testFile.getActualClass().equals("ham") && testFile.getSpamProbability() <= 0.5) {
                numTrueNegative++;
            }
        }

        // Calculate accuracy
        int totalFiles = testResults.size();
        return (numTruePositive + numTrueNegative) / totalFiles;
    }

    public double getPrecision() {
        double numTruePositive = 0;
        double numFalsePositive = 0;

        //Iterate test files
        for (TestFile testFile : testResults) {
            //Check actual class = 'spam' & check spam probability > 0.5
            if (testFile.getActualClass().equals("spam") && testFile.getSpamProbability() > 0.5) {
                numTruePositive++;
            }
            //Check actual class = 'ham' & check spam probability > 0.5
            else if (testFile.getActualClass().equals("ham") && testFile.getSpamProbability() > 0.5) {
                numFalsePositive++;
            }
        }

        // Calculate precision
        return numTruePositive / (numTruePositive + numFalsePositive);
    }

}