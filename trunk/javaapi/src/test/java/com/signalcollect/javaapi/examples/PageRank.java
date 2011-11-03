package com.signalcollect.javaapi.examples;

import com.signalcollect.ExecutionInformation;
import com.signalcollect.Graph;
import com.signalcollect.javaapi.GraphBuilder;

public class PageRank {

	public static void main(String[] args) {
		PageRank pr = new PageRank();
		pr.executePageRank();
	}

	public void executePageRank() {
		Graph cg = GraphBuilder.build();
		cg.addVertex(new PageRankVertex(1, 0.15));
		cg.addVertex(new PageRankVertex(2, 0.15));
		cg.addVertex(new PageRankVertex(3, 0.15));
		cg.addEdge(new PageRankEdge(1, 2));
		cg.addEdge(new PageRankEdge(2, 1));
		cg.addEdge(new PageRankEdge(2, 3));
		cg.addEdge(new PageRankEdge(3, 2));
		ExecutionInformation stats = cg.execute();
		System.out.println(stats);
		cg.shutdown();
	}
}
