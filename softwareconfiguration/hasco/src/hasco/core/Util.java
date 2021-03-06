package hasco.core;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.geometry.euclidean.oned.Interval;

import hasco.model.CategoricalParameterDomain;
import hasco.model.Component;
import hasco.model.ComponentInstance;
import hasco.model.Dependency;
import hasco.model.NumericParameterDomain;
import hasco.model.Parameter;
import hasco.model.ParameterDomain;
import jaicore.basic.sets.SetUtil;
import jaicore.basic.sets.SetUtil.Pair;
import jaicore.logic.fol.structure.Literal;
import jaicore.logic.fol.structure.Monom;
import jaicore.planning.model.core.Action;
import jaicore.planning.model.core.PlannerUtil;
import jaicore.search.structure.core.Node;

public class Util {

  static Map<String, String> getParameterContainerMap(final Monom state, final String objectName) {
    Map<String, String> parameterContainerMap = new HashMap<>();
    List<Literal> containerLiterals = state.stream().filter(l -> l.getPropertyName().equals("parameterContainer") && l.getParameters().get(2).getName().equals(objectName))
        .collect(Collectors.toList());
    containerLiterals.forEach(l -> parameterContainerMap.put(l.getParameters().get(1).getName(), l.getParameters().get(3).getName()));
    return parameterContainerMap;
  }

  public static Map<ComponentInstance, Map<Parameter, String>> getParametrizations(final Monom state, final Collection<Component> components, final boolean resolveIntervals) {
    Map<String, ComponentInstance> objectMap = new HashMap<>();
    Map<String, Map<String, String>> parameterContainerMap = new HashMap<>(); // stores for each object the name of the container of each parameter
    Map<String, String> parameterValues = new HashMap<>();

    Map<ComponentInstance, Map<Parameter, String>> parameterValuesPerComponentInstance = new HashMap<>();

    Collection<String> overwrittenDataContainers = getOverwrittenDatacontainersInState(state);

    /*
     * create (empty) component instances, detect containers for parameter values, and register the
     * values of the data containers
     */
    for (Literal l : state) {
      String[] params = l.getParameters().stream().map(p -> p.getName()).collect(Collectors.toList()).toArray(new String[] {});
      switch (l.getPropertyName()) {
        case "resolves":
//          String parentObjectName = params[0];
//          String interfaceName = params[1];
          String componentName = params[2];
          String objectName = params[3];
          Component component = components.stream().filter(c -> c.getName().equals(componentName)).findAny().get();
          ComponentInstance object = new ComponentInstance(component, new HashMap<>(), new HashMap<>());
          objectMap.put(objectName, object);
          break;
        case "parameterContainer":
          if (!parameterContainerMap.containsKey(params[2])) {
            parameterContainerMap.put(params[2], new HashMap<>());
          }
          parameterContainerMap.get(params[2]).put(params[1], params[3]);
          break;
        case "val":
          if (overwrittenDataContainers.contains(params[0])) {
            parameterValues.put(params[0], params[1]);
          }
          break;
      }
    }

    /* update the configurations of the objects */
    for (String objectName : objectMap.keySet()) {
      Map<Parameter, String> paramValuesForThisComponent = new HashMap<>();
      ComponentInstance object = objectMap.get(objectName);
      parameterValuesPerComponentInstance.put(object, paramValuesForThisComponent);
      for (Parameter p : object.getComponent().getParameters()) {

        assert parameterContainerMap.containsKey(objectName) : "No parameter container map has been defined for object " + objectName + " of component "
            + object.getComponent().getName() + "!";
        assert parameterContainerMap.get(objectName).containsKey(p.getName()) : "The data container for parameter " + p.getName() + " of " + object.getComponent().getName()
            + " is not defined!";

        String assignedValue = parameterValues.get(parameterContainerMap.get(objectName).get(p.getName()));
        String interpretedValue = "";
        if (assignedValue != null) {
          if (p.getDefaultDomain() instanceof NumericParameterDomain) {
            if (resolveIntervals) {
              NumericParameterDomain np = (NumericParameterDomain) p.getDefaultDomain();
              List<String> vals = SetUtil.unserializeList(assignedValue);
              Interval interval = new Interval(Double.valueOf(vals.get(0)), Double.valueOf(vals.get(1)));
              if (np.isInteger()) {
                interpretedValue = String.valueOf((int) Math.round(interval.getBarycenter()));
              } else {
                interpretedValue = String.valueOf(interval.getBarycenter());
              }
            } else {
              interpretedValue = assignedValue;
            }
          } else if (p.getDefaultDomain() instanceof CategoricalParameterDomain) {
            interpretedValue = assignedValue;
          } else {
            throw new UnsupportedOperationException("No support for parameters of type " + p.getClass().getName());
          }
          paramValuesForThisComponent.put(p, interpretedValue);
        }
      }
    }
    return parameterValuesPerComponentInstance;
  }

