package de.upb.crc901.reduction.single.confusion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import jaicore.basic.sets.SetUtil;
import jaicore.ml.WekaUtil;
import jaicore.ml.classification.multiclass.reduction.MCTreeNodeReD;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;

public class ConfusionBasedAlgorithm {

	public MCTreeNodeReD buildClassifier(Instances data, final Collection<String> pClassifierNames) throws Exception {

		System.out.println("START: " + data.relationName());
		int seed = 0;
		
		Map<String, double[][]> confusionMatrices = new HashMap<>();
		int numClasses = data.numClasses();
		System.out.println("Computing confusion matrices ...");
		for (int i = 0; i < 10; i++) {
			List<Instances> split = WekaUtil.getStratifiedSplit(data, new Random(seed), .7f);
			// MulticlassEvaluator eval = new MulticlassEvaluator(new Random(seed));

			/* compute confusion matrices for each classifier */
//			System.out.println("Iteration " + i);
			for (String classifier : pClassifierNames) {
				// System.out.println("\t" + classifier + " ...");
				try {
					Classifier c = AbstractClassifier.forName(classifier, null);
					c.buildClassifier(split.get(0));
					Evaluation eval = new Evaluation(split.get(0));
					eval.evaluateModel(c, split.get(1));
					if (!confusionMatrices.containsKey(classifier))
						confusionMatrices.put(classifier, new double[numClasses][numClasses]);
						
					double[][] currentCM = confusionMatrices.get(classifier);
					double[][] addedCM = eval.confusionMatrix();
					
					for (int j = 0; j < numClasses; j++) {
						for (int k = 0; k < numClasses; k++) {
							currentCM[j][k] += addedCM[j][k];
						}
					}
				} catch (Throwable e) {
//					System.err.println(e.getClass().getName() + ": " + e.getMessage());
				}
			}
		}
//		confusionMatrices.keySet().forEach(k -> {
//			System.out.println(k);
//			for (int i = 0; i < confusionMatrices.get(k).length; i++)
//				System.out.println(Arrays.toString(confusionMatrices.get(k)[i]));
//		});
		System.out.println("done");

		/* compute zero-conflict sets for each classifier */
		Map<String, Collection<Collection<Integer>>> zeroConflictSets = new HashMap<>();
		for (String classifier : confusionMatrices.keySet()) {
			zeroConflictSets.put(classifier, getZeroConflictSets(confusionMatrices.get(classifier)));
		}

		/* greedily identify the best left and right pair (that make least mistakes) */
		Collection<List<String>> classifierPairs = SetUtil.cartesianProduct(confusionMatrices.keySet(), 2);
		String bestLeft = null;
		String bestRight = null;
		String bestInner = null;
		Collection<Integer> bestLeftClasses = null;
		Collection<Integer> bestRightClasses = null;
//		int numPair = 0;
		for (List<String> classifierPair : classifierPairs) {
//			numPair++;
			String c1 = classifierPair.get(0);
			String c2 = classifierPair.get(1);
//			 System.out.println("\tConsidering " + c1 + "/" + c2 + "(" + numPair + "/" + classifierPairs.size() + ")");
//			double[][] cm1 = confusionMatrices.get(c1);
//			double[][] cm2 = confusionMatrices.get(c2);
			Collection<Collection<Integer>> z1 = zeroConflictSets.get(c1);
			Collection<Collection<Integer>> z2 = zeroConflictSets.get(c2);

			/* create candidate split */
			int sizeOfBestCombo = 0;
			for (Collection<Integer> zeroSet1 : z1) {
				for (Collection<Integer> zeroSet2 : z2) {
					Collection<Integer> coveredClassesOfThisPair = SetUtil.union(zeroSet1, zeroSet2);
					if (coveredClassesOfThisPair.size() > sizeOfBestCombo) {
						bestLeft = c1;
						bestRight = c2;
						sizeOfBestCombo = coveredClassesOfThisPair.size();
						bestLeftClasses = zeroSet1;
						bestRightClasses = zeroSet2;
					}
				}
			}
		}

		/* greedily complete the best candidates */
		double[][] cm1 = confusionMatrices.get(bestLeft);
		double[][] cm2 = confusionMatrices.get(bestRight);
		for (int cId = 0; cId < numClasses; cId++) {
			if (!bestLeftClasses.contains(cId) && !bestRightClasses.contains(cId)) {

				/* compute effect of adding this class to the respective clusters */
				Collection<Integer> newBestZ1 = new ArrayList<>(bestLeftClasses);
				newBestZ1.add(cId);
				int p1 = getPenaltyOfCluster(newBestZ1, cm1);
				Collection<Integer> newBestZ2 = new ArrayList<>(bestRightClasses);
				newBestZ2.add(cId);
				int p2 = getPenaltyOfCluster(newBestZ2, cm2);

				if (p1 < p2) {
					bestLeftClasses = newBestZ1;
				} else
					bestRightClasses = newBestZ2;
			}
		}
		int p1 = getPenaltyOfCluster(bestLeftClasses, cm1);
		int p2 = getPenaltyOfCluster(bestRightClasses, cm2);

		/* create the split problem */
		Map<String, String> classMap = new HashMap<>();
		for (int i1 : bestLeftClasses) {
			classMap.put(data.classAttribute().value(i1), "l");
		}
		for (int i2 : bestRightClasses) {
			classMap.put(data.classAttribute().value(i2), "r");
		}
		Instances newData = WekaUtil.getRefactoredInstances(data, classMap);
		List<Instances> binaryInnerSplit = WekaUtil.getStratifiedSplit(newData, new Random(seed), .7f);

		/* now identify the classifier that can best separate these two clusters */
		int leastSeenMistakes = Integer.MAX_VALUE;
		for (String classifier : pClassifierNames) {
			try {
//				 System.out.println("\t\tConsidering " + bestLeft + "/" + bestRight + "/" + classifier);
				Classifier c = AbstractClassifier.forName(classifier, null);

				c.buildClassifier(binaryInnerSplit.get(0));
				Evaluation eval = new Evaluation(newData);
				eval.evaluateModel(c, binaryInnerSplit.get(1));
				int mistakes = (int) eval.incorrect();
				int overallMistakes = p1 + p2 + mistakes;
//				System.out.println(overallMistakes);
				if (overallMistakes < leastSeenMistakes) {
					leastSeenMistakes = overallMistakes;
					 System.out.println("New best system: " + bestLeft + "/" + bestRight + "/" + classifier + " with " + leastSeenMistakes);
					bestInner = classifier;
				}
			} catch (Exception e) {
				System.err.println(e.getClass() + ": " + e.getMessage());
			}
		}
		if (bestInner == null)
			throw new IllegalStateException("No best inner has been chosen!");


		/* now create MCTreeNode with choices */
		MCTreeNodeReD tree = new MCTreeNodeReD(bestInner, bestLeftClasses.stream().map(i -> data.classAttribute().value(i)).collect(Collectors.toList()), bestLeft,
				bestRightClasses.stream().map(i -> data.classAttribute().value(i)).collect(Collectors.toList()), bestRight);
		tree.buildClassifier(data);
		return tree;
	}


