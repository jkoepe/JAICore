package jaicore.planning.model.task.ceocipstn;

import java.util.Map;

import jaicore.logic.fol.structure.CNFFormula;
import jaicore.logic.fol.structure.Monom;
import jaicore.logic.fol.theories.EvaluablePredicate;
import jaicore.planning.graphgenerators.task.ceociptfd.OracleTaskResolver;
import jaicore.planning.model.task.ceocstn.CEOCSTNPlanningDomain;
import jaicore.planning.model.task.ceocstn.CEOCSTNPlanningProblem;
import jaicore.planning.model.task.stn.TaskNetwork;

@SuppressWarnings("serial")
public class CEOCIPSTNPlanningProblem extends CEOCSTNPlanningProblem {
	private final Map<String, EvaluablePredicate> evaluablePlanningPredicates;
	private final Map<String, OracleTaskResolver> oracleResolvers;

	public CEOCIPSTNPlanningProblem(CEOCSTNPlanningDomain domain, CNFFormula knowledge, Monom init, TaskNetwork network, Map<String, EvaluablePredicate> evaluablePredicates,
			Map<String, OracleTaskResolver> oracleResolvers) {
		super(domain, knowledge, init, network);
		this.evaluablePlanningPredicates = evaluablePredicates;
		this.oracleResolvers = oracleResolvers;
	}

	public CEOCIPSTNPlanningDomain getDomain() {
		return (CEOCIPSTNPlanningDomain) super.getDomain();
	}

	public Map<String, EvaluablePredicate> getEvaluablePlanningPredicates() {
		return evaluablePlanningPredicates;
	}

	public Map<String, OracleTaskResolver> getOracleResolvers() {
		return oracleResolvers;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((evaluablePlanningPredicates == null) ? 0 : evaluablePlanningPredicates.hashCode());
		result = prime * result + ((oracleResolvers == null) ? 0 : oracleResolvers.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		CEOCIPSTNPlanningProblem other = (CEOCIPSTNPlanningProblem) obj;
		if (evaluablePlanningPredicates == null) {
			if (other.evaluablePlanningPredicates != null)
				return false;
		} else if (!evaluablePlanningPredicates.equals(other.evaluablePlanningPredicates))
			return false;
		if (oracleResolvers == null) {
			if (other.oracleResolvers != null)
				return false;
		} else if (!oracleResolvers.equals(other.oracleResolvers))
			return false;
		return true;
	}

}