  public static Collection<String> getOverwrittenDatacontainersInState(final Monom state) {
    return state.stream().filter(l -> l.getPropertyName().equals("overwritten")).map(l -> l.getParameters().get(0).getName()).collect(Collectors.toSet());
  }

  static Map<String, ComponentInstance> getGroundComponentsFromState(final Monom state, final Collection<Component> components, final boolean resolveIntervals) {
    Map<String, ComponentInstance> objectMap = new HashMap<>();
    Map<String, Map<String, String>> parameterContainerMap = new HashMap<>(); // stores for each object the name of the container of each parameter
    Map<String, String> parameterValues = new HashMap<>();
    Map<String, String> interfaceContainerMap = new HashMap<>();

    /*
     * create (empty) component instances, detect containers for parameter values, and register the
     * values of the data containers
     */
    for (Literal l : state) {
      String[] params = l.getParameters().stream().map(p -> p.getName()).collect(Collectors.toList()).toArray(new String[] {});
      switch (l.getPropertyName()) {
        case "resolves":
//          String parentObjectName = params[0];
//          String interfaceName = params[1];
          String componentName = params[2];
          String objectName = params[3];

          Component component = components.stream().filter(c -> c.getName().equals(componentName)).findAny().get();
          ComponentInstance object = new ComponentInstance(component, new HashMap<>(), new HashMap<>());
          objectMap.put(objectName, object);
          break;
        case "parameterContainer":
          if (!parameterContainerMap.containsKey(params[2])) {
            parameterContainerMap.put(params[2], new HashMap<>());
          }
          parameterContainerMap.get(params[2]).put(params[1], params[3]);
          break;
        case "val":
          parameterValues.put(params[0], params[1]);
          break;
        case "interfaceIdentifier":
          interfaceContainerMap.put(params[3], params[1]);
          break;
      }
    }

    /* now establish the binding of the required interfaces of the component instances */
    state.stream().filter(l -> l.getPropertyName().equals("resolves")).forEach(l -> {
      String[] params = l.getParameters().stream().map(p -> p.getName()).collect(Collectors.toList()).toArray(new String[] {});
      String parentObjectName = params[0];
//      String interfaceName = params[1];
      String objectName = params[3];

      ComponentInstance object = objectMap.get(objectName);
      if (!parentObjectName.equals("request")) {
        assert interfaceContainerMap.containsKey(objectName) : "Object name " + objectName + " for requried interface must have a defined identifier ";
        objectMap.get(parentObjectName).getSatisfactionOfRequiredInterfaces().put(interfaceContainerMap.get(objectName), object);
      }
    });

    /* update the configurations of the objects */
    for (String objectName : objectMap.keySet()) {
      ComponentInstance object = objectMap.get(objectName);
      for (Parameter p : object.getComponent().getParameters()) {

        assert parameterContainerMap.containsKey(objectName) : "No parameter container map has been defined for object " + objectName + " of component "
            + object.getComponent().getName() + "!";
        assert parameterContainerMap.get(objectName).containsKey(p.getName()) : "The data container for parameter " + p.getName() + " of " + object.getComponent().getName()
            + " is not defined!";

        String assignedValue = parameterValues.get(parameterContainerMap.get(objectName).get(p.getName()));
        if (assignedValue != null) {
          object.getParameterValues().put(p.getName(), getParamValue(p, assignedValue, resolveIntervals));
        }
      }
    }
    return objectMap;
  }