	private int getLeastConflictingClass(double[][] confusionMatrix, Collection<Integer> blackList) {

		/* compute least conflicting class */
		int leastConflictingClass = -1;
		int leastKnownScore = Integer.MAX_VALUE;
		for (int i = 0; i < confusionMatrix.length; i++) {
			if (blackList.contains(i))
				continue;
			int sum = 0;
			for (int j = 0; j < confusionMatrix.length; j++) {
				if (i != j)
					sum += confusionMatrix[i][j];
			}
			if (sum < leastKnownScore) {
				leastKnownScore = sum;
				leastConflictingClass = i;
			}
		}
		return leastConflictingClass;
	}

	private Collection<Collection<Integer>> getZeroConflictSets(double[][] confusionMatrix) {
		Collection<Integer> blackList = new ArrayList<>();
		Collection<Collection<Integer>> partitions = new ArrayList<>();

		int leastConflictingClass = -1;
		do {
			leastConflictingClass = getLeastConflictingClass(confusionMatrix, blackList);
			if (leastConflictingClass >= 0) {
				Collection<Integer> cluster = new ArrayList<>();
				cluster.add(leastConflictingClass);
				do {
					Collection<Integer> newCluster = incrementCluster(cluster, confusionMatrix, blackList);
					if (newCluster.size() == cluster.size())
						break;
					cluster = newCluster;
					if (cluster.contains(-1))
						throw new IllegalStateException("Computed illegal cluster: " + cluster);
				} while (getPenaltyOfCluster(cluster, confusionMatrix) == 0 && cluster.size() < confusionMatrix.length);
				blackList.addAll(cluster);
				partitions.add(cluster);
			}
		} while (leastConflictingClass >= 0 && blackList.size() < confusionMatrix.length);

		return partitions;
	}

	private Collection<Integer> incrementCluster(Collection<Integer> cluster, double[][] confusionMatrix, Collection<Integer> blackList) {
		int leastSeenPenalty = Integer.MAX_VALUE;
		int choice = -1;
		for (int cId = 0; cId < confusionMatrix.length; cId++) {
			if (cluster.contains(cId) || blackList.contains(cId))
				continue;
			int addedPenalty = 0;
			for (int i = 0; i < confusionMatrix.length; i++) {
				addedPenalty += confusionMatrix[i][cId];
				addedPenalty += confusionMatrix[cId][i];
			}
			if (addedPenalty < leastSeenPenalty) {
				leastSeenPenalty = addedPenalty;
				choice = cId;
			}
		}
		Collection<Integer> newCluster = new ArrayList<>(cluster);
		if (choice < 0)
			return newCluster;
		newCluster.add(choice);
		return newCluster;
	}

	private int getPenaltyOfCluster(Collection<Integer> cluster, double[][] confusionMatrix) {
		int sum = 0;
		for (int i : cluster) {
			for (int j : cluster) {
				if (i != j)
					sum += confusionMatrix[i][j];
			}
		}
		return sum;
	}
}
