package jaicore.search.algorithms.standard.core;

public class NodeAnnotationEvent<T> {

	private final T node;
	private final String annotationName;
	private final Object annotationValue;

	public NodeAnnotationEvent(T node, String annotationName, Object annotationValue) {
		super();
		this.node = node;
		this.annotationName = annotationName;
		this.annotationValue = annotationValue;
	}

	public T getNode() {
		return node;
	}

	public String getAnnotationName() {
		return annotationName;
	}

	public Object getAnnotationValue() {
		return annotationValue;
	}
}