  public static <N, A, V extends Comparable<V>> ComponentInstance getSolutionCompositionForNode(final IHASCOSearchSpaceUtilFactory<N, A, V> searchSpaceUtilFactory,
      final Collection<Component> components, final Monom initState, final Node<N, ?> path) {
    return getSolutionCompositionForPlan(components, initState, searchSpaceUtilFactory.getPathToPlanConverter().getPlan(path.externalPath()));
  }
  
  public static Monom getFinalStateOfPlan(final Monom initState, final List<Action> plan) {
    Monom state = new Monom(initState);
    for (Action a : plan) {
      PlannerUtil.updateState(state, a);
    }
    return state;
  }

  public static ComponentInstance getSolutionCompositionForPlan(final Collection<Component> components, final Monom initState, final List<Action> plan) {
    return getSolutionCompositionFromState(components, getFinalStateOfPlan(initState, plan));
  }

  public static ComponentInstance getSolutionCompositionFromState(final Collection<Component> components, final Monom state) {
    return Util.getGroundComponentsFromState(state, components, true).get("solution");
  }
  

	/**
	 * Computes a String of component names that appear in the composition which can
	 * be used as an identifier for the composition
	 * 
	 * @param composition
	 * @return String of all component names in right to left depth-first order
	 */
	public static String getComponentNamesOfComposition(ComponentInstance composition) {
		StringBuilder builder = new StringBuilder();
		Deque<ComponentInstance> componentInstances = new ArrayDeque<ComponentInstance>();
		componentInstances.push(composition);
		ComponentInstance curInstance;
		while (!componentInstances.isEmpty()) {
			curInstance = componentInstances.pop();
			builder.append(curInstance.getComponent().getName());
			LinkedHashMap<String, String> requiredInterfaces = curInstance.getComponent().getRequiredInterfaces();
			// This set should be ordered
			Set<String> requiredInterfaceNames = requiredInterfaces.keySet();
			for (String requiredInterfaceName : requiredInterfaceNames) {
				ComponentInstance instance = curInstance.getSatisfactionOfRequiredInterfaces()
						.get(requiredInterfaceName);
				componentInstances.push(instance);
			}
		}
		return builder.toString();
	}

	/**
	 * Computes a list of all components of the given composition.
	 * 
	 * @param composition
	 * @return List of components in right to left depth-first order
	 */
	public static List<Component> getComponentsOfComposition(ComponentInstance composition) {
		List<Component> components = new LinkedList<Component>();
		Deque<ComponentInstance> componentInstances = new ArrayDeque<ComponentInstance>();
		componentInstances.push(composition);
		ComponentInstance curInstance;
		while (!componentInstances.isEmpty()) {
			curInstance = componentInstances.pop();
			components.add(curInstance.getComponent());
			LinkedHashMap<String, String> requiredInterfaces = curInstance.getComponent().getRequiredInterfaces();
			// This set should be ordered
			Set<String> requiredInterfaceNames = requiredInterfaces.keySet();
			for (String requiredInterfaceName : requiredInterfaceNames) {
				ComponentInstance instance = curInstance.getSatisfactionOfRequiredInterfaces()
						.get(requiredInterfaceName);
				componentInstances.push(instance);
			}
		}
		return components;
	}

