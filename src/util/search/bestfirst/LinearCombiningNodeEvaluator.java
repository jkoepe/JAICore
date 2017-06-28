package util.search.bestfirst;

import java.util.Map;

import util.search.core.Node;
import util.search.core.NodeEvaluator;

public class LinearCombiningNodeEvaluator<T> implements NodeEvaluator<T,Integer> {

	private final Map<NodeEvaluator<T,Integer>, Double> evaluators;

	public LinearCombiningNodeEvaluator(Map<NodeEvaluator<T,Integer>, Double> evaluators) {
		super();
		this.evaluators = evaluators;
	}

	@Override
	public Integer f(Node<T,Integer> node) {
		double score = 0;
		int incr;
		for (NodeEvaluator<T,Integer> evaluator : evaluators.keySet()) {
			if (evaluators.get(evaluator) != 0) {
				incr = evaluator.f(node);
				if (incr == Integer.MAX_VALUE) {
					score = Integer.MAX_VALUE;
					break;
				}
				else
					score += incr * evaluators.get(evaluator);
			}
		}
		return (int)Math.round(score);
	}
}