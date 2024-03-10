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
        //Keep count of all the words in hamFiles and the number of hamFiles that contain the word
        trainHamFreq = new HashMap<>();
        //Keep count of all the words in spamFiles and the number of spamFiles that contain the word
        trainSpamFreq = new HashMap<>();
        //A map that keeps track of the Pr(S|Wi) of
        prSwiMap = new HashMap<>();
        //An ArrayList that returns the fileName,spamProbability, and the actualClass of all the
        //files in test/spam and test/ham
        testResults = new ArrayList<>();
        //Used to keep track of the number of hamFiles
        totalHamFiles = 0;
        //Used to keep track of the number of spamFiles
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
        //Check if files not empty
        if (files != null) {
            Set<String> wordsInCurrentFile = new HashSet<>();
            for (File file : files) {
                //Clear the set so the words from the previous file do
                //not affect the next file
                //This makes it so that each word is only counted once per file
                wordsInCurrentFile.clear(); //Clear set
                //Get the words from the file and put them in the set
                extractWords(file, wordsInCurrentFile);
                //Iterate over each word in the set and
                //increase the frequency count of the of that word in the freqMap
                for (String word : wordsInCurrentFile) {
                    freqMap.put(word, freqMap.getOrDefault(word, 0) + 1);
                }
            }
            //Update number files
            //Keep track of totalSpam and totalHam files in order to use them later
            //to calculate prWi|S and prWi|H
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
        //Add all the unique words from the trainHamFreq and trainSpamFreq
        //to the allWords Set
        allWords.addAll(trainHamFreq.keySet());
        allWords.addAll(trainSpamFreq.keySet());

        //Iterate over all the words in the Ste and find prWi|S and prWi|H
        for (String word : allWords) {
            //Calculate for prWi|S and prWi|H using the calculatePrWiGivenClass function
            double prWiGivenSpam = calculatePrWiGivenClass(word, trainSpamFreq, totalSpamFiles);
            double prWiGivenHam = calculatePrWiGivenClass(word, trainHamFreq, totalHamFiles);
            //Given both probabilities, we find prSwi
            double prSwi = prWiGivenSpam / (prWiGivenSpam + prWiGivenHam);
            //Put the probability into the prSwiMap
            prSwiMap.put(word, prSwi);
        }
    }

    //This is calculation for prWi|S and prWi|H
    private double calculatePrWiGivenClass(String word, Map<String, Integer> freqMap, int totalFiles) {
        int filesContainingWord = freqMap.getOrDefault(word, 0);
        //Type cast into double so decimals are not lost
        return (double) filesContainingWord / totalFiles;
    }

    private List<TestFile> testModel(File directory, boolean expectedSpam) {
        List<TestFile> testFiles = new ArrayList<>();
        //Get all the files from the directory
        File[] files = directory.listFiles();
        //Check if files are not empty
        if (files != null) {
            for (File file : files) {
                Set<String> words = new HashSet<>();
                //Get all the words from file and put in set
                extractWords(file, words);
                //Use given set of words to calc spamProb of file
                double spamProbability = calculateSpamProbability(words);
                String actualClass = expectedSpam ? "spam" : "ham";
                //Add the filename, the spamProbability and the actualclass of the file based on whether
                //expectedSpam is false or true to the List testfiles
                testFiles.add(new TestFile(file.getName(), spamProbability, actualClass));
            }
        }
        return testFiles;
    }
//extracts all the words
    private void extractWords(File file, Set<String> words) {
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNext()) {
                //Convert the word to lowercase
                String word = scanner.next().toLowerCase();
                //Use regex to to remove all non-alphabetic words
                word = word.replaceAll("[^a-zA-Z]", "");
                if (!word.isEmpty()) {
                    words.add(word);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    //In this function the SpamProbability for each file is calculated using the given formula
    //
    private double calculateSpamProbability(Set<String> words) {
        double eta = 0.0;

        for (String word : words) {
            double prSwi = prSwiMap.getOrDefault(word, 1.0 / (totalSpamFiles + 2));

            //Skip if prSWi is NaN or Infinite
            //It causes error
            if (Double.isNaN(prSwi) || Double.isInfinite(prSwi)) {
                continue;
            }

            //This part is to deal with log(0)
            //log(0) is undefined and causes issues
            if (prSwi == 0) {
                prSwi = 0.000000001;
            }

            //Calculate the eta
            eta += Math.log(1 - prSwi) - Math.log(prSwi);
        }

        //Calculate spamProbability
        double probability = 1 / (1 + Math.exp(eta));
        //Return probability
        return probability;
    }

    //Use the formula given for accuracy to calculate the accuracy
    //Formula is numTruePositives+numTrueNegative/numFiles
    //Used 0.5 as a threshold to compare
    public double getAccuracy() {
        double numTruePositive = 0;
        double numTrueNegative = 0;

        //Iterate test files
        for (TestFile testFile : testResults) {
            // Check actual class = 'spam' & check spam probability > 0.5
            if (testFile.getActualClass().equalsIgnoreCase("spam") && testFile.getSpamProbability() > 0.5) {
                numTruePositive++;
            }
            // Check actual class is 'ham' & check spam probability <= 0.5
            else if (testFile.getActualClass().equalsIgnoreCase("ham") && testFile.getSpamProbability() <= 0.5) {
                numTrueNegative++;
            }
        }

        // Calculate and return the accuracy
        int totalFiles = testResults.size();//Get the total number of files
        return (numTruePositive + numTrueNegative) / totalFiles;
    }

    //Use the formula given for precision to calculate the precision
    //Formula is numTruePositives/numTruePositives+numFalseNegative
    //Used 0.5 as a threshold to compare
    public double getPrecision() {
        double numTruePositive = 0;
        double numFalsePositive = 0;

        //Iterate test files
        for (TestFile testFile : testResults) {
            //Check actual class = 'spam' & check spam probability > 0.5
            if (testFile.getActualClass().equalsIgnoreCase("spam") && testFile.getSpamProbability() > 0.5) {
                numTruePositive++;
            }
            //Check actual class = 'ham' & check spam probability > 0.5
            else if (testFile.getActualClass().equalsIgnoreCase("ham") && testFile.getSpamProbability() > 0.5) {
                numFalsePositive++;
            }
        }

        //Calculate and return the precision
        return numTruePositive / (numTruePositive + numFalsePositive);
    }

}