  public static Map<Parameter, ParameterDomain> getUpdatedDomainsOfComponentParameters(final Monom state, final Component component, final String objectIdentifierInState) {
    Map<String, String> parameterContainerMap = new HashMap<>();
    Map<String, String> parameterContainerMapInv = new HashMap<>();
    Map<String, String> parameterValues = new HashMap<>();

    /* detect containers for parameter values, and register the values of the data containers */
    for (Literal l : state) {
      String[] params = l.getParameters().stream().map(p -> p.getName()).collect(Collectors.toList()).toArray(new String[] {});
      switch (l.getPropertyName()) {
        case "parameterContainer":
          if (!params[2].equals(objectIdentifierInState)) {
            continue;
          }
          parameterContainerMap.put(params[1], params[3]);
          parameterContainerMapInv.put(params[3], params[1]);
          break;
        case "val":
          parameterValues.put(params[0], params[1]);
          break;
      }
    }

    /* determine current values of the parameters of this component instance */
    Map<Parameter, String> paramValuesForThisComponentInstance = new HashMap<>();
    for (Parameter p : component.getParameters()) {
      assert parameterContainerMap.containsKey(p.getName()) : "The data container for parameter " + p.getName() + " of " + objectIdentifierInState + " is not defined!";
      String assignedValue = parameterValues.get(parameterContainerMap.get(p.getName()));
      assert assignedValue != null : "No value has been assigned to parameter " + p.getName() + " stored in container " + parameterContainerMap.get(p.getName()) + " in state "
          + state;
      String value = getParamValue(p, assignedValue, false);
      assert value != null : "Determined value NULL for parameter " + p.getName() + ", which is not plausible.";
      paramValuesForThisComponentInstance.put(p, value);
    }

    /* now compute the new domains based on the current values */
    Collection<Parameter> overwrittenParams = getOverwrittenDatacontainersInState(state).stream().filter(containerName -> parameterContainerMap.containsValue(containerName))
        .map(containerName -> component.getParameter(parameterContainerMapInv.get(containerName))).collect(Collectors.toList());
    return getUpdatedDomainsOfComponentParameters(component, paramValuesForThisComponentInstance, overwrittenParams);
  }

  private static String getParamValue(final Parameter p, final String assignedValue, final boolean resolveIntervals) {
    String interpretedValue = "";
    if (assignedValue == null) {
      throw new IllegalArgumentException("Cannot determine true value for assigned param value " + assignedValue + " for parameter " + p.getName());
    }
    if (p.isNumeric()) {
      if (resolveIntervals) {
        NumericParameterDomain np = (NumericParameterDomain) p.getDefaultDomain();
        List<String> vals = SetUtil.unserializeList(assignedValue);
        Interval interval = new Interval(Double.valueOf(vals.get(0)), Double.valueOf(vals.get(1)));
        if (np.isInteger()) {
          interpretedValue = String.valueOf((int) Math.round(interval.getBarycenter()));
        } else {
          interpretedValue = String.valueOf(interval.getBarycenter());
        }
      } else {
        interpretedValue = assignedValue;
      }
    } else if (p.getDefaultDomain() instanceof CategoricalParameterDomain) {
      interpretedValue = assignedValue;
    } else {
      throw new UnsupportedOperationException("No support for parameters of type " + p.getClass().getName());
    }
    return interpretedValue;
  }

