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
    private List<TestFile> testResults;
    public List<TestFile> trainAndTest(File mainDirectory) {
        // main method of loading the directories and files, training and testing the model
        trainHamFreq = new HashMap<>();
        trainSpamFreq = new HashMap<>();
        testResults = new ArrayList<>();

        // Training phase
        trainModel(new File(mainDirectory, "train/ham"), trainHamFreq);
        trainModel(new File(mainDirectory, "train/ham2"), trainHamFreq);
        trainModel(new File(mainDirectory, "train/spam"), trainSpamFreq);

        // Testing phase
        testResults.addAll(testModel(new File(mainDirectory, "test/ham"), false));
        testResults.addAll(testModel(new File(mainDirectory, "test/spam"), true));

        return testResults;
    }

    private void trainModel(File directory, Map<String, Integer> freqMap) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                Set<String> words = extractWords(file);
                for (String word : words) {
                    freqMap.put(word, freqMap.getOrDefault(word, 0) + 1);
                }
            }
        }
    }

    private List<TestFile> testModel(File directory, boolean expectedSpam) {
        List<TestFile> testFiles = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                Set<String> words = extractWords(file);
                double spamProbability = calculateSpamProbability(words);
                // Set the actual class based on the expectedSpam parameter
                String actualClass = expectedSpam ? "spam" : "ham";
                testFiles.add(new TestFile(file.getName(), spamProbability, actualClass));
            }
        }
        return testFiles;
    }

    private Set<String> extractWords(File file) {
        Set<String> words = new HashSet<>();
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNext()) {
                String word = scanner.next().toLowerCase();
                // Simple word extraction (can be improved)
                word = word.replaceAll("[^a-zA-Z]", ""); // Remove non-alphabetic characters
                if (!word.isEmpty()) {
                    words.add(word);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return words;
    }

    private double calculateSpamProbability(Set<String> words) {
        double spamLogProbability = 0.0;
        double hamLogProbability = 0.0;

        for (String word : words) {
            int hamCount = trainHamFreq.getOrDefault(word, 0);
            int spamCount = trainSpamFreq.getOrDefault(word, 0);

            // Calculate Pr(W|S) and Pr(W|H)
            double pWS = (spamCount + 1.0) / (spamCount + hamCount + 2.0);
            double pWH = (hamCount + 1.0) / (spamCount + hamCount + 2.0);

            // Convert to log probabilities and add to the total
            spamLogProbability += Math.log(pWS);
            hamLogProbability += Math.log(pWH);
        }

        // Convert back to regular probabilities

        return 1.0 / (1.0 + Math.exp(hamLogProbability - spamLogProbability));
    }

    public double getAccuracy() {
        int numTruePositive = 0;
        int numTrueNegative = 0;

        // Iterate over the test files
        for (TestFile testFile : testResults) {
            // Check if the actual class is 'spam' and predicted class is also 'spam'
            if (testFile.getActualClass().equals("spam") && testFile.getSpamProbability() > 0.5) {
                numTruePositive++; // Increment true positive count
            }
            // Check if the actual class is 'ham' and predicted class is also 'ham'
            else if (testFile.getActualClass().equals("ham") && testFile.getSpamProbability() <= 0.5) {
                numTrueNegative++; // Increment true negative count
            }
        }

        // Calculate accuracy
        int totalFiles = testResults.size();
        return (double) (numTruePositive + numTrueNegative) / totalFiles;
    }

    public double getPrecision() {
        int numTruePositive = 0;
        int numFalsePositive = 0;

        // Iterate over the test files
        for (TestFile testFile : testResults) {
            // Check if the actual class is 'spam' and predicted class is also 'spam'
            if (testFile.getActualClass().equals("spam") && testFile.getSpamProbability() > 0.5) {
                numTruePositive++; // Increment true positive count
            }
            // Check if the actual class is 'ham' but predicted class is 'spam'
            else if (testFile.getActualClass().equals("ham") && testFile.getSpamProbability() > 0.5) {
                numFalsePositive++; // Increment false positive count
            }
        }

        // Calculate precision
        return (double) numTruePositive / (numTruePositive + numFalsePositive);
    }

}