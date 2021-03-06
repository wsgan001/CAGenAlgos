package edu.asu.ca.kaushik.outputformatter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.asu.ca.kaushik.Runner;
import edu.asu.ca.kaushik.algorithms.structures.CA;

public class ExplicitCAOutputFormatter implements OutputFormatter {
	private List<String> algoNames;
	private String dir = "data\\out\\explicit-arrays\\";
	private String newDirName;
	private int t;
	private int v;
	
	public ExplicitCAOutputFormatter(String dirName) {
		this.dir = dirName;
	}
	
	public ExplicitCAOutputFormatter() {
		
	}

	@Override
	public void setAlgoNames(List<String> algoNames) {
		this.algoNames = algoNames;
		
	}

	@Override
	public void preprocess(int t, int v) throws IOException {
		this.t = t;
		this.v = v;
		
		this.newDirName = "" + t + "-" + "k" + "-" + v;
		
		File newDir = new File(this.dir + this.newDirName);
		newDir.mkdirs();
		
	}

	@Override
	public void output(int k, List<CA> results) throws IOException {
		for (int i = 0; i < results.size(); i++){
			this.outputExplicitCA(k, this.algoNames.get(i), results.get(i));
		}
	}

	private void outputExplicitCA(int k, String algoName, CA ca) throws IOException {
		String filename = this.getFileName(k, algoName);
		int numRows = ca.getNumRows();
		Iterator<Integer[]> caIterator = ca.iterator();
		
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(filename, false));
			this.writeCA(out, numRows, caIterator);
			
		} finally {
			if (out != null){
				out.close();
			}
		}		
	}

	private String getFileName(int k, String algoName) {
		return dir + newDirName + "\\" +  this.t + "-" + k + "-" + this.v + "-" + algoName + ".txt";
	}

	private void writeCA(PrintWriter out, int numRows,
			Iterator<Integer[]> caIterator) {
		
		out.println(numRows);
		while(caIterator.hasNext()) {
			writeRow(out, caIterator.next());
		}
		
	}

	private void writeRow(PrintWriter out, Integer[] row) {
		for(Integer i : row){
			out.print(i + " ");
		}
		out.println();
	}
	
	public static void main(String[] args) throws IOException {
		int t = 6;
		int v = 3;
		
		int k1 = 19;
		int k2 = 19;
		
		List<String> algoList = new ArrayList<String>();
		
		//algoList.add("DensityMultCandidateRow");
		algoList.add("HybridMultiCandidate");
		//algoList.add("BestOfKDensityMultiCandidateHybrid");
		//algoList.add("CondExpIteration");
		
		OutputFormatter formatter = new ExplicitCAOutputFormatter();
		
		Runner runner = new Runner(formatter);
		runner.setParam(t, v, k1, k2);
		runner.setAlgo(algoList);
		runner.run();
		
		//runner.setParam(4, 3, 5, 20);
		//runner.run();
	}

}