  public static Map<Parameter, ParameterDomain> getUpdatedDomainsOfComponentParameters(final Component component, final Map<Parameter, String> currentValues,
      final Collection<Parameter> parametersForWhichADecisionHasBeenMade) {

    /* initialize all params for which no decision has been made yet with the default domain. */
    Map<Parameter, ParameterDomain> domains = new HashMap<>();
    for (Parameter p : parametersForWhichADecisionHasBeenMade) {
      if (p.isNumeric()) {
        NumericParameterDomain defaultDomain = (NumericParameterDomain) p.getDefaultDomain();
        Interval interval = SetUtil.unserializeInterval(currentValues.get(p));
        domains.put(p, new NumericParameterDomain(defaultDomain.isInteger(), interval.getInf(), interval.getSup()));
      } else if (p.isCategorical()) {
        domains.put(p, new CategoricalParameterDomain(new String[] { currentValues.get(p) }));
      }
    }

    /* initialize all others with the domain that corresponds to the current choice */
    for (Parameter p : SetUtil.difference(component.getParameters(), parametersForWhichADecisionHasBeenMade)) {
      domains.put(p, p.getDefaultDomain());
    }
    assert (domains.keySet().equals(component.getParameters())) : "There are parameters for which no current domain was derived.";

    /* update domains based on the dependencies defined for this component */
    for (Dependency dependency : component.getDependencies()) {
      if (isDependencyPremiseSatisfied(dependency, currentValues)) {
        for (Pair<Parameter, ParameterDomain> newDomain : dependency.getConclusion()) {

          /*
           * directly use the concluded domain if the current value is NOT subsumed by it. Otherwise, just
           * stick to the current domain
           */
          Parameter param = newDomain.getX();
          ParameterDomain concludedDomain = newDomain.getY();
          if (!concludedDomain.subsumes(domains.get(param))) {
            domains.put(param, concludedDomain);
          }

          // ParameterDomain intersection = null;
          // if (param.isNumeric()) {
          // NumericParameterDomain cConcludedDomain = (NumericParameterDomain)concludedDomain;
          // NumericParameterDomain currentDomain = (NumericParameterDomain)domains.get(newDomain.getX());
          // intersection = new NumericParameterDomain(cConcludedDomain.isInteger(),
          // Math.max(cConcludedDomain.getMin(), currentDomain.getMin()), Math.min(cConcludedDomain.getMax(),
          // currentDomain.getMax()));
          // }
          // else if (param.isCategorical()) {
          // CategoricalParameterDomain cConcludedDomain = (CategoricalParameterDomain)concludedDomain;
          // CategoricalParameterDomain currentDomain =
          // (CategoricalParameterDomain)domains.get(newDomain.getX());
          // intersection = new
          // CategoricalParameterDomain(SetUtil.intersection(Arrays.asList(cConcludedDomain.getValues()),
          // Arrays.asList(currentDomain.getValues())));
          // }
          // else
          // throw new UnsupportedOperationException("Cannot currently handle parameters that are not numeric
          // and not categorical.");
          // assert intersection != null : "The intersection of the current domain and the domain dictated by
          // a rule has failed";
          // domains.put(param, intersection);
        }
      }
    }
    return domains;
  }

  public static boolean isDependencyPremiseSatisfied(final Dependency dependency, final Map<Parameter, String> values) {
    for (Collection<Pair<Parameter, ParameterDomain>> condition : dependency.getPremise()) {
      if (!isDependencyConditionSatisfied(condition, values)) {
        return false;
      }
    }
    return true;
  }

  public static boolean isDependencyConditionSatisfied(final Collection<Pair<Parameter, ParameterDomain>> condition, final Map<Parameter, String> values) {
    for (Pair<Parameter, ParameterDomain> conditionItem : condition) {
      ParameterDomain requiredDomain = conditionItem.getY();
      Parameter param = conditionItem.getX();
      assert values.containsKey(param) : "Cannot check condition " + condition + " as the value for parameter " + param.getName() + " is not defined in " + values;
      assert values.get(param) != null : "Cannot check condition " + condition + " as the value for parameter " + param.getName() + " is NULL in " + values;
      if (param.getDefaultDomain() instanceof NumericParameterDomain) {
        Interval actualInterval = SetUtil.unserializeInterval(values.get(param));
        ParameterDomain actualParameterDomain = new NumericParameterDomain(((NumericParameterDomain) param.getDefaultDomain()).isInteger(), actualInterval.getInf(),
            actualInterval.getSup());
        if (!requiredDomain.subsumes(actualParameterDomain)) {
          return false;
        }
      } else if (param.getDefaultDomain() instanceof CategoricalParameterDomain) {
        if (!requiredDomain.contains(values.get(param))) {
          return false;
        }
      }
    }
    return true;
  }
}
