package com.signalcollect.javaapi.examples;

import com.signalcollect.javaapi.*;

@SuppressWarnings("serial")
public class PageRankEdge extends DefaultEdge<PageRankVertex> {

	public PageRankEdge(Integer sourceId, Integer targetId) {
		super(sourceId, targetId);
	}
	
	public Object signal(PageRankVertex sourceVertex) {
		return sourceVertex.getState() * getWeight() / sourceVertex.getSumOfOutWeights();
	}
}